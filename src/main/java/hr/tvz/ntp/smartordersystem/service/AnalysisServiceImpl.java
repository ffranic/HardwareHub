package hr.tvz.ntp.smartordersystem.service;

import hr.tvz.ntp.smartordersystem.dto.SystemAnalysisResultDto;
import hr.tvz.ntp.smartordersystem.model.Order;
import hr.tvz.ntp.smartordersystem.model.Product;
import hr.tvz.ntp.smartordersystem.model.User;
import hr.tvz.ntp.smartordersystem.repository.OrderRepository;
import hr.tvz.ntp.smartordersystem.repository.ProductRepository;
import hr.tvz.ntp.smartordersystem.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/*
 * SERVIS ZA PARALELNU ANALIZU SUSTAVA.
 *
 * Ova klasa ima dvije važne svrhe:
 *
 * 1. demonstrira paralelno izvršavanje zadataka kroz ExecutorService
 * 2. demonstrira međusobno zaključavanje dretvi kroz ReentrantLock
 *
 * Tri paralelna zadatka analiziraju:
 * - korisnike
 * - proizvode
 * - narudžbe
 *
 * Svaki zadatak zatim želi pisati u ISTU datoteku analysis-results.txt.
 * Zato je samo dio koji zapisuje u datoteku kritična sekcija zaštićena lockom.
 */
@Service
@Transactional
public class AnalysisServiceImpl implements AnalysisService {

    /*
     * Koristimo pool od točno tri dretve jer imamo tri neovisna zadatka.
     */
    private static final int THREAD_POOL_SIZE = 3;

    /*
     * Zajednički izlazni resurs za sve dretve.
     */
    private static final Path ANALYSIS_FILE = Path.of("analysis-results.txt");

    /*
     * ReentrantLock štiti zapisivanje u zajedničku datoteku.
     *
     * VAŽNO:
     * Ne zaključavamo cijelu analizu.
     *
     * Analize se izvršavaju paralelno, a samo zapisivanje mora biti
     * sekvencijalno kako se sadržaj različitih dretvi ne bi pomiješao.
     */
    private final ReentrantLock fileWriteLock = new ReentrantLock();

    /*
     * Repozitoriji služe za dohvat stvarnih podataka iz baze.
     */
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    /*
     * Constructor injection.
     *
     * Spring automatski pronalazi implementacije repository beanova
     * i predaje ih konstruktoru.
     *
     * Constructor injection je poželjan jer dependencyji mogu biti final.
     */
    public AnalysisServiceImpl(UserRepository userRepository,
                               ProductRepository productRepository,
                               OrderRepository orderRepository) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    /*
     * Glavna metoda koja pokreće cijelu paralelnu analizu.
     */
    @Override
    public SystemAnalysisResultDto runSystemAnalysis() {

        // Vrijeme početka koristimo za mjerenje trajanja cijele analize.
        long start = System.currentTimeMillis();

        /*
         * Kreira se Thread Pool s tri worker dretve.
         *
         * Za razliku od ručnog new Thread(...), ExecutorService:
         * - upravlja dretvama
         * - reciklira ih
         * - raspoređuje zadatke
         */
        ExecutorService executorService =
                Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        try {

            /*
             * Na početku prepisujemo analysis-results.txt.
             *
             * Ovdje nema potrebe za lockom jer paralelni zadaci još
             * nisu pokrenuti.
             */
            Files.writeString(
                    ANALYSIS_FILE,
                    "=== Hardware Hub System Analysis ===\nGenerated at: "
                            + LocalDateTime.now() + "\n\n",
                    StandardCharsets.UTF_8
            );

            /*
             * Callable predstavlja zadatak koji se može izvršiti u dretvi.
             *
             * Imamo tri neovisna zadatka:
             *
             * Thread 1 -> analyzeUsers()
             * Thread 2 -> analyzeProducts()
             * Thread 3 -> analyzeOrders()
             *
             * Metode vraćaju Void jer nam njihov rezultat nije potreban
             * kroz return vrijednost - same rezultat pišu u datoteku.
             */
            List<Callable<Void>> tasks = List.of(
                    this::analyzeUsers,
                    this::analyzeProducts,
                    this::analyzeOrders
            );

            /*
             * invokeAll:
             * - preda sva tri zadatka Thread Poolu
             * - dopušta njihovo paralelno izvršavanje
             * - vraća Future objekte
             * - čeka završetak svih zadataka
             */
            List<Future<Void>> futures =
                    executorService.invokeAll(tasks);

            /*
             * future.get() dodatno provjerava je li svaki task završio
             * uspješno.
             *
             * Ako je task bacio exception, ovdje ćemo dobiti
             * ExecutionException.
             */
            for (Future<Void> future : futures) {
                future.get();
            }

            // Izračun ukupnog trajanja analize.
            long duration = System.currentTimeMillis() - start;

            /*
             * Nakon što su sva tri taska završila,
             * čitamo konačnu zajedničku datoteku.
             */
            String content =
                    Files.readString(ANALYSIS_FILE, StandardCharsets.UTF_8);

            /*
             * DTO vraća frontendu:
             * - naziv filea
             * - veličinu Thread Poola
             * - trajanje
             * - rezultat analize
             */
            return new SystemAnalysisResultDto(
                    ANALYSIS_FILE.toString(),
                    THREAD_POOL_SIZE,
                    duration,
                    content
            );

        } catch (InterruptedException e) {

            /*
             * Ako je dretva koja čeka rezultate prekinuta,
             * vraćamo interrupted flag.
             *
             * Ovo je ispravna praksa kod InterruptedExceptiona.
             */
            Thread.currentThread().interrupt();

            throw new RuntimeException(
                    "System analysis was interrupted.",
                    e
            );

        } catch (ExecutionException e) {

            /*
             * ExecutionException znači da je neki od Callable taskova
             * završio exceptionom.
             */
            throw new RuntimeException(
                    "System analysis failed.",
                    e
            );

        } catch (Exception e) {

            // Ostale greške: file I/O, baza itd.
            throw new RuntimeException(
                    "Could not run system analysis.",
                    e
            );

        } finally {

            /*
             * Vrlo važno:
             * nakon korištenja Thread Poola gasimo ExecutorService.
             *
             * Inače bi worker dretve mogle ostati aktivne.
             */
            executorService.shutdown();
        }
    }

    /*
     * TASK 1 - ANALIZA KORISNIKA
     */
    private Void analyzeUsers() {

        // Dohvaćamo sve korisnike iz baze.
        List<User> users = userRepository.findAll();

        /*
         * Stream filtrira ADMIN korisnike i broji ih.
         *
         * Provjera role != null štiti od NullPointerExceptiona.
         */
        long adminCount = users.stream()
                .filter(user ->
                        user.getRole() != null
                                && "ADMIN".equals(user.getRole().name()))
                .count();

        // Analogno brojimo CUSTOMER korisnike.
        long customerCount = users.stream()
                .filter(user ->
                        user.getRole() != null
                                && "CUSTOMER".equals(user.getRole().name()))
                .count();

        /*
         * Java text block + formatted().
         *
         * Thread.currentThread().getName()
         * nam omogućuje da u rezultatu vidimo koja je dretva
         * izvršila konkretan task.
         */
        String result = """
                [Users analysis]
                Thread: %s
                Total users: %d
                Admins: %d
                Customers: %d
                
                """.formatted(
                Thread.currentThread().getName(),
                users.size(),
                adminCount,
                customerCount
        );

        /*
         * Rezultat želi zapisati u ISTU datoteku kao druga dva taska.
         *
         * Ovdje ulazimo u metodu koja koristi ReentrantLock.
         */
        writeToAnalysisFile(result);

        return null;
    }

    /*
     * TASK 2 - ANALIZA PROIZVODA
     */
    private Void analyzeProducts() {

        List<Product> products = productRepository.findAll();

        // Broj proizvoda kojih više nema na zalihi.
        long outOfStock = products.stream()
                .filter(product -> product.getStock() <= 0)
                .count();

        /*
         * mapToInt pretvara stream proizvoda u IntStream zaliha,
         * nakon čega sum() daje ukupnu količinu svih artikala.
         */
        int totalStock = products.stream()
                .mapToInt(Product::getStock)
                .sum();

        /*
         * Računa vrijednost cijelog skladišta:
         *
         * cijena proizvoda * količina na zalihi
         *
         * zatim se svi iznosi zbrajaju.
         *
         * BigDecimal koristimo za novčane vrijednosti jer float/double
         * mogu imati probleme s preciznošću.
         */
        BigDecimal stockValue = products.stream()
                .map(product -> product.getPrice() != null
                        ? product.getPrice()
                          .multiply(BigDecimal.valueOf(product.getStock()))
                        : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String result = """
                [Products analysis]
                Thread: %s
                Total products: %d
                Out of stock: %d
                Total stock quantity: %d
                Total stock value: %s EUR
                
                """.formatted(
                Thread.currentThread().getName(),
                products.size(),
                outOfStock,
                totalStock,
                stockValue
        );

        writeToAnalysisFile(result);

        return null;
    }

    /*
     * TASK 3 - ANALIZA NARUDŽBI
     */
    private Void analyzeOrders() {

        List<Order> orders = orderRepository.findAll();

        /*
         * groupingBy grupira narudžbe prema statusu.
         *
         * Rezultat je konceptualno:
         *
         * NEW -> 5
         * PROCESSING -> 2
         * DELIVERED -> 10
         *
         * Collectors.counting() broji koliko je narudžbi u svakoj grupi.
         */
        String statusSummary = orders.stream()
                .collect(Collectors.groupingBy(
                        order -> order.getStatus() != null
                                ? order.getStatus().name()
                                : "UNKNOWN",
                        Collectors.counting()
                ))
                .entrySet()
                .stream()

                // Map.Entry pretvaramo u tekst "STATUS: broj".
                .map(entry ->
                        entry.getKey() + ": " + entry.getValue())

                // Sve statuse spajamo s novim redom između njih.
                .collect(Collectors.joining("\n"));

        /*
         * orderRepository.findAll() ne mora učitati lazy kolekciju
         * orderItems.
         *
         * Zato za izračun ukupne vrijednosti koristimo
         * findByIdWithItems(), koji JOIN FETCH-om učitava OrderIteme.
         *
         * Time izbjegavamo LazyInitializationException u worker dretvi.
         */
        BigDecimal totalOrderValue = orders.stream()
                .map(order ->
                        orderRepository.findByIdWithItems(order.getId())
                                .orElseThrow(() ->
                                        new NoSuchElementException(
                                                "Order not found")))
                .map(Order::calculateTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String result = """
                [Orders analysis]
                Thread: %s
                Total orders: %d
                Orders by status:
                %s
                Total order value: %s EUR
                
                """.formatted(
                Thread.currentThread().getName(),
                orders.size(),
                statusSummary.isBlank() ? "-" : statusSummary,
                totalOrderValue
        );

        writeToAnalysisFile(result);

        return null;
    }

    /*
     * KRITIČNA SEKCIJA.
     *
     * Ovo je najvažnija metoda za zahtjev ReentrantLock.
     *
     * Tri različite dretve mogu istovremeno završiti analizu i pokušati
     * pisati u analysis-results.txt.
     *
     * Bez zaključavanja:
     *
     * Thread 1 ─┐
     * Thread 2 ─┼──> analysis-results.txt
     * Thread 3 ─┘
     *
     * sadržaj bi potencijalno mogao biti isprepleten ili nekonzistentan.
     */
    private void writeToAnalysisFile(String content) {

        /*
         * lock() zauzima zaključavanje.
         *
         * Ako je lock slobodan -> dretva odmah ulazi.
         * Ako druga dretva već piše -> trenutna dretva čeka.
         */
        fileWriteLock.lock();

        try {

            /*
             * CREATE -> napravi file ako ne postoji.
             * APPEND -> ne prepisuj postojeći rezultat, nego dodaj novi.
             */
            Files.writeString(
                    ANALYSIS_FILE,
                    content,
                    StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Could not write analysis result.",
                    e
            );

        } finally {

            /*
             * unlock() MORA biti u finally bloku.
             *
             * Tako se lock oslobađa čak i ako Files.writeString()
             * baci exception.
             *
             * Inače bi ostale dretve mogle zauvijek čekati.
             */
            fileWriteLock.unlock();
        }
    }
}