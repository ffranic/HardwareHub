package hr.tvz.ntp.smartordersystem.service;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/*
 * SERVIS ZA PRE-HASHIRANJE LOZINKE PRIJE BCRYPTA.
 *
 * Konačni proces izgleda ovako:
 *
 * raw password
 *      +
 * variable salt
 *      +
 * pepper
 *      ↓
 * SHA-256
 *      ↓
 * pre-hashed password
 *      ↓
 * BCrypt
 *      ↓
 * vrijednost spremljena u bazi
 *
 * OBRANA:
 * SHA-256 ovdje nije konačni password hash koji se sprema u bazu.
 * On služi kao dodatni pre-hash sloj prije BCrypt algoritma.
 */
@Service
@Transactional
public class PasswordPreHashServiceImpl implements PasswordPreHashService {

    /*
     * Hash algoritam korišten za pre-hash i generiranje salta.
     */
    private static final String SHA_256 = "SHA-256";

    /*
     * Spring Security PasswordEncoder.
     *
     * U SecurityConfig je bean implementiran kao BCryptPasswordEncoder.
     *
     * Koristi se u findMatchingPepper() kako bismo provjerili odgovara li
     * neki kandidat za pepper spremljenom BCrypt hashu.
     */
    private final PasswordEncoder passwordEncoder;

    /*
     * Pepper se učitava iz konfiguracije:
     *
     * security.password.pepper
     *
     * Ako property nije definiran, koristi se default vrijednost "428".
     *
     * OBRANA:
     * Pepper je zajednička tajna vrijednost i za razliku od salta
     * ne sprema se uz svaki password u bazu.
     */
    @Value("${security.password.pepper:428}")
    private String configuredPepper;

    /*
     * Constructor injection.
     */
    public PasswordPreHashServiceImpl(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    /*
     * Standardni pre-hash koji koristi pepper iz konfiguracije.
     */
    @Override
    public String preHashPassword(String username, String rawPassword) {

        return preHashPasswordWithPepper(
                username,
                rawPassword,
                configuredPepper
        );
    }

    /*
     * Pre-hash s eksplicitno zadanim pepperom.
     *
     * Ova metoda je posebno korisna u findMatchingPepper(),
     * gdje se isprobavaju pepper vrijednosti 000-999.
     */
    @Override
    public String preHashPasswordWithPepper(
            String username,
            String rawPassword,
            String pepper) {

        /*
         * Prije bilo kakvog hashiranja provjeravamo ulaz.
         */
        validate(username, rawPassword);

        /*
         * Projekt definira pepper kao točno tri znamenke.
         *
         * Regex:
         * \d{3}
         *
         * prihvaća:
         * 000
         * 428
         * 999
         *
         * ne prihvaća:
         * 42
         * 1000
         * abc
         */
        if (pepper == null || !pepper.matches("\\d{3}")) {
            throw new IllegalArgumentException(
                    "Pepper must be a three-digit value from 000 to 999."
            );
        }

        /*
         * Salt se deterministički generira iz usernamea.
         *
         * Dakle različiti korisnici s istom lozinkom neće imati
         * isti pre-hash ulaz.
         */
        String salt = generateVariableSalt(username);

        /*
         * Formiramo sadržaj koji ulazi u SHA-256:
         *
         * password:salt:pepper
         *
         * Separator ":" sprječava nejasno spajanje vrijednosti.
         */
        String combined =
                rawPassword
                        + ":"
                        + salt
                        + ":"
                        + pepper;

        /*
         * Vraća 256-bitni SHA-256 hash u hexadecimalnom obliku.
         *
         * Nakon toga ostatak autentikacijskog sustava taj rezultat
         * dodatno BCrypt-hashira.
         */
        return sha256(combined);
    }

    /*
     * TRAŽENJE TOČNOG PEPPERA.
     *
     * OBRANA:
     * Demonstrira brute-force pronalaženje male, poznato ograničene
     * pepper domene 000-999.
     *
     * Ovo nije preporučeni produkcijski dizajn za velik secret,
     * nego namjerno ograničena demonstracija zahtjeva.
     */
    @Override
    public String findMatchingPepper(
            String username,
            String rawPassword,
            String storedBcryptHash) {

        validate(username, rawPassword);

        if (storedBcryptHash == null || storedBcryptHash.isBlank()) {
            throw new IllegalArgumentException(
                    "Stored hash must not be empty."
            );
        }

        /*
         * Isprobavamo sve moguće troznamenkaste vrijednosti:
         *
         * 000
         * 001
         * ...
         * 999
         */
        for (int i = 0; i <= 999; i++) {

            /*
             * %03d -> broj se prikazuje uvijek s tri znamenke.
             *
             * 5 -> "005"
             */
            String pepper = String.format("%03d", i);

            /*
             * Za svaki kandidat rekonstruiramo pre-hash.
             */
            String preHashedPassword =
                    preHashPasswordWithPepper(
                            username,
                            rawPassword,
                            pepper
                    );

            /*
             * BCrypt nije determinističan zbog vlastitog salta,
             * pa ne možemo raditi equals().
             *
             * passwordEncoder.matches() zna iz stored BCrypt hasha
             * izvući njegove parametre/salt i napraviti pravilnu provjeru.
             */
            if (passwordEncoder.matches(
                    preHashedPassword,
                    storedBcryptHash)) {

                /*
                 * Pronađen pepper koji daje ispravan password.
                 */
                return pepper;
            }
        }

        /*
         * Nijedna od 1000 vrijednosti nije odgovarala.
         */
        return null;
    }

    /*
     * GENERIRANJE VARIJABILNOG SALTA.
     *
     * Salt je vezan uz username.
     */
    private String generateVariableSalt(String username) {

        /*
         * Normaliziramo username:
         *
         * " Filip " -> "filip"
         *
         * tako ista logička vrijednost uvijek daje isti salt.
         */
        String normalizedUsername =
                username.toLowerCase().trim();

        /*
         * Dodajemo application-specific prefix kako salt ne bi bio
         * samo obični SHA-256 usernamea.
         */
        return sha256(
                "TechStockPasswordSalt:"
                        + normalizedUsername
        );
    }

    /*
     * GENERIČKI SHA-256 HELPER.
     */
    private String sha256(String input) {

        try {

            /*
             * MessageDigest je Java API za hash funkcije.
             */
            MessageDigest digest =
                    MessageDigest.getInstance(SHA_256);

            /*
             * String -> UTF-8 byteovi -> SHA-256.
             */
            byte[] hashBytes =
                    digest.digest(
                            input.getBytes(
                                    StandardCharsets.UTF_8)
                    );

            /*
             * Binary byte[] pretvaramo u hexadecimalni String.
             */
            return HexFormat
                    .of()
                    .formatHex(hashBytes);

        } catch (Exception e) {

            throw new RuntimeException(
                    "Could not calculate SHA-256 hash.",
                    e
            );
        }
    }

    /*
     * CENTRALNA VALIDACIJA.
     *
     * Izbjegava dupliciranje iste provjere kroz više metoda.
     */
    private void validate(
            String username,
            String rawPassword) {

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException(
                    "Username must not be empty."
            );
        }

        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException(
                    "Password must not be empty."
            );
        }
    }
}