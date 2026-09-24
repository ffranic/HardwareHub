package hr.tvz.ntp.smartordersystem.controller;

import hr.tvz.ntp.smartordersystem.model.AddToCartRequest;
import hr.tvz.ntp.smartordersystem.model.Cart;
import hr.tvz.ntp.smartordersystem.model.Order;
import hr.tvz.ntp.smartordersystem.model.User;
import hr.tvz.ntp.smartordersystem.repository.UserRepository;
import hr.tvz.ntp.smartordersystem.service.CartService;
import hr.tvz.ntp.smartordersystem.service.DigitalSignatureService;
import hr.tvz.ntp.smartordersystem.service.LogHelperService;
import hr.tvz.ntp.smartordersystem.service.OrderPdfAsyncService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/*
 * REST CONTROLLER ZA KOŠARICU I CHECKOUT.
 *
 * OBRANA:
 * Controller koordinira više poslovnih servisa kod checkouta:
 *
 * CartService
 * -> kreira Order i smanjuje stock
 *
 * DigitalSignatureService
 * -> digitalno potpisuje novu narudžbu
 *
 * OrderPdfAsyncService
 * -> generira PDF potvrdu
 *
 * LogHelperService
 * -> zapisuje audit događaj
 */
@RestController
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;
    private final UserRepository userRepository;
    private final LogHelperService logHelperService;
    private final OrderPdfAsyncService orderPdfAsyncService;
    private final DigitalSignatureService digitalSignatureService;

    public CartController(
            CartService cartService,
            UserRepository userRepository,
            LogHelperService logHelperService,
            OrderPdfAsyncService orderPdfAsyncService,
            DigitalSignatureService digitalSignatureService) {

        this.cartService = cartService;
        this.userRepository = userRepository;
        this.logHelperService = logHelperService;
        this.orderPdfAsyncService = orderPdfAsyncService;
        this.digitalSignatureService = digitalSignatureService;
    }

    /*
     * Helper metoda:
     * Authentication sadrži username, ali CartService radi s userId.
     *
     * Zato username -> User -> ID.
     */
    private Long getCurrentUserId(
            Authentication authentication) {

        String username =
                authentication.getName();

        User user =
                userRepository
                        .findByUsername(username)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "User not found"));

        return user.getId();
    }

    /*
     * GET /cart
     *
     * Vraća košaricu trenutno prijavljenog korisnika.
     */
    @GetMapping
    public ResponseEntity<Cart> getCart(
            Authentication authentication) {

        Long userId =
                getCurrentUserId(authentication);

        return ResponseEntity.ok(
                cartService.getCartForUser(userId)
        );
    }

    /*
     * POST /cart/items
     *
     * Dodaje proizvod u košaricu.
     */
    @PostMapping("/items")
    public ResponseEntity<Cart> addToCart(
            @RequestBody AddToCartRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        Long userId =
                getCurrentUserId(authentication);

        Cart cart =
                cartService.addToCart(
                        userId,
                        request
                );

        /*
         * Audit zapis:
         * tko je dodao koji proizvod i koju količinu.
         */
        logHelperService.log(
                authentication,
                httpRequest,
                "ADD_TO_CART",
                "Added product id "
                        + request.productId()
                        + " to cart with quantity "
                        + request.quantity()
        );

        return ResponseEntity.ok(cart);
    }

    /*
     * PUT /cart/items/{itemId}
     *
     * Mijenja quantity jedne CartItem stavke.
     *
     * Request body:
     *
     * {
     *   "quantity": 3
     * }
     */
    @PutMapping("/items/{itemId}")
    public ResponseEntity<Cart> updateQuantity(
            @PathVariable Long itemId,
            @RequestBody Map<String, Integer> request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        Long userId =
                getCurrentUserId(authentication);

        Integer quantity =
                request.get("quantity");

        /*
         * Service provjerava:
         * - pripada li item ovom korisniku
         * - ima li dovoljno stocka
         * - treba li item obrisati ako je quantity <= 0
         */
        Cart cart =
                cartService
                        .updateCartItemQuantity(
                                userId,
                                itemId,
                                quantity
                        );

        logHelperService.log(
                authentication,
                httpRequest,
                "UPDATE_CART_ITEM_QUANTITY",
                "Updated cart item id "
                        + itemId
                        + " to quantity "
                        + quantity
        );

        return ResponseEntity.ok(cart);
    }

    /*
     * DELETE /cart/items/{itemId}
     */
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> removeItem(
            @PathVariable Long itemId,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        Long userId =
                getCurrentUserId(authentication);

        /*
         * Service dodatno provjerava ownership CartItema.
         */
        cartService.removeCartItem(
                userId,
                itemId
        );

        logHelperService.log(
                authentication,
                httpRequest,
                "REMOVE_CART_ITEM",
                "Removed cart item id "
                        + itemId
        );

        /*
         * HTTP 204 No Content.
         */
        return ResponseEntity
                .noContent()
                .build();
    }

    /*
     * POST /cart/checkout
     *
     * Najvažniji workflow ovog controllera.
     */
    @PostMapping("/checkout")
    public ResponseEntity<Order> checkout(
            Authentication authentication,
            HttpServletRequest httpRequest) {

        Long userId =
                getCurrentUserId(authentication);

        /*
         * 1. CartService checkout:
         *
         * - koristi Semaphore po productId
         * - provjerava finalni stock
         * - smanjuje stock
         * - kreira Order
         * - kreira OrderIteme
         * - prazni Cart
         */
        Order order =
                cartService.checkout(userId);

        /*
         * 2. Digitalni potpis se radi ODMAH nakon nastanka narudžbe.
         *
         * OBRANA:
         * Time .original predstavlja stanje narudžbe
         * u trenutku kupnje.
         */
        digitalSignatureService
                .signOrder(
                        order.getId());

        /*
         * 3. Generiranje/spremanje PDF-a narudžbe.
         *
         * Naziv AsyncService sugerira da se obrada odvaja od glavnog
         * request workflowa; konkretan način ovisi o njegovoj implementaciji.
         */
        orderPdfAsyncService
                .generateAndStoreOrderPdf(
                        order.getId());

        /*
         * 4. Audit događaj.
         */
        logHelperService.log(
                authentication,
                httpRequest,
                "CHECKOUT",
                "Created order with id "
                        + order.getId()
        );

        return ResponseEntity.ok(order);
    }
}