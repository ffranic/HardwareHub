package hr.tvz.ntp.smartordersystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SignatureVerificationEntryDto {

    private Long orderId;

    private boolean valid;

    private String verifiedAt;

    private String originalContent;

    private String currentContent;

    private List<String> differences;
}
