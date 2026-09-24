package hr.tvz.ntp.smartordersystem.service;

import hr.tvz.ntp.smartordersystem.model.Order;
import hr.tvz.ntp.smartordersystem.model.Status;

import java.util.List;
import java.util.Optional;

public interface OrderService {

    List<Order> getOrdersByUserId(Long userId);
    Optional<Order> getOrderById(Long id);
    Order updateStatus(Long orderId, Status status);
    List<Order> getAllOrders();
}
