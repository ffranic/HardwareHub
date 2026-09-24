package hr.tvz.ntp.smartordersystem.service;

import hr.tvz.ntp.smartordersystem.dto.AesFileResultDto;
import hr.tvz.ntp.smartordersystem.dto.FileHashDto;
import hr.tvz.ntp.smartordersystem.dto.SecureArchiveResultDto;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Service
@Transactional
public class CryptoServiceImpl  implements CryptoService {

    private static final String SHA_256 = "SHA-256";

    private static final String AES_GCM = "AES/GCM/NoPadding";
    private static final String AES_ALGORITHM = "AES";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH_BYTES = 12;

    private static final String ACTIVITY_LOGS_JSON = "activity-logs.json";
    private static final String ACTIVITY_LOGS_ENC = "activity-logs.enc";

    private static final String RSA_ALGORITHM = "RSA";
    private static final String RSA_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";

    private static final String SECURE_ARCHIVE_DIR = "secure-log-archive";
    private static final String SECURE_LOG_FILE = "secure-log-archive/activity-logs.secure.enc";
    private static final String SECURE_AES_KEY_FILE = "secure-log-archive/activity-logs.aeskey.rsa";
    private static final String SECURE_PRIVATE_KEY_FILE = "secure-log-archive/private.key";
    private static final String SECURE_PUBLIC_KEY_FILE = "secure-log-archive/public.key";

    private static final String AES_KEY_TEXT = "SmartOrderSystemDemoAESKey2026!";

    @Override
    public FileHashDto hashActivityLogs() {
        return hashFile("activity-logs.json");
    }

    @Override
    public FileHashDto hashProductStockXml() {
        return hashFile("product-stock.xml");
    }

    @Override
    public FileHashDto hashFile(String fileName) {
        try {
            File file = new File(fileName);

            if (!file.exists()) {
                throw new IllegalArgumentException("File does not exist: " + fileName);
            }

            MessageDigest digest = MessageDigest.getInstance(SHA_256);

            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead = fis.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }

            String hash = bytesToHex(digest.digest());

            return new FileHashDto(fileName, SHA_256, hash);

        } catch (Exception e) {
            throw new RuntimeException("Could not calculate SHA-256 hash for file: " + fileName, e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();

        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }

        return result.toString();
    }

    @Override
    public AesFileResultDto encryptActivityLogs() {
        try {
            Path sourcePath = Path.of(ACTIVITY_LOGS_JSON);
            Path outputPath = Path.of(ACTIVITY_LOGS_ENC);

            if (!Files.exists(sourcePath)) {
                throw new IllegalArgumentException("Source file does not exist: " + ACTIVITY_LOGS_JSON);
            }

            byte[] plainBytes = Files.readAllBytes(sourcePath);

            byte[] iv = new byte[IV_LENGTH_BYTES];
            SecureRandom secureRandom = new SecureRandom();
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.ENCRYPT_MODE, getAesKey(), new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] encryptedBytes = cipher.doFinal(plainBytes);

            String encodedIv = Base64.getEncoder().encodeToString(iv);
            String encodedEncryptedData = Base64.getEncoder().encodeToString(encryptedBytes);

            String fileContent = encodedIv + System.lineSeparator() + encodedEncryptedData;

            Files.writeString(outputPath, fileContent, StandardCharsets.UTF_8);

            return new AesFileResultDto(
                    ACTIVITY_LOGS_JSON,
                    ACTIVITY_LOGS_ENC,
                    "AES-GCM",
                    "Activity logs encrypted successfully."
            );

        } catch (Exception e) {
            throw new RuntimeException("Could not encrypt activity logs file.", e);
        }
    }

    @Override
    public String decryptActivityLogs() {
        try {
            Path encryptedPath = Path.of(ACTIVITY_LOGS_ENC);

            if (!Files.exists(encryptedPath)) {
                throw new IllegalArgumentException("Encrypted file does not exist: " + ACTIVITY_LOGS_ENC);
            }

            String fileContent = Files.readString(encryptedPath, StandardCharsets.UTF_8);
            String[] parts = fileContent.split("\\R", 2);

            if (parts.length != 2) {
                throw new IllegalStateException("Invalid encrypted file format.");
            }

            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] encryptedBytes = Base64.getDecoder().decode(parts[1]);

            Cipher cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.DECRYPT_MODE, getAesKey(), new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

            return new String(decryptedBytes, StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new RuntimeException("Could not decrypt activity logs file.", e);
        }
    }

    private SecretKey getAesKey() {
        byte[] keyBytes = AES_KEY_TEXT.getBytes(StandardCharsets.UTF_8);

        byte[] normalizedKey = new byte[32];

        for (int i = 0; i < normalizedKey.length; i++) {
            normalizedKey[i] = i < keyBytes.length ? keyBytes[i] : 0;
        }

        return new SecretKeySpec(normalizedKey, AES_ALGORITHM);
    }

    @Override
    public SecureArchiveResultDto createSecureLogArchive() {
        try {
            Files.createDirectories(Path.of(SECURE_ARCHIVE_DIR));

            Path sourcePath = Path.of(ACTIVITY_LOGS_JSON);

            if (!Files.exists(sourcePath)) {
                throw new IllegalArgumentException("Source file does not exist: " + ACTIVITY_LOGS_JSON);
            }

            KeyPair rsaKeyPair = getOrCreateRsaKeyPair();

            KeyGenerator keyGenerator = KeyGenerator.getInstance(AES_ALGORITHM);
            keyGenerator.init(256);
            SecretKey generatedAesKey = keyGenerator.generateKey();

            byte[] iv = new byte[IV_LENGTH_BYTES];
            new SecureRandom().nextBytes(iv);

            Cipher aesCipher = Cipher.getInstance(AES_GCM);
            aesCipher.init(
                    Cipher.ENCRYPT_MODE,
                    generatedAesKey,
                    new GCMParameterSpec(GCM_TAG_LENGTH, iv)
            );

            byte[] plainBytes = Files.readAllBytes(sourcePath);
            byte[] encryptedLogBytes = aesCipher.doFinal(plainBytes);

            String encryptedLogFileContent =
                    Base64.getEncoder().encodeToString(iv)
                            + System.lineSeparator()
                            + Base64.getEncoder().encodeToString(encryptedLogBytes);

            Files.writeString(
                    Path.of(SECURE_LOG_FILE),
                    encryptedLogFileContent,
                    StandardCharsets.UTF_8
            );

            Cipher rsaCipher = Cipher.getInstance(RSA_TRANSFORMATION);
            rsaCipher.init(Cipher.ENCRYPT_MODE, rsaKeyPair.getPublic());

            byte[] encryptedAesKeyBytes = rsaCipher.doFinal(generatedAesKey.getEncoded());

            Files.writeString(
                    Path.of(SECURE_AES_KEY_FILE),
                    Base64.getEncoder().encodeToString(encryptedAesKeyBytes),
                    StandardCharsets.UTF_8
            );

            return new SecureArchiveResultDto(
                    ACTIVITY_LOGS_JSON,
                    SECURE_LOG_FILE,
                    SECURE_AES_KEY_FILE,
                    "AES-GCM + RSA-OAEP",
                    "Secure log archive created successfully."
            );

        } catch (Exception e) {
            throw new RuntimeException("Could not create secure log archive.", e);
        }
    }

    @Override
    public String decryptSecureLogArchive() {
        try {
            KeyPair rsaKeyPair = getOrCreateRsaKeyPair();

            Path encryptedLogPath = Path.of(SECURE_LOG_FILE);
            Path encryptedKeyPath = Path.of(SECURE_AES_KEY_FILE);

            if (!Files.exists(encryptedLogPath)) {
                throw new IllegalArgumentException("Encrypted log file does not exist: " + SECURE_LOG_FILE);
            }

            if (!Files.exists(encryptedKeyPath)) {
                throw new IllegalArgumentException("Encrypted AES key file does not exist: " + SECURE_AES_KEY_FILE);
            }

            byte[] encryptedAesKeyBytes = Base64.getDecoder().decode(
                    Files.readString(encryptedKeyPath, StandardCharsets.UTF_8)
            );

            Cipher rsaCipher = Cipher.getInstance(RSA_TRANSFORMATION);
            rsaCipher.init(Cipher.DECRYPT_MODE, rsaKeyPair.getPrivate());

            byte[] aesKeyBytes = rsaCipher.doFinal(encryptedAesKeyBytes);
            SecretKey restoredAesKey = new SecretKeySpec(aesKeyBytes, AES_ALGORITHM);

            String encryptedLogFileContent = Files.readString(encryptedLogPath, StandardCharsets.UTF_8);
            String[] parts = encryptedLogFileContent.split("\\R", 2);

            if (parts.length != 2) {
                throw new IllegalStateException("Invalid encrypted log archive format.");
            }

            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] encryptedLogBytes = Base64.getDecoder().decode(parts[1]);

            Cipher aesCipher = Cipher.getInstance(AES_GCM);
            aesCipher.init(
                    Cipher.DECRYPT_MODE,
                    restoredAesKey,
                    new GCMParameterSpec(GCM_TAG_LENGTH, iv)
            );

            byte[] decryptedBytes = aesCipher.doFinal(encryptedLogBytes);

            return new String(decryptedBytes, StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new RuntimeException("Could not decrypt secure log archive.", e);
        }
    }

    private KeyPair getOrCreateRsaKeyPair() throws Exception {
        Files.createDirectories(Path.of(SECURE_ARCHIVE_DIR));

        Path privateKeyPath = Path.of(SECURE_PRIVATE_KEY_FILE);
        Path publicKeyPath = Path.of(SECURE_PUBLIC_KEY_FILE);

        if (Files.exists(privateKeyPath) && Files.exists(publicKeyPath)) {
            byte[] privateKeyBytes = Base64.getDecoder().decode(
                    Files.readString(privateKeyPath, StandardCharsets.UTF_8)
            );

            byte[] publicKeyBytes = Base64.getDecoder().decode(
                    Files.readString(publicKeyPath, StandardCharsets.UTF_8)
            );

            KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);

            PrivateKey privateKey = keyFactory.generatePrivate(
                    new PKCS8EncodedKeySpec(privateKeyBytes)
            );

            PublicKey publicKey = keyFactory.generatePublic(
                    new X509EncodedKeySpec(publicKeyBytes)
            );

            return new KeyPair(publicKey, privateKey);
        }

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(RSA_ALGORITHM);
        keyPairGenerator.initialize(2048);

        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        Files.writeString(
                privateKeyPath,
                Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()),
                StandardCharsets.UTF_8
        );

        Files.writeString(
                publicKeyPath,
                Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()),
                StandardCharsets.UTF_8
        );

        return keyPair;
    }
}
