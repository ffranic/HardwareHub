package hr.tvz.ntp.smartordersystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SecureArchiveResultDto {

    private String sourceFile;
    private String encryptedFile;
    private String encryptedKeyFile;
    private String algorithm;
    private String message;

}
