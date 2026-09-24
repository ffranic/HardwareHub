package hr.tvz.ntp.smartordersystem.repository;

import hr.tvz.ntp.smartordersystem.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Category findById(long id);
    boolean existsByName(String name);
}
