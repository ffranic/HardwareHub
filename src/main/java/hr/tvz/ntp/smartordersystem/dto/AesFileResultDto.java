package hr.tvz.ntp.smartordersystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AesFileResultDto {

    private String sourceFile;
    private String outputFile;
    private String algorithm;
    private String message;

}
