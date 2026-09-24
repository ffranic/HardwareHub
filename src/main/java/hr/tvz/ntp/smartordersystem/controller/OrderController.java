package hr.tvz.ntp.smartordersystem.controller;

import hr.tvz.ntp.smartordersystem.model.Order;
import hr.tvz.ntp.smartordersystem.model.Status;
import hr.tvz.ntp.smartordersystem.model.User;
import hr.tvz.ntp.smartordersystem.repository.UserRepository;
import hr.tvz.ntp.smartordersystem.service.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/orders")
public class OrderController {
    private final OrderService orderService;
    private final UserRepository userRepository;
    private final OrderPdfAsyncService orderPdfAsyncService;
    private final EmailService emailService;
    private final LogHelperService  logHelperService;

    public OrderController(OrderService orderService,
                           UserRepository userRepository,
                           OrderPdfAsyncService orderPdfAsyncService,
                           EmailService emailService,
                           LogHelperService logHelperService) {
        this.orderService = orderService;
        this.userRepository = userRepository;
        this.orderPdfAsyncService = orderPdfAsyncService;
        this.emailService = emailService;
        this.logHelperService = logHelperService;
    }

    private Long getCurrentUserId(Authentication authentication) {
        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return user.getId();
    }

    @GetMapping("/admin/orders")
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/my")
    public ResponseEntity<List<Order>> getMyOrders(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        return ResponseEntity.ok(orderService.getOrdersByUserId(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrder(@PathVariable Long id,
                                          Authentication authentication) {
        Long userId = getCurrentUserId(authentication);

        Order order = orderService.getOrderById(id)
                .orElseThrow(() -> new NoSuchElementException("Order not found"));

        if (!order.getUser().getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(order);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Order> updateOrderStatus(@PathVariable Long id,
                                                   @RequestBody Map<String, String> requestBody,
                                                   Authentication authentication,
                                                   HttpServletRequest request) {
        Status status = Status.valueOf(requestBody.get("status"));
        Order updatedOrder = orderService.updateStatus(id, status);

        logHelperService.log(
                authentication,
                request,
                "UPDATE_ORDER_STATUS",
                "Changed status for order id " + id + " to " + status
        );

        return ResponseEntity.ok(updatedOrder);
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadOrderPdf(@PathVariable Long id,
                                                   Authentication authentication,
                                                   HttpServletRequest request) {
        Long userId = getCurrentUserId(authentication);

        Order order = orderService.getOrderById(id)
                .orElseThrow(() -> new NoSuchElementException("Order not found"));

        if (!order.getUser().getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (!orderPdfAsyncService.pdfExists(id)) {
            orderPdfAsyncService.generateAndStoreOrderPdf(id);

            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .header("Content-Type", "text/plain")
                    .body(("PDF confirmation is still being generated. Please try again shortly.").getBytes());
        }

        byte[] pdfBytes = orderPdfAsyncService.readStoredPdf(id);

        logHelperService.log(
                authentication,
                request,
                "DOWNLOAD_ORDER_PDF",
                "Downloaded stored PDF confirmation for order id " + id
        );

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=order-" + id + ".pdf")
                .header("Content-Type", "application/pdf")
                .body(pdfBytes);
    }

}
