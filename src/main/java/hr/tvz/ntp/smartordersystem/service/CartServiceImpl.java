package hr.tvz.ntp.smartordersystem.service;

import hr.tvz.ntp.smartordersystem.model.*;
import hr.tvz.ntp.smartordersystem.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

/*
 * Servis s poslovnom logikom košarice.
 *
 * Omogućuje:
 * - dohvat/kreiranje košarice
 * - dodavanje proizvoda
 * - promjenu količine
 * - uklanjanje proizvoda
 * - checkout
 *
 * OBRANA:
 * Checkout sadrži važan konkurentni mehanizam:
 * Semaphore(1) po proizvodu za zaštitu zalihe.
 */
@Service
@Transactional
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    /*
     * Za svaki productId držimo zaseban Semaphore.
     *
     * Primjer:
     *
     * product 5  -> Semaphore(1)
     * product 10 -> Semaphore(1)
     * product 27 -> Semaphore(1)
     *
     * Time dva checkouta istog proizvoda ne mogu istovremeno
     * mijenjati njegov stock.
     *
     * ConcurrentHashMap je thread-safe mapa, pa joj više dretvi
     * smije pristupati paralelno.
     */
    private final ConcurrentHashMap<Long, Semaphore> productStockSemaphores =
            new ConcurrentHashMap<>();

    public CartServiceImpl(CartRepository cartRepository,
                           CartItemRepository cartItemRepository,
                           ProductRepository productRepository,
                           UserRepository userRepository,
                           OrderRepository orderRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    /*
     * Dohvaća košaricu korisnika.
     *
     * Ako korisnik još nema košaricu, automatski je kreira.
     */
    @Override
    public Cart getCartForUser(Long userId) {

        return cartRepository.findByUserId(userId)

                /*
                 * orElseGet se izvršava samo ako košarica nije pronađena.
                 */
                .orElseGet(() -> {

                    User user = userRepository.findById(userId)
                            .orElseThrow(() ->
                                    new NoSuchElementException(
                                            "User not found"));

                    Cart cart = new Cart();

                    cart.setUser(user);
                    cart.setCreatedAt(LocalDateTime.now());

                    // Inicijaliziramo praznu kolekciju stavki.
                    cart.setItems(new ArrayList<>());

                    return cartRepository.save(cart);
                });
    }

    /*
     * Dodavanje proizvoda u košaricu.
     */
    @Override
    public Cart addToCart(Long userId, AddToCartRequest request) {

        Cart cart = getCartForUser(userId);

        Product product =
                productRepository.findById(request.productId())
                        .orElseThrow(() ->
                                new NoSuchElementException(
                                        "Product not found"));

        /*
         * Već pri dodavanju provjeravamo postoji li dovoljna zaliha.
         *
         * VAŽNO:
         * Konačna provjera se ponovno radi u checkoutu.
         *
         * Stock se između "Add to cart" i "Checkout" može promijeniti.
         */
        if (product.getStock() < request.quantity()) {
            throw new IllegalArgumentException(
                    "Not enough stock available"
            );
        }

        /*
         * Provjeravamo postoji li već isti proizvod u istoj košarici.
         */
        CartItem existingItem = cartItemRepository
                .findByCart_IdAndProduct_Id(
                        cart.getId(),
                        product.getId()
                )
                .orElse(null);

        if (existingItem != null) {

            /*
             * Ako već postoji, samo povećavamo quantity.
             */
            existingItem.setQuantity(
                    existingItem.getQuantity() + request.quantity()
            );

            cartItemRepository.save(existingItem);

        } else {

            /*
             * Inače kreiramo novi CartItem.
             */
            CartItem newItem = new CartItem();

            newItem.setCart(cart);
            newItem.setProduct(product);
            newItem.setQuantity(request.quantity());

            cart.getItems().add(newItem);

            cartItemRepository.save(newItem);
        }

        // Vraćamo svježe stanje košarice iz baze.
        return cartRepository.findById(cart.getId())
                .orElseThrow();
    }

    /*
     * Promjena količine stavke u košarici.
     */
    @Override
    public Cart updateCartItemQuantity(
            Long userId,
            Long cartItemId,
            int quantity) {

        Cart cart = getCartForUser(userId);

        CartItem item =
                cartItemRepository.findById(cartItemId)
                        .orElseThrow(() ->
                                new NoSuchElementException(
                                        "Cart item not found"));

        /*
         * Sigurnosna provjera vlasništva.
         *
         * Korisnik ne smije preko ID-a mijenjati stavku tuđe košarice.
         */
        if (!item.getCart().getId().equals(cart.getId())) {
            throw new IllegalArgumentException(
                    "This cart item does not belong to the current user"
            );
        }

        /*
         * Quantity <= 0 interpretiramo kao uklanjanje stavke.
         */
        if (quantity <= 0) {

            cartItemRepository.delete(item);

        } else {

            if (item.getProduct().getStock() < quantity) {
                throw new IllegalArgumentException(
                        "Not enough stock available"
                );
            }

            item.setQuantity(quantity);
            cartItemRepository.save(item);
        }

        return cartRepository.findById(cart.getId())
                .orElseThrow();
    }

    /*
     * Potpuno uklanjanje stavke iz košarice.
     */
    @Override
    public void removeCartItem(
            Long userId,
            Long cartItemId) {

        Cart cart = getCartForUser(userId);

        CartItem item =
                cartItemRepository.findById(cartItemId)
                        .orElseThrow(() ->
                                new NoSuchElementException(
                                        "Cart item not found"));

        /*
         * Opet provjeravamo da stavka pripada trenutnom korisniku.
         */
        if (!item.getCart().getId().equals(cart.getId())) {
            throw new IllegalArgumentException(
                    "This cart item does not belong to the current user"
            );
        }

        cartItemRepository.delete(item);
    }

    /*
     * CHECKOUT
     *
     * Najvažnija metoda klase.
     *
     * Pretvara sadržaj košarice u Order i OrderItem zapise,
     * smanjuje zalihe te prazni košaricu.
     *
     * Uz to koristi Semaphore za zaštitu zaliha kod paralelnih kupnji.
     */
    @Override
    public Order checkout(Long userId) {

        Cart cart = getCartForUser(userId);

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }

        /*
         * VAŽNO ZA DEADLOCK.
         *
         * Sve proizvode sortiramo prema ID-u prije zaključavanja.
         *
         * Tako sve dretve uvijek zauzimaju semafore istim redoslijedom.
         *
         * Npr. uvijek:
         *
         * Product 3 -> Product 8 -> Product 15
         *
         * a nikad jedna dretva 3->8, a druga 8->3.
         *
         * Time eliminiramo klasično kružno čekanje.
         */
        List<CartItem> sortedItems = cart.getItems()
                .stream()
                .sorted(
                        Comparator.comparing(
                                item -> item.getProduct().getId()
                        )
                )
                .toList();

        /*
         * Ovdje pamtimo koje smo semafore uspješno zauzeli.
         *
         * To je potrebno kako bismo ih kasnije sve oslobodili
         * u finally bloku.
         */
        List<Semaphore> acquiredSemaphores =
                new ArrayList<>();

        try {

            /*
             * Prvo zauzimamo semafore svih proizvoda koji sudjeluju
             * u checkoutu.
             */
            for (CartItem cartItem : sortedItems) {

                Long productId =
                        cartItem.getProduct().getId();

                /*
                 * Ako semafor za proizvod ne postoji, kreiramo ga.
                 *
                 * new Semaphore(1)
                 *
                 * znači da postoji samo JEDNA dozvola.
                 *
                 * Dakle samo jedna dretva može u određenom trenutku
                 * obrađivati stock tog proizvoda.
                 */
                Semaphore semaphore =
                        productStockSemaphores.computeIfAbsent(
                                productId,
                                id -> new Semaphore(1)
                        );

                /*
                 * acquire():
                 *
                 * Ako je permit dostupan:
                 * -> uzmi ga i nastavi.
                 *
                 * Ako ga druga dretva već drži:
                 * -> čekaj dok ga ne oslobodi.
                 */
                semaphore.acquire();

                acquiredSemaphores.add(semaphore);
            }

            /*
             * Tek nakon što smo sigurno zaključali potrebne proizvode
             * kreiramo narudžbu.
             */
            Order order = new Order();

            order.setUser(cart.getUser());

            /*
             * withNano(0) uklanja nanosekunde.
             *
             * To je uvedeno jer su razlike u preciznosti vremena između
             * Java objekta i baze ranije uzrokovale probleme kod
             * digitalnog potpisa.
             */
            order.setOrder_date(
                    LocalDateTime.now().withNano(0)
            );

            order.setStatus(Status.NEW);
            order.setOrderItems(new ArrayList<>());

            /*
             * Obrada svakog proizvoda.
             */
            for (CartItem cartItem : sortedItems) {

                /*
                 * Ponovno dohvaćamo proizvod iz baze NAKON što smo
                 * zauzeli semaphore.
                 *
                 * To je važno:
                 *
                 * želimo najsvježije stanje zalihe.
                 */
                Product product =
                        productRepository
                                .findById(
                                        cartItem.getProduct().getId())
                                .orElseThrow(() ->
                                        new NoSuchElementException(
                                                "Product not found"));

                /*
                 * FINALNA provjera zalihe.
                 *
                 * Ovo je ključna provjera, jer se stock mogao promijeniti
                 * nakon dodavanja proizvoda u košaricu.
                 */
                if (product.getStock()
                        < cartItem.getQuantity()) {

                    throw new IllegalArgumentException(
                            "Not enough stock for product: "
                                    + product.getName()
                    );
                }

                // Kreira se snapshot stavke narudžbe.
                OrderItem orderItem = new OrderItem();

                orderItem.setOrder(order);
                orderItem.setProduct(product);
                orderItem.setQuantity(
                        cartItem.getQuantity()
                );

                /*
                 * Spremamo cijenu U TRENUTKU NARUDŽBE.
                 *
                 * Ako Product.price sutra postane drugačiji,
                 * stara narudžba i dalje mora imati izvornu cijenu.
                 */
                orderItem.setPrice_at_order_time(
                        product.getPrice()
                );

                order.getOrderItems().add(orderItem);

                /*
                 * Sigurno smanjujemo zalihu.
                 *
                 * Za isti productId samo jedna dretva može doći do
                 * ovog dijela jer drži njegov Semaphore(1).
                 */
                product.setStock(
                        product.getStock()
                                - cartItem.getQuantity()
                );

                productRepository.save(product);
            }

            /*
             * Cascade konfiguracija na Order -> OrderItem omogućuje
             * spremanje narudžbe zajedno s njenim stavkama.
             */
            Order savedOrder =
                    orderRepository.save(order);

            /*
             * Checkout je uspješan -> praznimo košaricu.
             */
            cartItemRepository.deleteAll(cart.getItems());

            cart.getItems().clear();

            cartRepository.save(cart);

            return savedOrder;

        } catch (InterruptedException e) {

            /*
             * semaphore.acquire() može baciti InterruptedException.
             *
             * Vraćamo interrupt flag trenutnoj dretvi.
             */
            Thread.currentThread().interrupt();

            throw new RuntimeException(
                    "Checkout was interrupted while waiting for product stock lock.",
                    e
            );

        } finally {

            /*
             * KRITIČNO:
             *
             * svi zauzeti semafori MORAJU se osloboditi bez obzira
             * je li checkout završio uspješno ili exceptionom.
             */
            for (Semaphore semaphore : acquiredSemaphores) {
                semaphore.release();
            }
        }
    }
}