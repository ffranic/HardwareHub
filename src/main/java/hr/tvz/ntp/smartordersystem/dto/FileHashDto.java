package hr.tvz.ntp.smartordersystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileHashDto {

    private String fileName;
    private String algorithm;
    private String hash;
}
