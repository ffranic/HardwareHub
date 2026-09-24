package hr.tvz.ntp.smartordersystem.service;

import hr.tvz.ntp.smartordersystem.dto.ActivityLogDto;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/*
 * AKTUALNI SERVIS ZA SIGURNOSNE AUDIT LOGOVE.
 *
 * Ovo je jedna od ključnih sigurnosnih klasa projekta.
 *
 * Funkcionalnosti:
 *
 * - zapisivanje audit događaja
 * - AES-256-GCM enkripcija logova
 * - automatska dekripcija pri čitanju
 * - automatska dnevna arhivacija
 * - SHA-256 provjera integriteta encrypted datoteka
 *
 * OBRANA:
 * Na disku se audit logovi ne čuvaju kao plaintext JSON.
 *
 * JSON postoji samo privremeno u memoriji prije enkripcije
 * odnosno nakon dekripcije.
 */
@Service
@Transactional
public class JsonActivityLogServiceImpl
        implements ActivityLogService {

    /*
     * Glavni direktorij.
     */
    private static final Path LOG_DIR =
            Paths.get("logs");

    /*
     * Dnevne arhive:
     *
     * logs/archive/activity-YYYY-MM-DD.enc
     */
    private static final Path ARCHIVE_DIR =
            LOG_DIR.resolve("archive");

    /*
     * Aktivni encrypted log.
     */
    private static final Path CURRENT_LOG_FILE =
            LOG_DIR.resolve(
                    "activity-current.enc");

    /*
     * SHA-256 hash encrypted loga.
     */
    private static final Path CURRENT_HASH_FILE =
            LOG_DIR.resolve(
                    "activity-current.sha256");

    private static final String AES_ALGORITHM =
            "AES";

    /*
     * GCM daje:
     *
     * - confidentiality
     * - integrity/authentication
     */
    private static final String AES_TRANSFORMATION =
            "AES/GCM/NoPadding";

    /*
     * 12-byte GCM nonce/IV.
     */
    private static final int GCM_IV_LENGTH = 12;

    /*
     * 128-bit authentication tag.
     */
    private static final int GCM_TAG_LENGTH = 128;

    /*
     * ObjectMapper:
     *
     * List<ActivityLogDto> <-> JSON.
     */
    private final ObjectMapper objectMapper;

    /*
     * SecureRandom za kriptografski sigurne IV vrijednosti.
     */
    private final SecureRandom secureRandom =
            new SecureRandom();

    /*
     * AES passphrase/key material dolazi iz application propertiesa.
     *
     * Ako property ne postoji, koristi se default.
     *
     * OBRANA:
     * U produkciji bi secret trebao doći iz environment variablea,
     * vaulta/secret managera, a ne iz defaultne vrijednosti.
     */
    @Value("${security.logs.encryption-key:HardwareHubDefaultLogEncryptionKeyChangeMe}")
    private String encryptionKey;

    /*
     * Spring injektira konfigurirani ObjectMapper.
     */
    public JsonActivityLogServiceImpl(
            ObjectMapper objectMapper) {

        this.objectMapper = objectMapper;
    }

    /*
     * findAll je alias za aktualne logove.
     */
    @Override
    public synchronized List<ActivityLogDto> findAll() {
        return findCurrent();
    }

    /*
     * synchronized:
     *
     * više request dretvi ne smije istovremeno čitati/mijenjati
     * istu log datoteku dok se radi rotacija/arhiviranje.
     *
     * Ovo NIJE primjer koji koristimo za profesorov lock zahtjev,
     * ali je dodatna thread-safety zaštita.
     */
    @Override
    public synchronized List<ActivityLogDto> findCurrent() {

        /*
         * Prije čitanja provjeravamo ima li starih logova
         * koji trebaju u dnevnu arhivu.
         */
        archiveOldLogsIfNeeded();

        /*
         * Datoteka se automatski dešifrira u memoriji.
         */
        return readEncryptedLogs(
                CURRENT_LOG_FILE);
    }

    /*
     * Čitanje svih dnevnih arhiva.
     */
    @Override
    public synchronized
    Map<String, List<ActivityLogDto>> findArchived() {

        ensureDirectories();

        /*
         * TreeMap reverseOrder ->
         * najnoviji datum prvi.
         */
        Map<String, List<ActivityLogDto>> archivedLogs =
                new TreeMap<>(
                        Comparator.reverseOrder());

        try {

            if (!Files.exists(ARCHIVE_DIR)) {
                return archivedLogs;
            }

            /*
             * DirectoryStream dohvaća samo:
             *
             * activity-*.enc
             */
            try (DirectoryStream<Path> stream =
                         Files.newDirectoryStream(
                                 ARCHIVE_DIR,
                                 "activity-*.enc")) {

                for (Path archiveFile : stream) {

                    String date =
                            extractDateFromArchiveFile(
                                    archiveFile);

                    /*
                     * readEncryptedLogs automatski
                     * radi AES dekripciju.
                     */
                    archivedLogs.put(
                            date,
                            readEncryptedLogs(
                                    archiveFile)
                    );
                }
            }

            return archivedLogs;

        } catch (IOException e) {

            throw new RuntimeException(
                    "Could not read archived activity logs.",
                    e
            );
        }
    }

    /*
     * Provjera integriteta encrypted fileova pomoću SHA-256.
     */
    @Override
    public synchronized
    Map<String, Object> verifyIntegrity() {

        ensureCurrentFileExists();

        Map<String, Object> result =
                new LinkedHashMap<>();

        /*
         * Uspoređujemo aktualni SHA-256 encrypted filea
         * s ranije spremljenim hashom.
         */
        result.put(
                "currentLogValid",
                verifyFileHash(
                        CURRENT_LOG_FILE,
                        CURRENT_HASH_FILE)
        );

        Map<String, Boolean> archives =
                new TreeMap<>(
                        Comparator.reverseOrder());

        try {

            if (Files.exists(ARCHIVE_DIR)) {

                try (DirectoryStream<Path> stream =
                             Files.newDirectoryStream(
                                     ARCHIVE_DIR,
                                     "activity-*.enc")) {

                    for (Path archiveFile : stream) {

                        Path hashFile =
                                getArchiveHashFile(
                                        archiveFile);

                        archives.put(
                                archiveFile
                                        .getFileName()
                                        .toString(),
                                verifyFileHash(
                                        archiveFile,
                                        hashFile)
                        );
                    }
                }
            }

            result.put(
                    "archives",
                    archives);

            return result;

        } catch (IOException e) {

            throw new RuntimeException(
                    "Could not verify activity log integrity.",
                    e
            );
        }
    }

    /*
     * Traženje jednog loga.
     *
     * Pretražuju se i current i archived logovi.
     */
    @Override
    public synchronized
    ActivityLogDto findById(String id) {

        return getAllReadableLogs()
                .stream()
                .filter(log ->
                        log.getId().equals(id))
                .findFirst()
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Activity log not found with id: "
                                        + id));
    }

    /*
     * CREATE AUDIT LOGA.
     */
    @Override
    public synchronized
    ActivityLogDto create(ActivityLogDto log) {

        /*
         * Osigurava da postoji inicijalna encrypted datoteka.
         */
        ensureCurrentFileExists();

        /*
         * Trenutni .enc file:
         *
         * disk -> AES decrypt -> JSON -> List<ActivityLogDto>
         */
        List<ActivityLogDto> logs =
                readEncryptedLogs(
                        CURRENT_LOG_FILE);

        /*
         * Ako ID nije poslan, automatski ga generiramo.
         */
        if (log.getId() == null
                || log.getId().isBlank()) {

            log.setId(generateId());
        }

        /*
         * Isto za timestamp.
         */
        if (log.getTimestamp() == null
                || log.getTimestamp().isBlank()) {

            log.setTimestamp(
                    LocalDateTime.now()
                            .toString());
        }

        logs.add(log);

        /*
         * Prije ponovnog zapisivanja izdvajamo logove
         * prethodnih dana u dnevne arhive.
         */
        List<ActivityLogDto> currentLogs =
                archiveOldLogs(logs);

        /*
         * Lista -> JSON -> AES encryption -> .enc
         *
         * i paralelno se zapisuje SHA-256 hash ciphertexta.
         */
        writeEncryptedLogs(
                CURRENT_LOG_FILE,
                CURRENT_HASH_FILE,
                currentLogs
        );

        return log;
    }

    /*
     * Convenience metoda koju koristi LogHelperService.
     *
     * Ostatak aplikacije ne mora ručno kreirati DTO.
     */
    @Override
    public void log(
            String username,
            String role,
            String action,
            String details,
            String ipAddress) {

        ActivityLogDto log =
                new ActivityLogDto(
                        generateId(),
                        LocalDateTime.now()
                                .toString(),
                        username,
                        role,
                        action,
                        details,
                        ipAddress
                );

        create(log);
    }

    /*
     * Provjerava treba li postojeće logove rotirati
     * u dnevne arhive.
     */
    private void archiveOldLogsIfNeeded() {

        ensureCurrentFileExists();

        List<ActivityLogDto> logs =
                readEncryptedLogs(
                        CURRENT_LOG_FILE);

        List<ActivityLogDto> currentLogs =
                archiveOldLogs(logs);

        /*
         * Ako se broj promijenio, znači da smo dio
         * logova premjestili u arhivu.
         */
        if (currentLogs.size()
                != logs.size()) {

            writeEncryptedLogs(
                    CURRENT_LOG_FILE,
                    CURRENT_HASH_FILE,
                    currentLogs
            );
        }
    }

    /*
     * Razdvaja današnje logove od starijih.
     */
    private List<ActivityLogDto> archiveOldLogs(
            List<ActivityLogDto> logs) {

        LocalDate today =
                LocalDate.now();

        /*
         * Grupiranje logova prema datumu.
         */
        Map<LocalDate, List<ActivityLogDto>> groupedByDate =
                logs.stream()
                        .collect(
                                Collectors.groupingBy(
                                        this::extractLogDate));

        List<ActivityLogDto> currentLogs =
                new ArrayList<>();

        for (Map.Entry<
                LocalDate,
                List<ActivityLogDto>>
                entry
                : groupedByDate.entrySet()) {

            LocalDate logDate =
                    entry.getKey();

            List<ActivityLogDto> dailyLogs =
                    entry.getValue();

            /*
             * Ako je log stariji od današnjeg datuma,
             * ide u dnevnu arhivu.
             */
            if (logDate.isBefore(today)) {

                appendToDailyArchive(
                        logDate,
                        dailyLogs);

            } else {

                currentLogs.addAll(
                        dailyLogs);
            }
        }

        /*
         * Zadržavamo kronološki poredak.
         */
        currentLogs.sort(
                Comparator.comparing(
                        ActivityLogDto::getTimestamp));

        return currentLogs;
    }

    /*
     * Dodaje logove u odgovarajući dnevni encrypted archive.
     */
    private void appendToDailyArchive(
            LocalDate date,
            List<ActivityLogDto> logsToArchive) {

        if (logsToArchive.isEmpty()) {
            return;
        }

        Path archiveFile =
                ARCHIVE_DIR.resolve(
                        "activity-"
                                + date
                                + ".enc");

        Path hashFile =
                ARCHIVE_DIR.resolve(
                        "activity-"
                                + date
                                + ".sha256");

        /*
         * Ako arhiva već postoji, prvo je dešifriramo i čitamo.
         */
        List<ActivityLogDto> existingArchiveLogs =
                readEncryptedLogs(
                        archiveFile);

        /*
         * Set postojećih ID-eva sprečava duplikate.
         */
        Set<String> existingIds =
                existingArchiveLogs
                        .stream()
                        .map(
                                ActivityLogDto::getId)
                        .collect(
                                Collectors.toSet());

        for (ActivityLogDto log :
                logsToArchive) {

            if (!existingIds.contains(
                    log.getId())) {

                existingArchiveLogs.add(log);
            }
        }

        existingArchiveLogs.sort(
                Comparator.comparing(
                        ActivityLogDto::getTimestamp));

        /*
         * Ponovno AES-enkriptira cijelu dnevnu arhivu
         * i ažurira hash.
         */
        writeEncryptedLogs(
                archiveFile,
                hashFile,
                existingArchiveLogs
        );
    }

    /*
     * Kombinira current + archive za pretraživanje.
     */
    private List<ActivityLogDto> getAllReadableLogs() {

        List<ActivityLogDto> logs =
                new ArrayList<>();

        logs.addAll(
                readEncryptedLogs(
                        CURRENT_LOG_FILE));

        Map<String, List<ActivityLogDto>> archived =
                findArchived();

        archived.values()
                .forEach(logs::addAll);

        return logs;
    }

    /*
     * CENTRALNA DEKRIPCIJSKA METODA ZA LOGOVE.
     */
    private List<ActivityLogDto> readEncryptedLogs(Path file) {

        try {
            ensureDirectories();

            /*
             * Ne postoji ili je prazan ->
             * nema logova.
             */
            if (!Files.exists(file)
                    || Files.size(file) == 0) {

                return new ArrayList<>();
            }

            /*
             * Učitavamo binarni ciphertext.
             */
            byte[] encryptedBytes =
                    Files.readAllBytes(file);

            /*
             * AES-GCM decrypt.
             *
             * Tek u memoriji dobivamo JSON.
             */
            String json =
                    decrypt(encryptedBytes);

            if (json == null
                    || json.isBlank()) {

                return new ArrayList<>();
            }

            /*
             * JSON array -> List<ActivityLogDto>.
             *
             * TypeReference je potreban jer generički tip List<ActivityLogDto>
             * nije dostupan kroz obični List.class zbog type erasurea.
             */
            return objectMapper.readValue(
                    json,
                    new TypeReference<
                            List<ActivityLogDto>>() {}
            );

        } catch (NoSuchFileException e) {

            return new ArrayList<>();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Could not read encrypted activity logs from file: "
                            + file,
                    e
            );
        }
    }

    /*
     * CENTRALNA ENKRIPCIJSKA METODA ZA LOGOVE.
     */
    private void writeEncryptedLogs(
            Path file,
            Path hashFile,
            List<ActivityLogDto> logs) {

        try {
            ensureDirectories();

            /*
             * Java lista -> pretty JSON.
             *
             * JSON postoji samo privremeno u memoriji.
             */
            String json =
                    objectMapper
                            .writerWithDefaultPrettyPrinter()
                            .writeValueAsString(logs);

            /*
             * JSON -> AES-GCM ciphertext.
             */
            byte[] encryptedBytes =
                    encrypt(json);

            /*
             * Na disk ide samo encrypted byte[].
             *
             * TRUNCATE_EXISTING prepisuje prethodno stanje.
             */
            Files.write(
                    file,
                    encryptedBytes,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );

            /*
             * Hash radimo NAD ENKRIPTIRANOM DATOTEKOM.
             *
             * Time kasnije možemo provjeriti je li ciphertext
             * promijenjen nakon zapisivanja.
             */
            Files.writeString(
                    hashFile,
                    sha256Hex(encryptedBytes),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Could not write encrypted activity logs to file: "
                            + file,
                    e
            );
        }
    }

    /*
     * Inicijalizira current .enc i .sha256 datoteke.
     */
    private void ensureCurrentFileExists() {

        ensureDirectories();

        try {

            /*
             * Ako current file ne postoji,
             * kreiramo validnu AES-enkriptiranu praznu JSON listu [].
             */
            if (!Files.exists(
                    CURRENT_LOG_FILE)) {

                writeEncryptedLogs(
                        CURRENT_LOG_FILE,
                        CURRENT_HASH_FILE,
                        new ArrayList<>()
                );
            }

            /*
             * Ako encrypted file postoji, ali hash ne,
             * ponovno generiramo njegov SHA-256.
             */
            if (!Files.exists(
                    CURRENT_HASH_FILE)) {

                Files.writeString(
                        CURRENT_HASH_FILE,
                        sha256Hex(
                                Files.readAllBytes(
                                        CURRENT_LOG_FILE)),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING
                );
            }

        } catch (IOException e) {

            throw new RuntimeException(
                    "Could not initialize encrypted activity log file.",
                    e
            );
        }
    }

    /*
     * Kreira potrebne foldere.
     *
     * createDirectories je idempotent:
     * ako već postoje, neće biti problem.
     */
    private void ensureDirectories() {

        try {
            Files.createDirectories(
                    LOG_DIR);

            Files.createDirectories(
                    ARCHIVE_DIR);

        } catch (IOException e) {

            throw new RuntimeException(
                    "Could not create log directories.",
                    e
            );
        }
    }

    /*
     * SHA-256 integrity verification.
     */
    private boolean verifyFileHash(
            Path file,
            Path hashFile) {

        try {

            if (!Files.exists(file)
                    || !Files.exists(hashFile)) {

                return false;
            }

            /*
             * Aktualni hash encrypted sadržaja.
             */
            String actualHash =
                    sha256Hex(
                            Files.readAllBytes(file));

            /*
             * Hash koji smo spremili kod zadnjeg legitimnog zapisa.
             */
            String expectedHash =
                    Files.readString(
                                    hashFile)
                            .trim();

            return actualHash
                    .equalsIgnoreCase(
                            expectedHash);

        } catch (IOException e) {

            throw new RuntimeException(
                    "Could not verify file hash for: "
                            + file,
                    e
            );
        }
    }

    /*
     * Iz activity-2026-07-01.enc dobiva
     * activity-2026-07-01.sha256.
     */
    private Path getArchiveHashFile(
            Path archiveFile) {

        String fileName =
                archiveFile
                        .getFileName()
                        .toString();

        String hashName =
                fileName.replace(
                        ".enc",
                        ".sha256");

        return archiveFile
                .getParent()
                .resolve(hashName);
    }

    /*
     * Iz:
     *
     * activity-2026-07-01.enc
     *
     * dobiva:
     *
     * 2026-07-01
     */
    private String extractDateFromArchiveFile(
            Path archiveFile) {

        String fileName =
                archiveFile
                        .getFileName()
                        .toString();

        return fileName
                .replace(
                        "activity-",
                        "")
                .replace(
                        ".enc",
                        "");
    }

    /*
     * Timestamp String -> LocalDate.
     */
    private LocalDate extractLogDate(
            ActivityLogDto log) {

        try {

            return LocalDateTime
                    .parse(
                            log.getTimestamp())
                    .toLocalDate();

        } catch (Exception e) {

            /*
             * Ako je timestamp neispravan,
             * tretiramo ga kao današnji da ga ne izgubimo.
             */
            return LocalDate.now();
        }
    }

    /*
     * STVARNA AES-256-GCM ENKRIPCIJA.
     */
    private byte[] encrypt(String plainText)
            throws Exception {

        /*
         * Za SVAKO šifriranje novi random IV.
         */
        byte[] iv =
                new byte[GCM_IV_LENGTH];

        secureRandom.nextBytes(iv);

        Cipher cipher =
                Cipher.getInstance(
                        AES_TRANSFORMATION);

        /*
         * getSecretKey() vraća 256-bitni AES ključ.
         */
        cipher.init(
                Cipher.ENCRYPT_MODE,
                getSecretKey(),
                new GCMParameterSpec(
                        GCM_TAG_LENGTH,
                        iv
                )
        );

        /*
         * Plain JSON -> ciphertext + GCM authentication tag.
         */
        byte[] cipherText =
                cipher.doFinal(
                        plainText.getBytes(
                                StandardCharsets.UTF_8));

        /*
         * Datotečni format je:
         *
         * [12 byte IV][ciphertext + GCM tag]
         *
         * IV nije tajan pa ga možemo staviti ispred ciphertexta.
         */
        byte[] result =
                new byte[
                        iv.length
                                + cipherText.length
                        ];

        System.arraycopy(
                iv,
                0,
                result,
                0,
                iv.length
        );

        System.arraycopy(
                cipherText,
                0,
                result,
                iv.length,
                cipherText.length
        );

        return result;
    }

    /*
     * AES-GCM DEKRIPCIJA.
     */
    private String decrypt(
            byte[] encryptedBytes)
            throws Exception {

        /*
         * Bez barem 12 byteova nema ni kompletnog IV-a.
         */
        if (encryptedBytes.length
                < GCM_IV_LENGTH) {

            return "";
        }

        /*
         * Prvih 12 byteova -> IV.
         */
        byte[] iv =
                Arrays.copyOfRange(
                        encryptedBytes,
                        0,
                        GCM_IV_LENGTH);

        /*
         * Ostatak -> ciphertext + authentication tag.
         */
        byte[] cipherText =
                Arrays.copyOfRange(
                        encryptedBytes,
                        GCM_IV_LENGTH,
                        encryptedBytes.length);

        Cipher cipher =
                Cipher.getInstance(
                        AES_TRANSFORMATION);

        cipher.init(
                Cipher.DECRYPT_MODE,
                getSecretKey(),
                new GCMParameterSpec(
                        GCM_TAG_LENGTH,
                        iv
                )
        );

        /*
         * Ako je ciphertext ili GCM tag modificiran,
         * doFinal() neće uspjeti.
         *
         * OBRANA:
         * GCM osigurava i povjerljivost i autentikaciju/integritet.
         */
        return new String(
                cipher.doFinal(cipherText),
                StandardCharsets.UTF_8
        );
    }

    /*
     * GENERIRANJE 256-BITNOG AES KLJUČA IZ KONFIGURACIJSKOG SECRETA.
     */
    private SecretKeySpec getSecretKey()
            throws Exception {

        /*
         * SHA-256 uvijek daje 32 bajta = 256 bitova.
         */
        byte[] keyBytes =
                MessageDigest
                        .getInstance("SHA-256")
                        .digest(
                                encryptionKey.getBytes(
                                        StandardCharsets.UTF_8)
                        );

        return new SecretKeySpec(
                keyBytes,
                AES_ALGORITHM
        );
    }

    /*
     * SHA-256 -> hexadecimal string.
     */
    private String sha256Hex(
            byte[] input) {

        try {

            byte[] hash =
                    MessageDigest
                            .getInstance("SHA-256")
                            .digest(input);

            StringBuilder builder =
                    new StringBuilder();

            for (byte b : hash) {

                builder.append(
                        String.format(
                                "%02x",
                                b));
            }

            return builder.toString();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Could not calculate SHA-256 hash.",
                    e
            );
        }
    }

    /*
     * Svaki audit zapis dobiva globalno praktički jedinstveni UUID.
     */
    private String generateId() {

        return "LOG-"
                + UUID.randomUUID();
    }
}