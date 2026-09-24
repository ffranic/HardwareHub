package hr.tvz.ntp.smartordersystem.service;

import hr.tvz.ntp.smartordersystem.dto.ArchivedOrderBinaryRecordDto;
import hr.tvz.ntp.smartordersystem.model.Order;
import hr.tvz.ntp.smartordersystem.model.Status;
import hr.tvz.ntp.smartordersystem.repository.OrderRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/*
 * SERVIS ZA ARHIVIRANJE ZAVRŠENIH NARUDŽBI
 * U PRILAGOĐENOM BINARNOM FORMATU.
 *
 * OBRANA:
 * Ova klasa pokriva zahtjev:
 * "razviti i koristiti datoteku prilagođenog binarnog formata"
 *
 * Podržano je:
 * - WRITE -> spremanje narudžbe u .bin arhivu
 * - READ  -> čitanje svih arhiviranih narudžbi
 *
 * Arhiviraju se samo terminalni statusi:
 * - DELIVERED
 * - CANCELLED
 *
 * Svaki dan ima svoju datoteku:
 *
 * order-archives/orders-2026-08-17.bin
 *
 * Format datoteke nije Java serialization,
 * nego smo ga sami definirali pomoću DataOutputStream/DataInputStream.
 */
@Service
@Transactional
public class BinaryOrderArchiveServiceImpl
        implements BinaryOrderArchiveService {

    /*
     * Folder u kojem se čuvaju dnevne binarne arhive.
     */
    private static final Path ARCHIVE_DIR =
            Path.of("order-archives");

    /*
     * "Magic" vrijednost na početku datoteke.
     *
     * OBRANA:
     * Magic služi kao identifikator našeg prilagođenog formata.
     *
     * Ako otvorimo neku drugu .bin datoteku,
     * možemo odmah provjeriti je li to stvarno HHORDERS format.
     */
    private static final String FILE_MAGIC =
            "HHORDERS";

    /*
     * Verzija formata.
     *
     * Ako jednog dana promijenimo strukturu datoteke,
     * možemo prijeći na version 2.
     */
    private static final int FILE_VERSION = 1;

    private final OrderRepository orderRepository;

    public BinaryOrderArchiveServiceImpl(
            OrderRepository orderRepository) {

        this.orderRepository =
                orderRepository;
    }

    /*
     * WRITE OPERACIJA.
     *
     * Metoda se poziva kada se provjerava treba li narudžbu arhivirati.
     *
     * synchronized znači da unutar iste instance servisa samo jedna
     * dretva u jednom trenutku može izvršavati ovu metodu.
     *
     * Time se smanjuje rizik da dvije dretve istovremeno:
     * - pročitaju isti file
     * - obje dodaju zapis
     * - jedna drugoj prepišu rezultat
     */
    @Override
    public synchronized void archiveOrderIfTerminal(Long orderId) {

        /*
         * Dohvat narudžbe zajedno s OrderItemima i Productima.
         *
         * To nam treba jer kasnije računamo ukupnu vrijednost.
         */
        Order order =
                orderRepository
                        .findByIdWithItems(orderId)
                        .orElseThrow(() ->
                                new NoSuchElementException(
                                        "Order not found: "
                                                + orderId));

        /*
         * Arhiviraju se samo završene narudžbe.
         *
         * NEW / PROCESSING itd. se ne spremaju u binarnu arhivu.
         */
        if (order.getStatus() != Status.DELIVERED
                && order.getStatus() != Status.CANCELLED) {

            return;
        }

        try {

            /*
             * Ako folder ne postoji, kreiraj ga.
             *
             * createDirectories je siguran i ako folder već postoji.
             */
            Files.createDirectories(
                    ARCHIVE_DIR);

            /*
             * Arhivska datoteka je vezana uz današnji datum.
             */
            LocalDate archiveDate =
                    LocalDate.now();

            /*
             * Primjer:
             *
             * orders-2026-08-17.bin
             */
            Path archiveFile =
                    getArchiveFile(
                            archiveDate);

            /*
             * Ako dnevni file već postoji,
             * prvo čitamo sve postojeće zapise.
             *
             * Ako ne postoji,
             * metoda vraća praznu listu.
             */
            List<ArchivedOrderBinaryRecordDto> existingRecords =
                    readArchiveFileIfExists(
                            archiveFile);

            /*
             * Provjera da ista narudžba nije već arhivirana.
             *
             * OBRANA:
             * Time sprječavamo duplicate zapis ako se metoda
             * slučajno pozove više puta za isti orderId.
             */
            boolean alreadyArchived =
                    existingRecords
                            .stream()
                            .anyMatch(record ->
                                    record.getOrderId()
                                            .equals(orderId));

            if (alreadyArchived) {
                return;
            }

            /*
             * JPA Order pretvaramo u jednostavniji DTO
             * prilagođen binarnoj arhivi.
             */
            existingRecords.add(
                    mapOrderToRecord(order));

            /*
             * Ponovno zapisujemo cijelu dnevnu arhivu.
             *
             * Dakle princip je:
             *
             * pročitaj postojeće
             * -> dodaj novi zapis
             * -> zapiši cijelu listu
             */
            writeArchiveFile(
                    archiveFile,
                    existingRecords);

        } catch (IOException e) {

            throw new RuntimeException(
                    "Could not archive order to binary file.",
                    e
            );
        }
    }

    /*
     * READ OPERACIJA.
     *
     * Čita sve orders-*.bin datoteke iz archive foldera.
     */
    @Override
    public synchronized
    Map<String, List<ArchivedOrderBinaryRecordDto>> readArchivedOrders() {

        /*
         * TreeMap reverseOrder:
         *
         * najnoviji datum će biti prvi.
         */
        Map<String, List<ArchivedOrderBinaryRecordDto>> result =
                new TreeMap<>(
                        Comparator.reverseOrder());

        try {

            Files.createDirectories(
                    ARCHIVE_DIR);

            /*
             * DirectoryStream filtrira samo:
             *
             * orders-*.bin
             *
             * Ostale datoteke u folderu ignorira.
             */
            try (DirectoryStream<Path> stream =
                         Files.newDirectoryStream(
                                 ARCHIVE_DIR,
                                 "orders-*.bin")) {

                for (Path file : stream) {

                    /*
                     * Iz:
                     *
                     * orders-2026-08-17.bin
                     *
                     * izvlačimo:
                     *
                     * 2026-08-17
                     */
                    String date =
                            file.getFileName()
                                    .toString()
                                    .replace(
                                            "orders-",
                                            "")
                                    .replace(
                                            ".bin",
                                            "");

                    /*
                     * Datum -> lista narudžbi iz te datoteke.
                     */
                    result.put(
                            date,
                            readArchiveFileIfExists(
                                    file)
                    );
                }
            }

            return result;

        } catch (IOException e) {

            throw new RuntimeException(
                    "Could not read binary order archives.",
                    e
            );
        }
    }

    /*
     * Order entity -> ArchivedOrderBinaryRecordDto.
     *
     * U binary file ne spremamo cijeli JPA objektni graf,
     * nego samo podatke koji su nam potrebni za arhivu.
     */
    private ArchivedOrderBinaryRecordDto mapOrderToRecord(Order order) {

        /*
         * Calculated field:
         *
         * total se ne sprema direktno u Order,
         * nego se računa iz OrderItem stavki.
         */
        BigDecimal total =
                order.calculateTotal();

        return new ArchivedOrderBinaryRecordDto(
                order.getId(),
                order.getUser().getUsername(),
                order.getUser().getEmail(),

                /*
                 * Defensive null handling.
                 */
                order.getOrder_date() != null
                        ? order.getOrder_date().toString()
                        : "-",

                /*
                 * Vrijeme kada je zapis stvarno arhiviran.
                 */
                LocalDateTime.now().toString(),

                order.getStatus().name(),
                total
        );
    }

    /*
     * Helper za generiranje putanje dnevne arhive.
     *
     * Primjer:
     *
     * date = 2026-08-17
     *
     * rezultat:
     * order-archives/orders-2026-08-17.bin
     */
    private Path getArchiveFile(
            LocalDate date) {

        return ARCHIVE_DIR.resolve(
                "orders-"
                        + date
                        + ".bin"
        );
    }

    /*
     * GLAVNA READ METODA BINARNOG FORMATA.
     *
     * OBRANA:
     * DataInputStream omogućuje čitanje primitive/string vrijednosti
     * točno istim redoslijedom kojim su zapisane DataOutputStreamom.
     *
     * REDOSLIJED JE KRITIČAN.
     */
    private List<ArchivedOrderBinaryRecordDto> readArchiveFileIfExists(Path file) {

        /*
         * Ako file ne postoji,
         * tretiramo arhivu kao praznu.
         */
        if (!Files.exists(file)) {
            return new ArrayList<>();
        }

        /*
         * try-with-resources automatski zatvara stream.
         */
        try (DataInputStream dis =
                     new DataInputStream(
                             new FileInputStream(
                                     file.toFile()))) {

            /*
             * PRVI PODATAK:
             * magic string
             */
            String magic =
                    dis.readUTF();

            /*
             * Provjera da je ovo stvarno naš format.
             */
            if (!FILE_MAGIC.equals(magic)) {

                throw new IllegalStateException(
                        "Invalid binary order archive format."
                );
            }

            /*
             * DRUGI PODATAK:
             * verzija formata
             */
            int version =
                    dis.readInt();

            if (version != FILE_VERSION) {

                throw new IllegalStateException(
                        "Unsupported binary order archive version: "
                                + version
                );
            }

            /*
             * TREĆI PODATAK:
             * broj zapisa koji slijede.
             *
             * Bez ovoga ne bismo znali koliko puta treba čitati record.
             */
            int recordCount =
                    dis.readInt();

            List<ArchivedOrderBinaryRecordDto> records =
                    new ArrayList<>();

            /*
             * Za svaki zapis čitamo polja TOČNO istim redoslijedom
             * kojim ih writeArchiveFile() zapisuje.
             */
            for (int i = 0;
                 i < recordCount;
                 i++) {

                /*
                 * writeLong <-> readLong
                 */
                Long orderId =
                        dis.readLong();

                /*
                 * writeUTF <-> readUTF
                 */
                String username =
                        dis.readUTF();

                String email =
                        dis.readUTF();

                String orderDate =
                        dis.readUTF();

                String archivedAt =
                        dis.readUTF();

                String status =
                        dis.readUTF();

                /*
                 * BigDecimal spremamo kao String.
                 *
                 * Zašto?
                 * DataOutputStream nema writeBigDecimal().
                 *
                 * Zato:
                 * BigDecimal -> String -> binary UTF
                 *
                 * a kod čitanja:
                 * String -> BigDecimal
                 */
                BigDecimal total =
                        new BigDecimal(
                                dis.readUTF());

                /*
                 * Iz učitanih vrijednosti rekonstruiramo DTO.
                 */
                records.add(
                        new ArchivedOrderBinaryRecordDto(
                                orderId,
                                username,
                                email,
                                orderDate,
                                archivedAt,
                                status,
                                total
                        )
                );
            }

            return records;

        } catch (IOException e) {

            throw new RuntimeException(
                    "Could not read binary order archive file.",
                    e
            );
        }
    }

    /*
     * GLAVNA WRITE METODA BINARNOG FORMATA.
     *
     * OBRANA:
     * Ovdje je zapravo definiran naš custom binary format.
     */
    private void writeArchiveFile(
            Path file,
            List<ArchivedOrderBinaryRecordDto> records) {

        /*
         * FileOutputStream bez append=true prepisuje cijelu datoteku.
         *
         * To je namjerno jer prije toga imamo kompletnu listu zapisa.
         */
        try (DataOutputStream dos =
                     new DataOutputStream(
                             new FileOutputStream(
                                     file.toFile()))) {

            /*
             * ZAGLAVLJE DATOTEKE:
             *
             * 1. MAGIC
             * 2. VERSION
             * 3. RECORD COUNT
             */
            dos.writeUTF(
                    FILE_MAGIC);

            dos.writeInt(
                    FILE_VERSION);

            dos.writeInt(
                    records.size());

            /*
             * Nakon headera dolaze zapisi.
             */
            for (ArchivedOrderBinaryRecordDto record :
                    records) {

                /*
                 * Redoslijed mora biti identičan read metodi.
                 */
                dos.writeLong(
                        record.getOrderId());

                dos.writeUTF(
                        record.getUsername() != null
                                ? record.getUsername()
                                : "");

                dos.writeUTF(
                        record.getEmail() != null
                                ? record.getEmail()
                                : "");

                dos.writeUTF(
                        record.getOrderDate() != null
                                ? record.getOrderDate()
                                : "");

                dos.writeUTF(
                        record.getArchivedAt() != null
                                ? record.getArchivedAt()
                                : "");

                dos.writeUTF(
                        record.getStatus() != null
                                ? record.getStatus()
                                : "");

                /*
                 * BigDecimal -> String jer DataOutputStream
                 * nema direktnu podršku za BigDecimal.
                 */
                dos.writeUTF(
                        record.getTotal() != null
                                ? record.getTotal().toString()
                                : "0"
                );
            }

        } catch (IOException e) {

            throw new RuntimeException(
                    "Could not write binary order archive file.",
                    e
            );
        }
    }
}