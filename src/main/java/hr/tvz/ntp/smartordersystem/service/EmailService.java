package hr.tvz.ntp.smartordersystem.service;

public interface EmailService {
    void sendOrderPdfToUser(Long orderId);
}
