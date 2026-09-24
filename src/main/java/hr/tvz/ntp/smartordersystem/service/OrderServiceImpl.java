package hr.tvz.ntp.smartordersystem.service;

import hr.tvz.ntp.smartordersystem.model.Order;
import hr.tvz.ntp.smartordersystem.model.Status;
import hr.tvz.ntp.smartordersystem.repository.OrderRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final BinaryOrderArchiveService binaryOrderArchiveService;

    public OrderServiceImpl(OrderRepository orderRepository,
                            BinaryOrderArchiveService binaryOrderArchiveService) {
        this.orderRepository = orderRepository;
        this.binaryOrderArchiveService = binaryOrderArchiveService;
    }

    @Override
    public List<Order> getOrdersByUserId(Long userId) {
        return orderRepository.findByUser_Id(userId);
    }

    @Override
    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    @Override
    public Order updateStatus(Long orderId, Status newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found"));

        order.setStatus(newStatus);
        Order savedOrder = orderRepository.save(order);

        if (newStatus == Status.DELIVERED || newStatus == Status.CANCELLED) {
            binaryOrderArchiveService.archiveOrderIfTerminal(orderId);
        }

        return savedOrder;
    }

    @Override
    public List<Order> getAllOrders() {
        return orderRepository.findByStatusIn(List.of(
                Status.NEW,
                Status.PROCESSING,
                Status.SHIPPED
        ));
    }
}