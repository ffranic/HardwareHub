package hr.tvz.ntp.smartordersystem.repository;

import hr.tvz.ntp.smartordersystem.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    OrderItem findById(long id);
}
