package hr.tvz.ntp.smartordersystem.service;

import hr.tvz.ntp.smartordersystem.dto.PdfBatchBenchmarkDto;
import hr.tvz.ntp.smartordersystem.dto.PdfBatchResultDto;
import hr.tvz.ntp.smartordersystem.model.Order;
import hr.tvz.ntp.smartordersystem.repository.OrderRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@Transactional
public class PdfBatchServiceImpl implements PdfBatchService {

    private static final int THREAD_POOL_SIZE = 4;
    private static final String OUTPUT_DIR = "order-pdf-batches";

    private final OrderRepository orderRepository;
    private final PdfService pdfService;

    public PdfBatchServiceImpl(OrderRepository orderRepository,
                               PdfService pdfService) {
        this.orderRepository = orderRepository;
        this.pdfService = pdfService;
    }

    @Override
    public PdfBatchResultDto generateOrderPdfBatch() {
        List<Order> orders = orderRepository.findAll();

        if (orders.isEmpty()) {
            return new PdfBatchResultDto(
                    0,
                    THREAD_POOL_SIZE,
                    0,
                    "-",
                    "No orders available for PDF batch generation."
            );
        }

        long start = System.currentTimeMillis();

        try {
            Files.createDirectories(Path.of(OUTPUT_DIR));

            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));

            String outputFile = OUTPUT_DIR + "/orders-batch-" + timestamp + ".zip";

            ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

            try {
                List<Future<PdfGenerationResult>> futures = orders.stream()
                        .map(order -> executorService.submit(() -> generatePdfForOrder(order.getId())))
                        .toList();

                try (ZipOutputStream zipOutputStream = new ZipOutputStream(
                        Files.newOutputStream(Path.of(outputFile))
                )) {
                    for (Future<PdfGenerationResult> future : futures) {
                        PdfGenerationResult result = future.get();

                        ZipEntry entry = new ZipEntry("order-" + result.orderId() + ".pdf");
                        zipOutputStream.putNextEntry(entry);
                        zipOutputStream.write(result.pdfBytes());
                        zipOutputStream.closeEntry();
                    }
                }
            } finally {
                executorService.shutdown();
            }

            long duration = System.currentTimeMillis() - start;

            return new PdfBatchResultDto(
                    orders.size(),
                    THREAD_POOL_SIZE,
                    duration,
                    outputFile,
                    "Order PDF batch ZIP generated successfully using Thread Pool."
            );

        } catch (Exception e) {
            throw new RuntimeException("Could not generate order PDF batch.", e);
        }
    }

    @Override
    public PdfBatchBenchmarkDto benchmarkPdfGeneration() {
        List<Order> orders = orderRepository.findAll();

        if (orders.isEmpty()) {
            return new PdfBatchBenchmarkDto(0, THREAD_POOL_SIZE, 0, 0, false);
        }

        long sequentialStart = System.currentTimeMillis();
        generateSequentiallyForBenchmark(orders);
        long sequentialDuration = System.currentTimeMillis() - sequentialStart;

        long parallelStart = System.currentTimeMillis();
        generateParallelForBenchmark(orders);
        long parallelDuration = System.currentTimeMillis() - parallelStart;

        return new PdfBatchBenchmarkDto(
                orders.size(),
                THREAD_POOL_SIZE,
                sequentialDuration,
                parallelDuration,
                parallelDuration < sequentialDuration
        );
    }

    private PdfGenerationResult generatePdfForOrder(Long orderId) {
        byte[] pdfBytes = pdfService.generateOrderPdf(orderId);
        return new PdfGenerationResult(orderId, pdfBytes);
    }

    private void generateSequentiallyForBenchmark(List<Order> orders) {
        for (Order order : orders) {
            pdfService.generateOrderPdf(order.getId());
        }
    }

    private void generateParallelForBenchmark(List<Order> orders) {
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        try {
            List<Callable<byte[]>> tasks = orders.stream()
                    .map(order -> (Callable<byte[]>) () -> pdfService.generateOrderPdf(order.getId()))
                    .toList();

            List<Future<byte[]>> futures = executorService.invokeAll(tasks);

            for (Future<byte[]> future : futures) {
                future.get();
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("PDF benchmark was interrupted.", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("PDF benchmark failed.", e);
        } finally {
            executorService.shutdown();
        }
    }

    private record PdfGenerationResult(Long orderId, byte[] pdfBytes) {
    }
}
