package hr.tvz.ntp.smartordersystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SignatureVerificationReportDto {

    private String generatedAt;
    private String algorithm;
    private int totalOrders;
    private int validOrders;
    private int invalidOrders;

    private List<SignatureVerificationEntryDto> orders;
}
