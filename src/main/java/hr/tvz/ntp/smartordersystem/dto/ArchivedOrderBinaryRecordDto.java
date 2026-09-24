package hr.tvz.ntp.smartordersystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ArchivedOrderBinaryRecordDto {

    private Long orderId;
    private String username;
    private String email;
    private String orderDate;
    private String archivedAt;
    private String status;
    private BigDecimal total;
}