package hr.tvz.ntp.smartordersystem.service;

import hr.tvz.ntp.smartordersystem.dto.DigitalSignatureBatchResultDto;
import hr.tvz.ntp.smartordersystem.dto.DigitalSignatureResultDto;
import hr.tvz.ntp.smartordersystem.dto.SignatureVerificationEntryDto;
import hr.tvz.ntp.smartordersystem.dto.SignatureVerificationReportDto;
import hr.tvz.ntp.smartordersystem.model.Order;
import hr.tvz.ntp.smartordersystem.model.OrderItem;
import hr.tvz.ntp.smartordersystem.repository.OrderRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.*;

/*
 * GLAVNI SERVIS ZA DIGITALNE POTPISE I FORENZIČKU PROVJERU NARUDŽBI.
 *
 * Ova klasa ima više povezanih odgovornosti:
 *
 * 1. RSA digitalno potpisivanje narudžbe
 * 2. RSA verifikaciju potpisa
 * 3. paralelnu provjeru svih potpisa preko Thread Pool-a
 * 4. generiranje JSON forenzičkog izvještaja
 * 5. AES-GCM enkripciju tog izvještaja
 * 6. RSA zaštitu AES ključa -> hibridna kriptografija
 *
 * OBRANA:
 * Ovo je vrlo dobar primjer kako se više zahtjeva projekta povezuje
 * u jednu smislenu sigurnosnu funkcionalnost.
 */
@Service
@Transactional
public class DigitalSignatureServiceImpl implements DigitalSignatureService {

    /*
     * SHA256withRSA znači:
     *
     * 1. nad sadržajem se izračuna SHA-256 hash
     * 2. hash se potpisuje RSA privatnim ključem
     *
     * Java Signature API sve to odradi interno.
     */
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    /*
     * Osnovni algoritam za generiranje/rekonstrukciju RSA ključeva.
     */
    private static final String KEY_ALGORITHM = "RSA";

    /*
     * Thread Pool za grupnu verifikaciju svih narudžbi.
     */
    private static final int SIGNATURE_THREAD_POOL_SIZE = 3;

    /*
     * Direktorij i datoteke RSA ključeva.
     */
    private static final String SIGNATURE_DIR = "order-signatures";
    private static final String PRIVATE_KEY_FILE = "order-signatures/private.key";
    private static final String PUBLIC_KEY_FILE = "order-signatures/public.key";

    /*
     * Čitljivi JSON forenzički report.
     *
     * Ovaj file ostaje čitljiv kako bi se pokrio JSON zahtjev.
     */
    private static final String REPORT_FILE =
            "order-signatures/signature-verification-report.json";

    /*
     * AES-GCM šifrirana kopija JSON reporta.
     */
    private static final String REPORT_ENCRYPTED_FILE =
            "order-signatures/signature-verification-report.json.enc";

    /*
     * AES ključ koji je dodatno šifriran RSA javnim ključem.
     */
    private static final String REPORT_KEY_FILE =
            "order-signatures/signature-verification-report.key.rsa";

    /*
     * IV potreban za AES-GCM dešifriranje.
     *
     * IV nije tajan, ali mora biti sačuvan.
     */
    private static final String REPORT_IV_FILE =
            "order-signatures/signature-verification-report.iv";

    /*
     * AES-GCM konfiguracija.
     */
    private static final String AES_TRANSFORMATION = "AES/GCM/NoPadding";

    /*
     * RSA-OAEP se koristi za zaštitu malog AES ključa.
     */
    private static final String RSA_TRANSFORMATION =
            "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";

    /*
     * Standardni 12-byte IV za GCM.
     */
    private static final int GCM_IV_LENGTH = 12;

    /*
     * Authentication tag od 128 bitova.
     */
    private static final int GCM_TAG_LENGTH = 128;

    /*
     * ObjectMapper:
     *
     * Java DTO <-> JSON.
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final OrderRepository orderRepository;

    public DigitalSignatureServiceImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /*
     * DIGITALNO POTPISIVANJE JEDNE NARUDŽBE.
     *
     * Poziva se u trenutku nastanka narudžbe.
     */
    @Override
    public DigitalSignatureResultDto signOrder(Long orderId) {

        try {
            createDirectoryIfNeeded();

            /*
             * Dohvat postojećeg RSA key paira ili generiranje novog.
             */
            KeyPair keyPair = getOrCreateKeyPair();

            /*
             * Kanonski sadržaj narudžbe.
             *
             * Važno je da se isti sadržaj može reproducirati
             * deterministički.
             */
            String originalContent = buildOrderContent(orderId);

            /*
             * Spremamo TOČNO ono što je bilo potpisano.
             *
             * OBRANA:
             * .original služi kao dokaz što je ulazilo u potpis
             * u trenutku nastanka narudžbe.
             */
            Files.writeString(
                    getOriginalFile(orderId).toPath(),
                    originalContent,
                    StandardCharsets.UTF_8
            );

            /*
             * Kreira se Signature objekt za SHA256withRSA.
             */
            Signature signature =
                    Signature.getInstance(SIGNATURE_ALGORITHM);

            /*
             * Potpisivanje koristi PRIVATNI ključ.
             */
            signature.initSign(keyPair.getPrivate());

            /*
             * Predajemo sadržaj koji želimo potpisati.
             */
            signature.update(
                    originalContent.getBytes(StandardCharsets.UTF_8)
            );

            /*
             * signature.sign() daje binarni RSA digitalni potpis.
             *
             * Base64 ga samo pretvara u tekstualni format za spremanje.
             */
            String encodedSignature =
                    Base64.getEncoder()
                            .encodeToString(signature.sign());

            /*
             * Potpis se sprema u zasebnu .sig datoteku.
             */
            Files.writeString(
                    getSignatureFile(orderId).toPath(),
                    encodedSignature,
                    StandardCharsets.UTF_8
            );

            return new DigitalSignatureResultDto(
                    orderId,
                    SIGNATURE_ALGORITHM,
                    true,
                    "Order was digitally signed successfully."
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Could not digitally sign order.",
                    e
            );
        }
    }

    /*
     * VERIFIKACIJA POTPISA JEDNE NARUDŽBE.
     */
    @Override
    public DigitalSignatureResultDto verifyOrderSignature(Long orderId) {

        try {
            createDirectoryIfNeeded();

            KeyPair keyPair = getOrCreateKeyPair();

            File signatureFile = getSignatureFile(orderId);
            File originalFile = getOriginalFile(orderId);

            /*
             * Bez .sig datoteke nema što verificirati.
             */
            if (!signatureFile.exists()) {
                return new DigitalSignatureResultDto(
                        orderId,
                        SIGNATURE_ALGORITHM,
                        false,
                        "Signature file does not exist for this order."
                );
            }

            /*
             * Bez .original datoteke ne znamo što je točno bilo potpisano.
             */
            if (!originalFile.exists()) {
                return new DigitalSignatureResultDto(
                        orderId,
                        SIGNATURE_ALGORITHM,
                        false,
                        "Original signed content does not exist for this order."
                );
            }

            String originalContent =
                    Files.readString(
                            originalFile.toPath(),
                            StandardCharsets.UTF_8
                    );

            String encodedSignature =
                    Files.readString(
                                    signatureFile.toPath(),
                                    StandardCharsets.UTF_8)
                            .trim();

            /*
             * Base64 natrag u originalne byteove potpisa.
             */
            byte[] signatureBytes =
                    Base64.getDecoder()
                            .decode(encodedSignature);

            Signature signature =
                    Signature.getInstance(
                            SIGNATURE_ALGORITHM);

            /*
             * Verifikacija koristi JAVNI ključ.
             */
            signature.initVerify(
                    keyPair.getPublic());

            /*
             * U Signature ubacujemo originalni sadržaj.
             */
            signature.update(
                    originalContent.getBytes(
                            StandardCharsets.UTF_8));

            /*
             * verify vraća true ako:
             *
             * - potpis odgovara sadržaju
             * - potpis je napravljen odgovarajućim privatnim ključem
             */
            boolean valid =
                    signature.verify(signatureBytes);

            return new DigitalSignatureResultDto(
                    orderId,
                    SIGNATURE_ALGORITHM,
                    valid,
                    valid
                            ? "Order signature is valid. Original signed content was not changed."
                            : "Order signature is invalid. Original signed content or signature may have been changed."
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Could not verify digital signature.",
                    e
            );
        }
    }

    /*
     * GRUPNA VERIFIKACIJA SVIH POTPISA.
     *
     * OBRANA:
     * Ovo je implementacija Thread Pool zahtjeva.
     */
    @Override
    public DigitalSignatureBatchResultDto verifyAllOrderSignatures() {

        long start = System.currentTimeMillis();

        /*
         * Dohvaćamo sve order ID-eve.
         */
        List<Long> orderIds =
                orderRepository.findAll()
                        .stream()
                        .map(Order::getId)
                        .toList();

        /*
         * Kreira se pool od 3 dretve.
         */
        ExecutorService executorService =
                Executors.newFixedThreadPool(
                        SIGNATURE_THREAD_POOL_SIZE);

        try {

            /*
             * Za SVAKU narudžbu kreiramo zaseban Callable task.
             *
             * Svaki task:
             *
             * - verificira RSA potpis
             * - čita originalni sadržaj
             * - generira trenutno stanje iz baze
             * - uspoređuje original i trenutno stanje
             */
            List<Callable<SignatureVerificationEntryDto>> tasks =
                    orderIds.stream()
                            .map(orderId ->
                                    (Callable<SignatureVerificationEntryDto>) () -> {

                                        DigitalSignatureResultDto result =
                                                verifyOrderSignature(orderId);

                                        File originalFile =
                                                getOriginalFile(orderId);

                                        String original =
                                                originalFile.exists()
                                                        ? Files.readString(
                                                        originalFile.toPath(),
                                                        StandardCharsets.UTF_8)
                                                        : "Original signed content file is missing.";

                                        /*
                                         * Trenutno stanje ponovno gradimo iz baze.
                                         *
                                         * OBRANA:
                                         * digitalni potpis se verificira nad .original,
                                         * dok se current koristi za forenzičku usporedbu.
                                         */
                                        String current =
                                                buildOrderContent(orderId);

                                        return new SignatureVerificationEntryDto(
                                                orderId,
                                                result.isValid(),
                                                LocalDateTime.now().toString(),
                                                original,
                                                current,
                                                compareContents(
                                                        original,
                                                        current
                                                )
                                        );
                                    })
                            .toList();

            /*
             * Sva tri worker threada mogu paralelno obrađivati taskove.
             *
             * invokeAll čeka da svi završe.
             */
            List<Future<SignatureVerificationEntryDto>> futures =
                    executorService.invokeAll(tasks);

            List<SignatureVerificationEntryDto> reportEntries =
                    new ArrayList<>();

            List<DigitalSignatureResultDto> results =
                    new ArrayList<>();

            /*
             * Nakon završetka taskova rezultate skupljamo sekvencijalno.
             */
            for (Future<SignatureVerificationEntryDto> future : futures) {

                SignatureVerificationEntryDto entry =
                        future.get();

                reportEntries.add(entry);

                /*
                 * Batch API-u trebamo jednostavniji DigitalSignatureResultDto.
                 */
                results.add(
                        new DigitalSignatureResultDto(
                                entry.getOrderId(),
                                SIGNATURE_ALGORITHM,
                                entry.isValid(),
                                entry.isValid()
                                        ? "Valid"
                                        : "Invalid"
                        )
                );
            }

            long validCountLong =
                    reportEntries.stream()
                            .filter(
                                    SignatureVerificationEntryDto::isValid)
                            .count();

            int validCount = (int) validCountLong;

            int invalidCount =
                    reportEntries.size()
                            - validCount;

            long duration =
                    System.currentTimeMillis()
                            - start;

            /*
             * Finalni forenzički JSON report.
             */
            SignatureVerificationReportDto report =
                    new SignatureVerificationReportDto(
                            LocalDateTime.now().toString(),
                            SIGNATURE_ALGORITHM,
                            reportEntries.size(),
                            validCount,
                            invalidCount,
                            reportEntries
                    );

            /*
             * Sprema obični JSON i njegovu kriptografski
             * zaštićenu kopiju.
             */
            saveVerificationReport(report);

            return new DigitalSignatureBatchResultDto(
                    results.size(),
                    validCount,
                    invalidCount,
                    SIGNATURE_THREAD_POOL_SIZE,
                    duration,
                    results
            );

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new RuntimeException(
                    "Signature verification was interrupted.",
                    e
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Signature verification failed.",
                    e
            );

        } finally {
            executorService.shutdown();
        }
    }

    /*
     * ČITANJE FORENZIČKOG REPORTA.
     */
    @Override
    public SignatureVerificationReportDto readVerificationReport() {

        try {

            /*
             * Ako postoji kompletna encrypted verzija,
             * preferiramo nju.
             *
             * Dakle admin kroz View report stvarno demonstrira
             * RSA + AES dekripciju.
             */
            if (encryptedReportExists()) {
                return decryptVerificationReport();
            }

            /*
             * Fallback na obični JSON ako encrypted kopija ne postoji.
             */
            File reportFile = getReportFile();

            if (!reportFile.exists()) {
                throw new NoSuchElementException(
                        "Verification report does not exist."
                );
            }

            return objectMapper.readValue(
                    reportFile,
                    SignatureVerificationReportDto.class
            );

        } catch (NoSuchElementException e) {

            throw e;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Could not read signature verification report.",
                    e
            );
        }
    }

    /*
     * DELETE JSON reporta i svih kriptografskih pratećih datoteka.
     */
    @Override
    public void deleteVerificationReport() {

        try {

            boolean deletedJson =
                    Files.deleteIfExists(
                            Path.of(REPORT_FILE));

            boolean deletedEncrypted =
                    Files.deleteIfExists(
                            Path.of(REPORT_ENCRYPTED_FILE));

            boolean deletedKey =
                    Files.deleteIfExists(
                            Path.of(REPORT_KEY_FILE));

            boolean deletedIv =
                    Files.deleteIfExists(
                            Path.of(REPORT_IV_FILE));

            /*
             * Ako baš ništa nije postojalo,
             * prijavljujemo da report ne postoji.
             */
            if (!deletedJson
                    && !deletedEncrypted
                    && !deletedKey
                    && !deletedIv) {

                throw new NoSuchElementException(
                        "Verification report does not exist."
                );
            }

        } catch (NoSuchElementException e) {

            throw e;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Could not delete signature verification report.",
                    e
            );
        }
    }


    /*
     * Spremanje čitljivog JSON-a.
     */
    private void saveVerificationReport(
            SignatureVerificationReportDto report)
            throws Exception {

        createDirectoryIfNeeded();

        /*
         * writerWithDefaultPrettyPrinter ->
         * ljudski čitljiv JSON.
         */
        objectMapper
                .writerWithDefaultPrettyPrinter()
                .writeValue(
                        new File(REPORT_FILE),
                        report
                );

        /*
         * Nakon spremanja običnog JSON-a,
         * automatski radimo hibridnu enkripciju kopije.
         */
        encryptVerificationReport();
    }

    /*
     * HIBRIDNA AES + RSA ENKRIPCIJA REPORTA.
     *
     * JSON -> AES-GCM
     * AES key -> RSA public key
     */
    private void encryptVerificationReport()
            throws Exception {

        KeyPair keyPair =
                getOrCreateKeyPair();

        /*
         * Učitavamo obični JSON kao byte[].
         */
        byte[] jsonBytes =
                Files.readAllBytes(
                        Path.of(REPORT_FILE));

        /*
         * Za svaki report generiramo NOVI random AES-256 ključ.
         */
        KeyGenerator keyGenerator =
                KeyGenerator.getInstance("AES");

        keyGenerator.init(256);

        SecretKey aesKey =
                keyGenerator.generateKey();

        /*
         * Novi random IV za GCM.
         */
        byte[] iv =
                new byte[GCM_IV_LENGTH];

        new SecureRandom().nextBytes(iv);

        /*
         * AES šifrira stvarni JSON sadržaj.
         */
        Cipher aesCipher =
                Cipher.getInstance(
                        AES_TRANSFORMATION);

        aesCipher.init(
                Cipher.ENCRYPT_MODE,
                aesKey,
                new GCMParameterSpec(
                        GCM_TAG_LENGTH,
                        iv
                )
        );

        byte[] encryptedJson =
                aesCipher.doFinal(jsonBytes);

        /*
         * RSA ne šifrira veliki JSON.
         *
         * RSA šifrira samo mali AES ključ.
         */
        Cipher rsaCipher =
                Cipher.getInstance(
                        RSA_TRANSFORMATION);

        rsaCipher.init(
                Cipher.ENCRYPT_MODE,
                keyPair.getPublic()
        );

        byte[] encryptedAesKey =
                rsaCipher.doFinal(
                        aesKey.getEncoded());

        /*
         * Spremamo tri dijela:
         *
         * encrypted JSON
         * encrypted AES key
         * IV
         */
        Files.write(
                Path.of(REPORT_ENCRYPTED_FILE),
                encryptedJson
        );

        Files.write(
                Path.of(REPORT_KEY_FILE),
                encryptedAesKey
        );

        Files.write(
                Path.of(REPORT_IV_FILE),
                iv
        );
    }

    /*
     * OBRNUTI HIBRIDNI POSTUPAK.
     */
    private SignatureVerificationReportDto decryptVerificationReport()
            throws Exception {

        KeyPair keyPair =
                getOrCreateKeyPair();

        byte[] encryptedJson =
                Files.readAllBytes(
                        Path.of(REPORT_ENCRYPTED_FILE));

        byte[] encryptedAesKey =
                Files.readAllBytes(
                        Path.of(REPORT_KEY_FILE));

        byte[] iv =
                Files.readAllBytes(
                        Path.of(REPORT_IV_FILE));

        /*
         * RSA PRIVATE KEY vraća originalni AES ključ.
         */
        Cipher rsaCipher =
                Cipher.getInstance(
                        RSA_TRANSFORMATION);

        rsaCipher.init(
                Cipher.DECRYPT_MODE,
                keyPair.getPrivate()
        );

        byte[] aesKeyBytes =
                rsaCipher.doFinal(
                        encryptedAesKey);

        /*
         * Rekonstruiramo SecretKey iz dekriptiranih byteova.
         */
        SecretKey aesKey =
                new SecretKeySpec(
                        aesKeyBytes,
                        "AES"
                );

        /*
         * AES-GCM sada dešifrira pravi JSON.
         */
        Cipher aesCipher =
                Cipher.getInstance(
                        AES_TRANSFORMATION);

        aesCipher.init(
                Cipher.DECRYPT_MODE,
                aesKey,
                new GCMParameterSpec(
                        GCM_TAG_LENGTH,
                        iv
                )
        );

        byte[] decryptedJson =
                aesCipher.doFinal(
                        encryptedJson);

        String json =
                new String(
                        decryptedJson,
                        StandardCharsets.UTF_8
                );

        /*
         * JSON string -> Java DTO.
         */
        return objectMapper.readValue(
                json,
                SignatureVerificationReportDto.class
        );
    }

    /*
     * Provjerava postoje li SVA tri potrebna dijela
     * hibridno šifriranog reporta.
     */
    private boolean encryptedReportExists() {

        return Files.exists(
                Path.of(REPORT_ENCRYPTED_FILE))
                && Files.exists(
                Path.of(REPORT_KEY_FILE))
                && Files.exists(
                Path.of(REPORT_IV_FILE));
    }

    /*
     * GRADI DETERMINISTIČKI SADRŽAJ NARUDŽBE.
     *
     * Ovo je izuzetno važno za digitalni potpis.
     *
     * Isti semantički sadržaj mora svaki put proizvesti
     * identičan tekst.
     */
    private String buildOrderContent(Long orderId) {

        /*
         * findByIdWithItems JOIN FETCH-om učitava:
         *
         * Order
         * User
         * OrderItems
         * Product
         *
         * Time izbjegavamo LazyInitializationException.
         */
        Order order =
                orderRepository
                        .findByIdWithItems(orderId)
                        .orElseThrow(() ->
                                new NoSuchElementException(
                                        "Order not found"));

        StringBuilder builder =
                new StringBuilder();

        /*
         * U potpis uključujemo ključne podatke narudžbe.
         */
        builder.append("ORDER_ID=")
                .append(order.getId())
                .append("\n");

        builder.append("USER_ID=")
                .append(order.getUser().getId())
                .append("\n");

        builder.append("USERNAME=")
                .append(order.getUser().getUsername())
                .append("\n");

        builder.append("EMAIL=")
                .append(order.getUser().getEmail())
                .append("\n");

        builder.append("ORDER_DATE=")
                .append(
                        normalizeDate(
                                order.getOrder_date()))
                .append("\n");

        /*
         * OrderIteme sortiramo po ID-u.
         *
         * OBRANA:
         * Redoslijed kolekcije ne smije slučajno promijeniti
         * sadržaj koji ulazi u potpis.
         */
        order.getOrderItems()
                .stream()
                .sorted(
                        Comparator.comparing(
                                OrderItem::getId))
                .forEach(item -> {

                    BigDecimal subtotal =
                            item.calculateTotalPrice();

                    builder.append("ITEM=")
                            .append(
                                    item.getProduct().getId())
                            .append("|")
                            .append(
                                    item.getProduct().getName())
                            .append("|")
                            .append(
                                    item.getProduct().getBrand())
                            .append("|")
                            .append(
                                    item.getQuantity())
                            .append("|")
                            .append(
                                    normalizeMoney(
                                            item.getPrice_at_order_time()))
                            .append("|")
                            .append(
                                    normalizeMoney(subtotal))
                            .append("\n");
                });

        builder.append("TOTAL=")
                .append(
                        normalizeMoney(
                                order.calculateTotal()))
                .append("\n");

        return builder.toString();
    }

    /*
     * NORMALIZACIJA DATUMA.
     *
     * Nanosekunde se uklanjaju kako baza i Java reprezentacija
     * ne bi proizvodile različit tekst.
     */
    private String normalizeDate(
            LocalDateTime dateTime) {

        if (dateTime == null) {
            return "-";
        }

        return dateTime
                .withNano(0)
                .format(
                        DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    /*
     * NORMALIZACIJA NOVCA.
     *
     * 1600
     * 1600.0
     * 1600.00
     *
     * moraju postati isti kanonski string:
     *
     * 1600.00
     */
    private String normalizeMoney(
            BigDecimal value) {

        if (value == null) {
            return "0.00";
        }

        return value
                .setScale(
                        2,
                        RoundingMode.HALF_UP)
                .toPlainString();
    }

    /*
     * FORENZIČKA USPOREDBA ORIGINALA I TRENUTNOG STANJA.
     */
    private List<String> compareContents(
            String original,
            String current) {

        List<String> differences =
                new ArrayList<>();

        /*
         * Razbijamo oba sadržaja po redovima.
         */
        String[] originalLines =
                original.split("\\R");

        String[] currentLines =
                current.split("\\R");

        /*
         * Moramo proći duljinu dužeg sadržaja.
         */
        int max =
                Math.max(
                        originalLines.length,
                        currentLines.length);

        for (int i = 0; i < max; i++) {

            String oldLine =
                    i < originalLines.length
                            ? originalLines[i]
                            : "";

            String newLine =
                    i < currentLines.length
                            ? currentLines[i]
                            : "";

            if (!oldLine.equals(newLine)) {

                differences.add(
                        "Original: "
                                + oldLine
                                + " | Current: "
                                + newLine
                );
            }
        }

        return differences;
    }

    /*
     * Helper za .sig file.
     */
    private File getSignatureFile(Long orderId) {

        return new File(
                SIGNATURE_DIR
                        + "/order-"
                        + orderId
                        + ".sig"
        );
    }

    /*
     * Helper za .original file.
     */
    private File getOriginalFile(Long orderId) {

        return new File(
                SIGNATURE_DIR
                        + "/order-"
                        + orderId
                        + ".original"
        );
    }

    private File getReportFile() {
        return new File(REPORT_FILE);
    }

    /*
     * Osigurava postojanje direktorija.
     */
    private void createDirectoryIfNeeded() {

        File dir =
                new File(SIGNATURE_DIR);

        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    /*
     * RSA KEY MANAGEMENT.
     *
     * Učitava postojeći key pair ili generira novi.
     */
    private KeyPair getOrCreateKeyPair()
            throws Exception {

        File privateKeyFile =
                new File(PRIVATE_KEY_FILE);

        File publicKeyFile =
                new File(PUBLIC_KEY_FILE);

        /*
         * Ako oba postoje, rekonstruiramo RSA objekte.
         */
        if (privateKeyFile.exists()
                && publicKeyFile.exists()) {

            byte[] privateKeyBytes =
                    Base64.getDecoder()
                            .decode(
                                    Files.readString(
                                                    privateKeyFile.toPath())
                                            .trim()
                            );

            byte[] publicKeyBytes =
                    Base64.getDecoder()
                            .decode(
                                    Files.readString(
                                                    publicKeyFile.toPath())
                                            .trim()
                            );

            KeyFactory keyFactory =
                    KeyFactory.getInstance(
                            KEY_ALGORITHM);

            /*
             * Private key -> PKCS#8.
             */
            PrivateKey privateKey =
                    keyFactory.generatePrivate(
                            new java.security.spec.PKCS8EncodedKeySpec(
                                    privateKeyBytes)
                    );

            /*
             * Public key -> X.509.
             */
            PublicKey publicKey =
                    keyFactory.generatePublic(
                            new java.security.spec.X509EncodedKeySpec(
                                    publicKeyBytes)
                    );

            return new KeyPair(
                    publicKey,
                    privateKey
            );
        }

        /*
         * Ako ključevi ne postoje, generiramo RSA-2048.
         */
        KeyPairGenerator generator =
                KeyPairGenerator.getInstance(
                        KEY_ALGORITHM);

        generator.initialize(2048);

        KeyPair keyPair =
                generator.generateKeyPair();

        /*
         * Base64 služi samo za tekstualno spremanje ključeva.
         */
        Files.writeString(
                privateKeyFile.toPath(),
                Base64.getEncoder()
                        .encodeToString(
                                keyPair.getPrivate()
                                        .getEncoded())
        );

        Files.writeString(
                publicKeyFile.toPath(),
                Base64.getEncoder()
                        .encodeToString(
                                keyPair.getPublic()
                                        .getEncoded())
        );

        return keyPair;
    }
}