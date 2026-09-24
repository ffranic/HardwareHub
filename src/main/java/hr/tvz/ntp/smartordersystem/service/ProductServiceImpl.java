package hr.tvz.ntp.smartordersystem.service;

import hr.tvz.ntp.smartordersystem.model.Category;
import hr.tvz.ntp.smartordersystem.model.Product;
import hr.tvz.ntp.smartordersystem.repository.CategoryRepository;
import hr.tvz.ntp.smartordersystem.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/*
 * SERVICE LAYER ZA PROIZVODE.
 *
 * Pokriva:
 *
 * - CRUD proizvoda
 * - dohvat kategorije preko ID-a
 * - spremanje slike proizvoda kao byte[]
 * - update proizvoda sa ili bez nove slike
 *
 * OBRANA:
 * Slike se dobivaju preko MultipartFile i pretvaraju u byte[],
 * što se dalje sprema u BLOB polje Product entiteta.
 */
@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductServiceImpl(
            ProductRepository productRepository,
            CategoryRepository categoryRepository) {

        this.productRepository =
                productRepository;

        this.categoryRepository =
                categoryRepository;
    }

    /*
     * READ ALL.
     */
    @Override
    public List<Product> findAllProducts() {
        return productRepository.findAll();
    }

    /*
     * READ ONE.
     *
     * Ovdje vraćamo Optional jer controller može elegantno
     * pretvoriti empty u HTTP 404.
     */
    @Override
    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }

    /*
     * CREATE / generički save.
     */
    @Override
    public Product save(Product product) {
        return productRepository.save(product);
    }

    /*
     * DELETE.
     */
    @Override
    public void deleteById(Long id) {

        /*
         * Prije deletea eksplicitno provjeravamo postoji li proizvod.
         */
        if (!productRepository.existsById(id)) {

            throw new EntityNotFoundException(
                    "Product with id: "
                            + id
                            + " not found"
            );
        }

        productRepository.deleteById(id);
    }

    /*
     * UPDATE bez MultipartFile requesta.
     */
    @Override
    public Product update(
            Long id,
            Product updatedProduct) {

        /*
         * Prvo dohvatimo managed postojeći entitet.
         */
        Product existingProduct =
                productRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new NoSuchElementException(
                                        "Product not found"));

        /*
         * Prepisujemo dozvoljena polja.
         */
        existingProduct.setName(
                updatedProduct.getName());

        existingProduct.setBrand(
                updatedProduct.getBrand());

        existingProduct.setCategory(
                updatedProduct.getCategory());

        existingProduct.setPrice(
                updatedProduct.getPrice());

        existingProduct.setDescription(
                updatedProduct.getDescription());

        existingProduct.setStock(
                updatedProduct.getStock());

        /*
         * Ako update request nema sliku,
         * zadržava se postojeća slika.
         *
         * Ne želimo je slučajno postaviti na null.
         */
        if (updatedProduct.getImage() != null
                && updatedProduct
                .getImage()
                .length > 0) {

            existingProduct.setImage(
                    updatedProduct.getImage());
        }

        return productRepository
                .save(existingProduct);
    }

    /*
     * CREATE S MULTIPART SLIKOM.
     */
    @Override
    public Product saveWithImage(
            String name,
            String brand,
            Long categoryId,
            BigDecimal price,
            String description,
            int stock,
            MultipartFile imageFile)
            throws IOException {

        /*
         * Frontend šalje categoryId.
         *
         * Prije kreiranja Product entiteta dohvaćamo stvarni
         * Category objekt iz baze.
         */
        Category category =
                categoryRepository
                        .findById(categoryId)
                        .orElseThrow(() ->
                                new NoSuchElementException(
                                        "Category not found"));

        Product product =
                new Product();

        product.setName(name);
        product.setBrand(brand);
        product.setCategory(category);
        product.setPrice(price);
        product.setDescription(description);
        product.setStock(stock);

        /*
         * MultipartFile -> byte[].
         *
         * Taj byte[] se kroz Product entity sprema u BLOB.
         */
        if (imageFile != null
                && !imageFile.isEmpty()) {

            product.setImage(
                    imageFile.getBytes());
        }

        return productRepository
                .save(product);
    }

    /*
     * UPDATE S MULTIPART SLIKOM.
     */
    @Override
    public Product updateWithImage(
            Long id,
            String name,
            String brand,
            Long categoryId,
            BigDecimal price,
            String description,
            int stock,
            MultipartFile image)
            throws IOException {

        /*
         * Dohvat proizvoda kojeg uređujemo.
         */
        Product existingProduct =
                productRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new NoSuchElementException(
                                        "Product not found"));

        /*
         * Dohvat odabrane kategorije.
         */
        Category category =
                categoryRepository
                        .findById(categoryId)
                        .orElseThrow(() ->
                                new NoSuchElementException(
                                        "Category not found"));

        existingProduct.setName(name);
        existingProduct.setBrand(brand);
        existingProduct.setCategory(category);
        existingProduct.setPrice(price);
        existingProduct.setDescription(description);
        existingProduct.setStock(stock);

        /*
         * Nova slika se sprema samo ako je stvarno uploadana.
         *
         * Ako korisnik uređuje ime/cijenu bez nove slike,
         * stara slika ostaje sačuvana.
         */
        if (image != null
                && !image.isEmpty()) {

            existingProduct.setImage(
                    image.getBytes());
        }

        return productRepository
                .save(existingProduct);
    }
}