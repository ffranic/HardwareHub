package hr.tvz.ntp.smartordersystem.repository;

import hr.tvz.ntp.smartordersystem.model.Order;
import hr.tvz.ntp.smartordersystem.model.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUser_Id(long id);

    List<Order> findByStatusIn(Collection<Status> statuses);

    @Query("""
           SELECT DISTINCT o
           FROM Order o
           LEFT JOIN FETCH o.user
           LEFT JOIN FETCH o.orderItems oi
           LEFT JOIN FETCH oi.product
           WHERE o.id = :id
           """)
    Optional<Order> findByIdWithItems(@Param("id") Long id);
}