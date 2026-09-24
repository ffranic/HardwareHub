package hr.tvz.ntp.smartordersystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PdfBatchBenchmarkDto {

    private int ordersProcessed;
    private int threadPoolSize;
    private long sequentialDurationMs;
    private long parallelDurationMs;
    private boolean fasterWithThreadPool;

}
