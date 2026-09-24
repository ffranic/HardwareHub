package hr.tvz.ntp.smartordersystem.service;

import hr.tvz.ntp.smartordersystem.model.Product;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductService {
    List<Product> findAllProducts();
    Optional<Product> findById(Long id);
    Product save(Product product);
    void deleteById(Long id);
    Product update(Long id,  Product updatedProduct);
    Product saveWithImage(String name,
                          String brand,
                          Long categoryId,
                          BigDecimal price,
                          String description,
                          int stock,
                          MultipartFile image) throws IOException;
    Product updateWithImage(Long id,
                            String name,
                            String brand,
                            Long categoryId,
                            BigDecimal price,
                            String description,
                            int stock,
                            MultipartFile image) throws IOException;
}
