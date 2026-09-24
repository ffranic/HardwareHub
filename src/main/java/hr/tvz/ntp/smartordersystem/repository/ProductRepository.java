package hr.tvz.ntp.smartordersystem.repository;

import hr.tvz.ntp.smartordersystem.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Product findById(long id);
}
