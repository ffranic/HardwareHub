package hr.tvz.ntp.smartordersystem.controller;

import hr.tvz.ntp.smartordersystem.model.Category;
import hr.tvz.ntp.smartordersystem.service.AdminEditLockService;
import hr.tvz.ntp.smartordersystem.service.CategoryService;
import hr.tvz.ntp.smartordersystem.service.LogHelperService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final LogHelperService logHelperService;
    private final AdminEditLockService adminEditLockService;

    public CategoryController(CategoryService categoryService,
                              LogHelperService logHelperService,
                              AdminEditLockService adminEditLockService) {
        this.categoryService = categoryService;
        this.logHelperService = logHelperService;
        this.adminEditLockService = adminEditLockService;
    }

    @GetMapping
    public ResponseEntity<List<Category>> getAllCategories() {
        return ResponseEntity.ok(categoryService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Category> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.findById(id));
    }

    @PostMapping
    public ResponseEntity<Category> saveCategory(@Valid @RequestBody Category category,
                                                 Authentication authentication,
                                                 HttpServletRequest request) {
        Category savedCategory = categoryService.save(category);

        logHelperService.log(authentication, request, "CREATE_CATEGORY",
                "Created category with id " + savedCategory.getId() + " and name " + savedCategory.getName());

        return ResponseEntity.status(HttpStatus.CREATED).body(savedCategory);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Category> updateCategory(@PathVariable Long id,
                                                   @Valid @RequestBody Category category,
                                                   Authentication authentication,
                                                   HttpServletRequest request) {
        adminEditLockService.acquireLock("category", id, authentication.getName());

        try {
            Category updatedCategory = categoryService.update(id, category);

            logHelperService.log(authentication, request, "UPDATE_CATEGORY",
                    "Updated category with id " + id + " to name " + updatedCategory.getName());

            return ResponseEntity.ok(updatedCategory);
        } finally {
            adminEditLockService.releaseLock("category", id, authentication.getName());
        }
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable Long id,
                               Authentication authentication,
                               HttpServletRequest request) {
        adminEditLockService.acquireLock("category", id, authentication.getName());

        try {
            categoryService.delete(id);

            logHelperService.log(authentication, request, "DELETE_CATEGORY",
                    "Deleted category with id " + id);
        } finally {
            adminEditLockService.releaseLock("category", id, authentication.getName());
        }
    }

    @ExceptionHandler({NoSuchElementException.class, IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<?> handleExceptions(RuntimeException ex) {
        HttpStatus status;

        if (ex instanceof NoSuchElementException) {
            status = HttpStatus.NOT_FOUND;
        } else if (ex instanceof IllegalStateException) {
            status = HttpStatus.CONFLICT;
        } else {
            status = HttpStatus.BAD_REQUEST;
        }

        return ResponseEntity.status(status).body(Map.of("error", ex.getMessage()));
    }
}