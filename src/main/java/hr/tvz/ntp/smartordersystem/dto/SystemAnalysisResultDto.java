package hr.tvz.ntp.smartordersystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SystemAnalysisResultDto {
    private String outputFile;
    private int threadPoolSize;
    private long durationMs;
    private String content;
}