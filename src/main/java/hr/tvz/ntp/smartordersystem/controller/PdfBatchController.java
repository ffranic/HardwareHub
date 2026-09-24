package hr.tvz.ntp.smartordersystem.controller;

import hr.tvz.ntp.smartordersystem.dto.PdfBatchBenchmarkDto;
import hr.tvz.ntp.smartordersystem.dto.PdfBatchResultDto;
import hr.tvz.ntp.smartordersystem.service.LogHelperService;
import hr.tvz.ntp.smartordersystem.service.PdfBatchService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/pdf-batch")
public class PdfBatchController {

    private final PdfBatchService pdfBatchService;
    private final LogHelperService logHelperService;

    public PdfBatchController(PdfBatchService pdfBatchService,
                              LogHelperService logHelperService) {
        this.pdfBatchService = pdfBatchService;
        this.logHelperService = logHelperService;
    }

    @PostMapping("/generate")
    public ResponseEntity<PdfBatchResultDto> generateBatch(Authentication authentication,
                                                           HttpServletRequest request) {
        PdfBatchResultDto result = pdfBatchService.generateOrderPdfBatch();

        logHelperService.log(
                authentication,
                request,
                "GENERATE_ORDER_PDF_BATCH",
                "Generated order PDF batch using Thread Pool. Orders processed: " + result.getOrdersProcessed()
        );

        return ResponseEntity.ok(result);
    }

    @PostMapping("/benchmark")
    public ResponseEntity<PdfBatchBenchmarkDto> benchmark(Authentication authentication,
                                                          HttpServletRequest request) {
        PdfBatchBenchmarkDto result = pdfBatchService.benchmarkPdfGeneration();

        logHelperService.log(
                authentication,
                request,
                "BENCHMARK_ORDER_PDF_GENERATION",
                "Compared sequential and parallel PDF generation. Orders processed: " + result.getOrdersProcessed()
        );

        return ResponseEntity.ok(result);
    }
}
