package hr.tvz.ntp.smartordersystem.controller;

import hr.tvz.ntp.smartordersystem.model.Product;
import hr.tvz.ntp.smartordersystem.service.AdminEditLockService;
import hr.tvz.ntp.smartordersystem.service.LogHelperService;
import hr.tvz.ntp.smartordersystem.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

/*
 * REST CONTROLLER ZA PRODUCT RESURS.
 *
 * Ovdje se mapiraju HTTP requestovi prema ProductService sloju.
 *
 * Glavne funkcionalnosti:
 *
 * GET    -> čitanje proizvoda
 * POST   -> kreiranje proizvoda
 * PUT    -> izmjena proizvoda
 * DELETE -> brisanje proizvoda
 *
 * Dodatno:
 * - dohvat slike proizvoda
 * - multipart upload slike
 * - audit logging
 * - admin edit lock
 */
@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;
    private final LogHelperService logHelperService;
    private final AdminEditLockService adminEditLockService;

    /*
     * Constructor injection.
     *
     * Controller ne radi direktno s bazom,
     * nego delegira ProductServiceu.
     */
    public ProductController(
            ProductService productService,
            LogHelperService logHelperService,
            AdminEditLockService adminEditLockService) {

        this.productService = productService;
        this.logHelperService = logHelperService;
        this.adminEditLockService = adminEditLockService;
    }


    /*
     * GET /products
     *
     * READ ALL.
     *
     * OBRANA:
     * Ovo je endpoint kojeg products.js koristi za dohvat
     * svih proizvoda iz baze.
     */
    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {

        /*
         * ProductService -> ProductRepository -> baza.
         */
        return ResponseEntity.ok(
                productService.findAllProducts()
        );
    }


    /*
     * GET /products/{id}
     *
     * READ ONE.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Product> findById(
            @PathVariable Long id) {

        /*
         * ProductService vraća Optional<Product>.
         *
         * Ako postoji:
         * -> HTTP 200
         *
         * Ako ne postoji:
         * -> HTTP 404
         */
        return productService
                .findById(id)
                .map(ResponseEntity::ok)
                .orElse(
                        ResponseEntity
                                .notFound()
                                .build()
                );
    }


    /*
     * POST /products
     *
     * CREATE običnog Product JSON objekta.
     */
    @PostMapping
    public ResponseEntity<Product> saveProduct(
            @Valid @RequestBody Product product,
            Authentication authentication,
            HttpServletRequest request) {

        /*
         * @RequestBody:
         * JSON request body -> Product Java objekt.
         *
         * @Valid:
         * aktivira Bean Validation ako Product ima validation anotacije.
         */
        Product savedProduct =
                productService.save(product);

        /*
         * Audit zapis administrativne akcije.
         */
        logHelperService.log(
                authentication,
                request,
                "CREATE_PRODUCT",
                "Created product with id "
                        + savedProduct.getId()
                        + " and name "
                        + savedProduct.getName()
        );

        /*
         * HTTP 201 Created.
         */
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(savedProduct);
    }


    /*
     * PUT /products/{id}
     *
     * UPDATE proizvoda bez multipart slike.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody Product product,
            Authentication authentication,
            HttpServletRequest request) {

        /*
         * ADMIN EDIT LOCK.
         *
         * Prije izmjene pokušavamo rezervirati konkretni
         * product ID za trenutnog administratora.
         *
         * OBRANA:
         * Ovo je aplikacijski edit-lock mehanizam,
         * ali ga više ne koristimo kao glavni primjer
         * za profesorov zahtjev međusobnog zaključavanja dretvi.
         */
        adminEditLockService.acquireLock(
                "product",
                id,
                authentication.getName()
        );

        try {

            /*
             * Stvarni update je u ProductServiceImpl.
             */
            Product updatedProduct =
                    productService.update(
                            id,
                            product
                    );

            logHelperService.log(
                    authentication,
                    request,
                    "UPDATE_PRODUCT",
                    "Updated product with id "
                            + id
            );

            return ResponseEntity.ok(
                    updatedProduct
            );

        } finally {

            /*
             * Lock se mora osloboditi i ako update baci exception.
             *
             * Zato release ide u finally.
             */
            adminEditLockService.releaseLock(
                    "product",
                    id,
                    authentication.getName()
            );
        }
    }


    /*
     * DELETE /products/{id}
     *
     * DELETE operacija.
     */
    @DeleteMapping("/{id}")

    /*
     * Ako metoda normalno završi,
     * HTTP status je 204 No Content.
     */
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProductById(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest request) {

        /*
         * I delete koristi aplikacijski edit lock
         * za konkretni Product.
         */
        adminEditLockService.acquireLock(
                "product",
                id,
                authentication.getName()
        );

        try {

            productService.deleteById(id);

            logHelperService.log(
                    authentication,
                    request,
                    "DELETE_PRODUCT",
                    "Deleted product with id "
                            + id
            );

        } finally {

            adminEditLockService.releaseLock(
                    "product",
                    id,
                    authentication.getName()
            );
        }
    }


    /*
     * GET /products/{id}/image
     *
     * Poseban endpoint za dohvat BLOB slike proizvoda.
     *
     * OBRANA:
     * Product JSON ne vraća image zbog @JsonIgnore.
     * Slika se dohvaća odvojeno samo kada je potrebna.
     */
    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> getProductImage(
            @PathVariable Long id) {

        Product product =
                productService
                        .findById(id)
                        .orElseThrow(() ->
                                new NoSuchElementException(
                                        "Product not found"));

        /*
         * Ako proizvod nema sliku:
         * HTTP 404.
         */
        if (product.getImage() == null
                || product.getImage().length == 0) {

            return ResponseEntity
                    .notFound()
                    .build();
        }

        /*
         * Vraćamo byte[] kao HTTP response body.
         */
        return ResponseEntity.ok()

                /*
                 * Browseru govorimo da body predstavlja JPEG sliku.
                 *
                 * NAPOMENA:
                 * Ovo pretpostavlja JPEG, čak i ako je uploadana druga
                 * image ekstenzija.
                 */
                .contentType(
                        MediaType.IMAGE_JPEG)

                /*
                 * Browser smije cacheirati sliku 1 sat.
                 *
                 * Time se smanjuje broj ponovnih requestova za iste slike.
                 */
                .cacheControl(
                        CacheControl.maxAge(
                                1,
                                TimeUnit.HOURS))

                /*
                 * inline znači:
                 * pokušaj prikazati sadržaj u browseru,
                 * umjesto prisilnog downloada.
                 */
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"product-"
                                + id
                                + ".jpg\""
                )

                /*
                 * byte[] iz BLOB stupca postaje response body.
                 */
                .body(
                        product.getImage());
    }


    /*
     * POST /products/with-image
     *
     * CREATE proizvoda s multipart form-data requestom.
     *
     * OBRANA:
     * JSON nije pogodan za normalan upload binarne slike,
     * zato se koristi multipart/form-data.
     */
    @PostMapping(
            value = "/with-image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<Product> saveProductWithImage(

            /*
             * @RequestParam čita pojedina polja multipart requesta.
             */
            @RequestParam String name,
            @RequestParam String brand,
            @RequestParam Long categoryId,
            @RequestParam BigDecimal price,
            @RequestParam int stock,

            /*
             * description nije obavezan.
             */
            @RequestParam(required = false)
            String description,

            /*
             * MultipartFile predstavlja uploadanu sliku.
             *
             * required=false:
             * proizvod može biti spremljen bez slike.
             */
            @RequestParam(required = false)
            MultipartFile image,

            Authentication authentication,
            HttpServletRequest request
    ) throws IOException {

        /*
         * Service:
         *
         * - dohvaća Category prema categoryId
         * - kreira Product
         * - MultipartFile pretvara u byte[]
         * - sprema Product u bazu
         */
        Product savedProduct =
                productService.saveWithImage(
                        name,
                        brand,
                        categoryId,
                        price,
                        description,
                        stock,
                        image
                );

        logHelperService.log(
                authentication,
                request,
                "CREATE_PRODUCT_WITH_IMAGE",
                "Created product with id "
                        + savedProduct.getId()
                        + " and name "
                        + savedProduct.getName()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(savedProduct);
    }


    /*
     * PUT /products/{id}/with-image
     *
     * UPDATE proizvoda s mogućnošću uploadanja nove slike.
     */
    @PutMapping(
            value = "/{id}/with-image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<Product> updateProductWithImage(

            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam String brand,
            @RequestParam Long categoryId,
            @RequestParam BigDecimal price,
            @RequestParam int stock,
            @RequestParam(required = false)
            String description,
            @RequestParam(required = false)
            MultipartFile image,
            Authentication authentication,
            HttpServletRequest request

    ) throws IOException {

        /*
         * Zaključavamo konkretni product edit.
         */
        adminEditLockService.acquireLock(
                "product",
                id,
                authentication.getName()
        );

        try {

            Product updatedProduct =
                    productService.updateWithImage(
                            id,
                            name,
                            brand,
                            categoryId,
                            price,
                            description,
                            stock,
                            image
                    );

            logHelperService.log(
                    authentication,
                    request,
                    "UPDATE_PRODUCT_WITH_IMAGE",
                    "Updated product with id "
                            + id
            );

            return ResponseEntity.ok(
                    updatedProduct
            );

        } finally {

            adminEditLockService.releaseLock(
                    "product",
                    id,
                    authentication.getName()
            );
        }
    }


    /*
     * LOKALNI EXCEPTION HANDLER ZA NOT FOUND.
     *
     * Ako neka metoda ovog controllera baci NoSuchElementException,
     * pretvara se u HTTP 404.
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<?> handleNoSuchElementException(
            NoSuchElementException ex) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(
                        Map.of(
                                "error",
                                ex.getMessage()
                        )
                );
    }


    /*
     * Ako AdminEditLockService javi da je resurs zaključan,
     * vraćamo HTTP 409 Conflict.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> handleLockedResource(
            IllegalStateException ex) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(
                        Map.of(
                                "error",
                                ex.getMessage()
                        )
                );
    }
}