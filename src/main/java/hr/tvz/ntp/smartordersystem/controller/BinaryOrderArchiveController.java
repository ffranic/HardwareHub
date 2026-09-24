package hr.tvz.ntp.smartordersystem.controller;

import hr.tvz.ntp.smartordersystem.dto.ArchivedOrderBinaryRecordDto;
import hr.tvz.ntp.smartordersystem.service.BinaryOrderArchiveService;
import hr.tvz.ntp.smartordersystem.service.LogHelperService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/order-archive")
public class BinaryOrderArchiveController {

    private final BinaryOrderArchiveService binaryOrderArchiveService;
    private final LogHelperService logHelperService;

    public BinaryOrderArchiveController(BinaryOrderArchiveService binaryOrderArchiveService,
                                        LogHelperService logHelperService) {
        this.binaryOrderArchiveService = binaryOrderArchiveService;
        this.logHelperService = logHelperService;
    }

    @GetMapping
    public ResponseEntity<Map<String, List<ArchivedOrderBinaryRecordDto>>> readArchivedOrders(
            Authentication authentication,
            HttpServletRequest request) {

        Map<String, List<ArchivedOrderBinaryRecordDto>> records =
                binaryOrderArchiveService.readArchivedOrders();

        logHelperService.log(
                authentication,
                request,
                "READ_BINARY_ORDER_ARCHIVE",
                "Read daily binary archive of completed and cancelled orders"
        );

        return ResponseEntity.ok(records);
    }
}