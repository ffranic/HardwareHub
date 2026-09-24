package hr.tvz.ntp.smartordersystem.service;

import hr.tvz.ntp.smartordersystem.dto.PdfBatchBenchmarkDto;
import hr.tvz.ntp.smartordersystem.dto.PdfBatchResultDto;

public interface PdfBatchService {

    PdfBatchResultDto generateOrderPdfBatch();
    PdfBatchBenchmarkDto benchmarkPdfGeneration();

}
