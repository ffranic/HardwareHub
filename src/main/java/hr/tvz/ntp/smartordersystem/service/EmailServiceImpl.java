package hr.tvz.ntp.smartordersystem.service;

import hr.tvz.ntp.smartordersystem.model.Order;
import hr.tvz.ntp.smartordersystem.repository.OrderRepository;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
public class EmailServiceImpl implements EmailService {
    private final JavaMailSender mailSender;
    private final OrderRepository orderRepository;
    private final PdfService pdfService;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailServiceImpl(JavaMailSender mailSender,
                            OrderRepository orderRepository,
                            PdfService pdfService) {
        this.mailSender = mailSender;
        this.orderRepository = orderRepository;
        this.pdfService = pdfService;
    }

    @Override
    public void sendOrderPdfToUser(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found"));

        try {
            byte[] pdfBytes = pdfService.generateOrderPdf(orderId);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromEmail);
            helper.setTo(order.getUser().getEmail());
            helper.setSubject("Order Confirmation #" + order.getId());
            helper.setText(
                    "Hello " + order.getUser().getUsername() + ",\n\n" +
                            "Attached is your order confirmation PDF for order #" + order.getId() + ".\n\n" +
                            "Thank you for your purchase.\n" +
                            "Smart Order System"
            );

            helper.addAttachment(
                    "order-" + order.getId() + ".pdf",
                    new ByteArrayResource(pdfBytes)
            );

            mailSender.send(message);

        } catch (Exception e) {
            throw new RuntimeException("Failed to send email with PDF attachment", e);
        }
    }
}
