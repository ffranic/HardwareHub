package hr.tvz.ntp.smartordersystem.controller;

import hr.tvz.ntp.smartordersystem.dto.DigitalSignatureBatchResultDto;
import hr.tvz.ntp.smartordersystem.dto.DigitalSignatureResultDto;
import hr.tvz.ntp.smartordersystem.dto.SignatureVerificationReportDto;
import hr.tvz.ntp.smartordersystem.service.DigitalSignatureService;
import hr.tvz.ntp.smartordersystem.service.LogHelperService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.NoSuchElementException;

/*
 * REST CONTROLLER ZA DIGITALNE POTPISE I FORENZIČKI REPORT.
 *
 * OBRANA:
 * Sama kriptografija nije u controlleru.
 *
 * Controller:
 * HTTP <-> Service
 *
 * DigitalSignatureServiceImpl:
 * RSA / AES / Thread Pool / JSON / file I/O
 */
@RestController
@RequestMapping("/digital-signature")
public class DigitalSignatureController {

    private final DigitalSignatureService digitalSignatureService;
    private final LogHelperService logHelperService;

    public DigitalSignatureController(
            DigitalSignatureService digitalSignatureService,
            LogHelperService logHelperService) {

        this.digitalSignatureService =
                digitalSignatureService;

        this.logHelperService =
                logHelperService;
    }

    /*
     * GET /digital-signature/orders/{orderId}/verify
     *
     * Verificira jednu narudžbu.
     */
    @GetMapping("/orders/{orderId}/verify")
    public ResponseEntity<DigitalSignatureResultDto>
    verifyOrderSignature(
            @PathVariable Long orderId,
            Authentication authentication,
            HttpServletRequest request) {

        /*
         * RSA verifikacija nad spremljenim originalnim sadržajem.
         */
        DigitalSignatureResultDto result =
                digitalSignatureService
                        .verifyOrderSignature(orderId);

        logHelperService.log(
                authentication,
                request,
                "VERIFY_ORDER_SIGNATURE",
                "Verified digital signature for order id "
                        + orderId
        );

        return ResponseEntity.ok(result);
    }

    /*
     * GET /digital-signature/orders/verify-all
     *
     * OBRANA:
     * Ovaj endpoint aktivira Thread Pool funkcionalnost.
     */
    @GetMapping("/orders/verify-all")
    public ResponseEntity<DigitalSignatureBatchResultDto>
    verifyAllOrderSignatures(
            Authentication authentication,
            HttpServletRequest request) {

        /*
         * DigitalSignatureServiceImpl:
         *
         * - fixed thread pool = 3
         * - task po narudžbi
         * - paralelna RSA verifikacija
         * - usporedba original/current
         * - JSON report
         * - AES/RSA encrypted kopija
         */
        DigitalSignatureBatchResultDto result =
                digitalSignatureService
                        .verifyAllOrderSignatures();

        logHelperService.log(
                authentication,
                request,
                "VERIFY_ALL_ORDER_SIGNATURES",
                "Verified all order signatures using thread pool with "
                        + result.getThreadPoolSize()
                        + " threads"
        );

        return ResponseEntity.ok(result);
    }

    /*
     * GET /digital-signature/report
     *
     * Čita posljednji forenzički report.
     *
     * OBRANA:
     * Ako encrypted verzija postoji, service:
     *
     * RSA private key
     * -> dešifrira AES key
     * -> AES-GCM dešifrira JSON
     * -> ObjectMapper rekonstruira DTO.
     */
    @GetMapping("/report")
    public ResponseEntity<SignatureVerificationReportDto>
    readVerificationReport(
            Authentication authentication,
            HttpServletRequest request) {

        SignatureVerificationReportDto report =
                digitalSignatureService
                        .readVerificationReport();

        logHelperService.log(
                authentication,
                request,
                "READ_SIGNATURE_VERIFICATION_REPORT",
                "Read signature verification JSON report"
        );

        return ResponseEntity.ok(report);
    }

    /*
     * DELETE /digital-signature/report
     *
     * Briše:
     *
     * .json
     * .json.enc
     * .key.rsa
     * .iv
     */
    @DeleteMapping("/report")
    public ResponseEntity<Void>
    deleteVerificationReport(
            Authentication authentication,
            HttpServletRequest request) {

        digitalSignatureService
                .deleteVerificationReport();

        logHelperService.log(
                authentication,
                request,
                "DELETE_SIGNATURE_VERIFICATION_REPORT",
                "Deleted signature verification JSON report"
        );

        return ResponseEntity
                .noContent()
                .build();
    }

    /*
     * LOKALNI EXCEPTION HANDLER.
     *
     * Ako servis baci NoSuchElementException,
     * pretvaramo je u HTTP 404.
     *
     * Bez ovoga bi takva exception često završila kao generički 500.
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<?> handleNotFound(
            NoSuchElementException ex) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(
                        Map.of(
                                "error",
                                ex.getMessage()
                        )
                );
    }
}