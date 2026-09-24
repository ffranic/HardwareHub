package hr.tvz.ntp.smartordersystem.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/*
 * CENTRALNI HELPER ZA AUDIT LOGGING.
 *
 * Cilj ove klase je da kontroleri ne moraju svaki put ručno:
 *
 * - vaditi username
 * - vaditi role
 * - vaditi IP adresu
 * - zvati ActivityLogService sa svim parametrima
 *
 * Umjesto toga controller samo kaže:
 *
 * logHelperService.log(
 *     authentication,
 *     request,
 *     "DELETE_PRODUCT",
 *     "Deleted product id 5"
 * );
 */
@Service
@Transactional
public class LogHelperServiceImpl
        implements LogHelperService {

    /*
     * ActivityLogService je zapravo implementiran
     * kroz JsonActivityLogServiceImpl.
     */
    private final ActivityLogService activityLogService;

    public LogHelperServiceImpl(
            ActivityLogService activityLogService) {

        this.activityLogService =
                activityLogService;
    }

    /*
     * Centralna metoda za generiranje audit zapisa.
     */
    @Override
    public void log(
            Authentication authentication,
            HttpServletRequest request,
            String action,
            String details) {

        /*
         * Default vrijednosti ako nemamo autentificiranog korisnika.
         */
        String username =
                "anonymous";

        String role =
                "NONE";

        /*
         * Spring Security Authentication objekt sadrži podatke
         * o trenutnom prijavljenom korisniku.
         */
        if (authentication != null
                && authentication.isAuthenticated()) {

            /*
             * getName() obično vraća username.
             */
            username =
                    authentication.getName();

            /*
             * Authorities sadrže role/permissions.
             *
             * Kod nas korisnik ima jednu glavnu role vrijednost,
             * pa uzimamo prvu.
             *
             * Primjer:
             * ROLE_ADMIN
             * ROLE_CUSTOMER
             */
            if (!authentication
                    .getAuthorities()
                    .isEmpty()) {

                role =
                        authentication
                                .getAuthorities()
                                .iterator()
                                .next()
                                .getAuthority();
            }
        }

        /*
         * IP adresa HTTP klijenta.
         *
         * U lokalnom developmentu često će biti:
         *
         * 127.0.0.1
         * ili
         * 0:0:0:0:0:0:0:1
         */
        String ipAddress =
                request != null
                        ? request.getRemoteAddr()
                        : "unknown";

        /*
         * Helper ne brine kako se log fizički sprema.
         *
         * To delegira ActivityLogServiceu.
         *
         * Aktualni JsonActivityLogServiceImpl:
         *
         * napravi DTO
         * -> JSON
         * -> AES-GCM
         * -> .enc file
         */
        activityLogService.log(
                username,
                role,
                action,
                details,
                ipAddress
        );
    }
}