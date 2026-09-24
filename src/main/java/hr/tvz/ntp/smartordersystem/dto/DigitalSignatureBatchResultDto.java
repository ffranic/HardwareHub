package hr.tvz.ntp.smartordersystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DigitalSignatureBatchResultDto {

    private int totalChecked;
    private int validCount;
    private int invalidCount;
    private int threadPoolSize;
    private long durationMs;
    private List<DigitalSignatureResultDto> results;
}