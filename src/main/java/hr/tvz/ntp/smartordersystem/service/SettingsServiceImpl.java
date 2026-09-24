package hr.tvz.ntp.smartordersystem.service;

import hr.tvz.ntp.smartordersystem.dto.AppSettingsDTO;
import jakarta.transaction.Transactional;
import org.ini4j.Ini;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

/*
 * SERVIS ZA APLIKACIJSKE POSTAVKE U INI DATOTECI.
 *
 * File:
 *
 * app-settings.ini
 *
 * Primjer sadržaja:
 *
 * [Application]
 * language=en
 * theme=dark
 * itemsPerPage=8
 *
 * OBRANA:
 * Ini4j biblioteka služi za parsiranje i zapisivanje INI formata.
 */
@Service
@Transactional
public class SettingsServiceImpl implements SettingsService {

    /*
     * Naziv INI datoteke.
     */
    private static final String SETTINGS_FILE =
            "app-settings.ini";

    /*
     * INI section:
     *
     * [Application]
     */
    private static final String SECTION =
            "Application";

    /*
     * ČITANJE POSTAVKI.
     */
    @Override
    public AppSettingsDTO getSettings() {

        try {

            File file =
                    getOrCreateSettingsFile();

            /*
             * Ini4j učita cijelu datoteku.
             */
            Ini ini =
                    new Ini(file);

            /*
             * Dohvat vrijednosti iz [Application] sectiona.
             */
            String language =
                    ini.get(
                            SECTION,
                            "language",
                            String.class);

            String theme =
                    ini.get(
                            SECTION,
                            "theme",
                            String.class);

            Integer itemsPerPage =
                    ini.get(
                            SECTION,
                            "itemsPerPage",
                            Integer.class);

            /*
             * Defensive fallback ako neka vrijednost nedostaje.
             */
            return new AppSettingsDTO(
                    language != null
                            ? language
                            : "en",

                    theme != null
                            ? theme
                            : "dark",

                    itemsPerPage != null
                            ? itemsPerPage
                            : 8
            );

        } catch (IOException e) {

            throw new RuntimeException(
                    "Could not read settings file.",
                    e
            );
        }
    }

    /*
     * UPDATE POSTAVKI.
     */
    @Override
    public AppSettingsDTO updateSettings(
            AppSettingsDTO settings) {

        /*
         * Prije pisanja provjeravamo dopuštene vrijednosti.
         */
        validateSettings(settings);

        try {

            File file =
                    getOrCreateSettingsFile();

            Ini ini =
                    new Ini(file);

            /*
             * ini.put mijenja ili dodaje key unutar sectiona.
             */
            ini.put(
                    SECTION,
                    "language",
                    settings.getLanguage());

            ini.put(
                    SECTION,
                    "theme",
                    settings.getTheme());

            ini.put(
                    SECTION,
                    "itemsPerPage",
                    settings.getItemsPerPage());

            /*
             * Fizičko zapisivanje promjena na disk.
             */
            ini.store(file);

            return settings;

        } catch (IOException e) {

            throw new RuntimeException(
                    "Could not update settings file.",
                    e
            );
        }
    }

    /*
     * LAZY INICIJALIZACIJA FILEA.
     *
     * Ako app-settings.ini još ne postoji,
     * automatski se kreira s defaultnim vrijednostima.
     */
    private File getOrCreateSettingsFile()
            throws IOException {

        File file =
                new File(SETTINGS_FILE);

        if (!file.exists()) {

            Ini ini =
                    new Ini();

            ini.put(
                    SECTION,
                    "language",
                    "en");

            ini.put(
                    SECTION,
                    "theme",
                    "dark");

            ini.put(
                    SECTION,
                    "itemsPerPage",
                    8);

            ini.store(file);
        }

        return file;
    }

    /*
     * VALIDACIJA POSTAVKI.
     */
    private void validateSettings(
            AppSettingsDTO settings) {

        /*
         * Dopušteni jezici su samo:
         *
         * en
         * hr
         */
        if (!settings.getLanguage().equals("en")
                && !settings
                .getLanguage()
                .equals("hr")) {

            throw new IllegalArgumentException(
                    "Language must be 'en' or 'hr'."
            );
        }

        /*
         * Razumna granica broja proizvoda po stranici.
         */
        if (settings.getItemsPerPage() < 4
                || settings
                .getItemsPerPage() > 50) {

            throw new IllegalArgumentException(
                    "Items per page must be between 4 and 50."
            );
        }
    }
}