package hr.tvz.ntp.smartordersystem.service;

public interface PdfService {
    byte[] generateOrderPdf(Long orderId);
}