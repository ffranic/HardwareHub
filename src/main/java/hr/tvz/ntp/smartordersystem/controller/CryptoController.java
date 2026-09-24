package hr.tvz.ntp.smartordersystem.controller;

import hr.tvz.ntp.smartordersystem.dto.AesFileResultDto;
import hr.tvz.ntp.smartordersystem.dto.FileHashDto;
import hr.tvz.ntp.smartordersystem.dto.SecureArchiveResultDto;
import hr.tvz.ntp.smartordersystem.model.User;
import hr.tvz.ntp.smartordersystem.repository.UserRepository;
import hr.tvz.ntp.smartordersystem.service.CryptoService;
import hr.tvz.ntp.smartordersystem.service.LogHelperService;
import hr.tvz.ntp.smartordersystem.service.PasswordPreHashService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.NoSuchElementException;

/*
 * KRIPTOGRAFSKI / DEMONSTRACIJSKI CONTROLLER.
 *
 * VAŽNO ZA FINALNO STANJE PROJEKTA:
 *
 * Dio endpointa ovdje pripada starijoj implementaciji:
 * - activity-logs.json AES demonstracija
 * - secure-log-archive RSA/AES demonstracija
 *
 * Aktualni:
 * - AES audit logging -> JsonActivityLogServiceImpl
 * - RSA/AES report -> DigitalSignatureServiceImpl
 *
 * Ipak, hash endpoint za product-stock.xml može još biti stvarno korišten.
 */
@RestController
@RequestMapping("/crypto")
public class CryptoController {

    private final CryptoService cryptoService;
    private final LogHelperService logHelperService;
    private final UserRepository userRepository;
    private final PasswordPreHashService passwordPreHashService;

    public CryptoController(
            CryptoService cryptoService,
            LogHelperService logHelperService,
            UserRepository userRepository,
            PasswordPreHashService passwordPreHashService) {

        this.cryptoService = cryptoService;
        this.logHelperService = logHelperService;
        this.userRepository = userRepository;
        this.passwordPreHashService = passwordPreHashService;
    }

    /*
     * GET /crypto/hash/activity-logs
     *
     * LEGACY:
     * hashira stari activity-logs.json file.
     *
     * Aktualni encrypted audit log sustav koristi vlastite
     * .sha256 datoteke u JsonActivityLogServiceImpl.
     */
    @GetMapping("/hash/activity-logs")
    public ResponseEntity<FileHashDto> hashActivityLogs(
            Authentication authentication,
            HttpServletRequest request) {

        FileHashDto hash =
                cryptoService.hashActivityLogs();

        logHelperService.log(
                authentication,
                request,
                "HASH_ACTIVITY_LOGS",
                "Calculated SHA-256 hash for activity-logs.json"
        );

        return ResponseEntity.ok(hash);
    }

    /*
     * GET /crypto/hash/product-stock
     *
     * Računa SHA-256 hash product-stock.xml datoteke.
     *
     * Ovaj endpoint može imati stvarnu primjenu kao provjera
     * integriteta XML snapshota.
     */
    @GetMapping("/hash/product-stock")
    public ResponseEntity<FileHashDto> hashProductStockXml(
            Authentication authentication,
            HttpServletRequest request) {

        FileHashDto hash =
                cryptoService.hashProductStockXml();

        logHelperService.log(
                authentication,
                request,
                "HASH_PRODUCT_STOCK_XML",
                "Calculated SHA-256 hash for product-stock.xml"
        );

        return ResponseEntity.ok(hash);
    }

    /*
     * POST /crypto/aes/encrypt-logs
     *
     * LEGACY demonstracija.
     *
     * Ručno pretvara:
     *
     * activity-logs.json
     * ->
     * activity-logs.enc
     *
     * Finalni audit sustav ovo više ne treba jer se šifriranje
     * odvija automatski u JsonActivityLogServiceImpl.
     */
    @PostMapping("/aes/encrypt-logs")
    public ResponseEntity<AesFileResultDto> encryptActivityLogs(
            Authentication authentication,
            HttpServletRequest request) {

        AesFileResultDto result =
                cryptoService.encryptActivityLogs();

        logHelperService.log(
                authentication,
                request,
                "AES_ENCRYPT_ACTIVITY_LOGS",
                "Encrypted activity-logs.json to activity-logs.enc"
        );

        return ResponseEntity.ok(result);
    }

    /*
     * GET /crypto/aes/decrypt-logs
     *
     * LEGACY ručna AES demonstracija.
     */
    @GetMapping("/aes/decrypt-logs")
    public ResponseEntity<String> decryptActivityLogs(
            Authentication authentication,
            HttpServletRequest request) {

        String decryptedContent =
                cryptoService.decryptActivityLogs();

        logHelperService.log(
                authentication,
                request,
                "AES_DECRYPT_ACTIVITY_LOGS",
                "Decrypted activity-logs.enc for demonstration"
        );

        return ResponseEntity.ok(decryptedContent);
    }

    /*
     * POST /crypto/rsa/secure-log-archive
     *
     * LEGACY demonstracija hibridne AES + RSA kriptografije.
     *
     * Aktualna hibridna implementacija postoji nad
     * signature-verification-report.json u DigitalSignatureServiceImpl.
     */
    @PostMapping("/rsa/secure-log-archive")
    public ResponseEntity<SecureArchiveResultDto> createSecureLogArchive(
            Authentication authentication,
            HttpServletRequest request) {

        SecureArchiveResultDto result =
                cryptoService.createSecureLogArchive();

        logHelperService.log(
                authentication,
                request,
                "CREATE_SECURE_LOG_ARCHIVE",
                "Created secure log archive using AES-GCM and RSA-OAEP"
        );

        return ResponseEntity.ok(result);
    }

    /*
     * GET /crypto/rsa/decrypt-log-archive
     *
     * Obrnuti legacy postupak:
     * RSA private key -> AES key -> AES decrypt loga.
     */
    @GetMapping("/rsa/decrypt-log-archive")
    public ResponseEntity<String> decryptSecureLogArchive(
            Authentication authentication,
            HttpServletRequest request) {

        String decryptedContent =
                cryptoService.decryptSecureLogArchive();

        logHelperService.log(
                authentication,
                request,
                "DECRYPT_SECURE_LOG_ARCHIVE",
                "Decrypted secure log archive using RSA private key and AES key"
        );

        return ResponseEntity.ok(decryptedContent);
    }

    /*
     * POST /crypto/password-pepper/check
     *
     * Demonstrira pronalaženje troznamenkastog peppera
     * iz ograničene domene 000-999.
     *
     * Request:
     *
     * {
     *   "username": "...",
     *   "password": "..."
     * }
     */
    @PostMapping("/password-pepper/check")
    public ResponseEntity<?> checkPepper(
            @RequestBody Map<String, String> request) {

        String username =
                request.get("username");

        String password =
                request.get("password");

        User user =
                userRepository
                        .findByUsername(username)
                        .orElseThrow(() ->
                                new NoSuchElementException(
                                        "User not found"));

        /*
         * Service isprobava:
         *
         * 000
         * 001
         * ...
         * 999
         *
         * i za svaki kandidat radi:
         *
         * SHA256(password:salt:pepper)
         * ->
         * BCrypt.matches(...)
         */
        String foundPepper =
                passwordPreHashService
                        .findMatchingPepper(
                                username,
                                password,
                                user.getPassword()
                        );

        return ResponseEntity.ok(
                Map.of(
                        "username", username,
                        "pepperFound", foundPepper != null,
                        "pepper", foundPepper != null
                                ? foundPepper
                                : "-",
                        "checkedRange", "000-999"
                )
        );
    }
}