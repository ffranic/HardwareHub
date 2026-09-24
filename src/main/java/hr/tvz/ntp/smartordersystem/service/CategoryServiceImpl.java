package hr.tvz.ntp.smartordersystem.service;

import hr.tvz.ntp.smartordersystem.model.Category;
import hr.tvz.ntp.smartordersystem.repository.CategoryRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

/*
 * SERVICE LAYER za upravljanje kategorijama.
 *
 * Controller ne pristupa bazi direktno, nego:
 *
 * Controller
 *    ↓
 * CategoryService
 *    ↓
 * CategoryServiceImpl
 *    ↓
 * CategoryRepository
 *    ↓
 * MySQL
 *
 * Ovdje se nalazi poslovna logika vezana uz kategorije,
 * npr. provjera dupliciranih naziva.
 */
@Service
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    /*
     * Constructor injection repository dependencyja.
     */
    public CategoryServiceImpl(
            CategoryRepository categoryRepository) {

        this.categoryRepository =
                categoryRepository;
    }

    /*
     * READ ALL
     *
     * Dohvaća sve kategorije iz baze.
     */
    @Override
    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    /*
     * CREATE
     *
     * Prije spremanja provjerava postoji li kategorija
     * istog naziva.
     */
    @Override
    public Category save(Category category) {

        if (categoryRepository
                .existsByName(category.getName())) {

            throw new IllegalStateException(
                    "Category with name "
                            + category.getName()
                            + " already exists"
            );
        }

        return categoryRepository.save(category);
    }

    /*
     * UPDATE
     *
     * Prima ID kategorije koju mijenjamo i nove podatke.
     */
    @Override
    public Category update(
            Long id,
            Category category) {

        /*
         * Prvo tražimo postojeći zapis.
         *
         * Ako ga nema -> 404 se kasnije može mapirati
         * kroz controller/exception handler.
         */
        Category existing =
                categoryRepository.findById(id)
                        .orElseThrow(() ->
                                new NoSuchElementException(
                                        "Category not found with id: "
                                                + id));

        /*
         * Provjera dupliciranog naziva.
         *
         * Ne smijemo uspoređivati samu kategoriju sa sobom,
         * zato:
         *
         * !c.getId().equals(id)
         *
         * Zatim case-insensitive provjeravamo ime.
         */
        boolean duplicateExists =
                categoryRepository.findAll()
                        .stream()
                        .anyMatch(c ->
                                !c.getId().equals(id)
                                        && c.getName()
                                        .equalsIgnoreCase(
                                                category.getName()
                                        )
                        );

        if (duplicateExists) {
            throw new IllegalArgumentException(
                    "Category with that name already exists."
            );
        }

        /*
         * Ne stvaramo novi Category objekt.
         *
         * Mijenjamo postojeći managed entity.
         */
        existing.setName(category.getName());

        return categoryRepository.save(existing);
    }

    /*
     * READ BY ID
     */
    @Override
    public Category findById(Long id) {

        return categoryRepository.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Category not found"));
    }

    /*
     * DELETE
     */
    @Override
    public void delete(Long id) {

        /*
         * Prije deletea provjeravamo postoji li zapis,
         * kako bismo mogli vratiti smisleniju grešku.
         */
        if (!categoryRepository.existsById(id)) {
            throw new NoSuchElementException(
                    "Category not found"
            );
        }

        categoryRepository.deleteById(id);
    }
}