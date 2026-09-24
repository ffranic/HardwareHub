package hr.tvz.ntp.smartordersystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PdfBatchResultDto {

    private int ordersProcessed;
    private int threadPoolSize;
    private long durationMs;
    private String outputFile;
    private String message;

}
