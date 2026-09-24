package hr.tvz.ntp.smartordersystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DigitalSignatureResultDto {

    private Long orderId;
    private String algorithm;
    private boolean valid;
    private String message;

}
