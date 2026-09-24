package hr.tvz.ntp.smartordersystem.controller;

import hr.tvz.ntp.smartordersystem.dto.ExchangeRateDto;
import hr.tvz.ntp.smartordersystem.service.ExternalRestService;
import hr.tvz.ntp.smartordersystem.service.LogHelperService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 * CONTROLLER ZA VANJSKI REST SERVIS.
 *
 * Frontend ne komunicira direktno s Frankfurter API-jem.
 *
 * Frontend
 *    ↓
 * /external/exchange-rate/eur-usd
 *    ↓
 * ExternalRestController
 *    ↓
 * ExternalRestService
 *    ↓
 * RestClient
 *    ↓
 * Frankfurter API
 */
@RestController
@RequestMapping("/external")
public class ExternalRestController {

    private final ExternalRestService externalRestService;
    private final LogHelperService logHelperService;

    public ExternalRestController(
            ExternalRestService externalRestService,
            LogHelperService logHelperService) {

        this.externalRestService =
                externalRestService;

        this.logHelperService =
                logHelperService;
    }

    /*
     * GET /external/exchange-rate/eur-usd
     */
    @GetMapping("/exchange-rate/eur-usd")
    public ResponseEntity<ExchangeRateDto>
    getEurToUsdRate(
            Authentication authentication,
            HttpServletRequest request) {

        /*
         * Service radi stvarni HTTP GET prema udaljenom API-ju.
         */
        ExchangeRateDto rate =
                externalRestService
                        .getEurToUsdRate();

        /*
         * Bilježimo korištenje vanjskog servisa.
         */
        logHelperService.log(
                authentication,
                request,
                "EXTERNAL_REST_EXCHANGE_RATE",
                "Fetched EUR to USD exchange rate from external REST API"
        );

        return ResponseEntity.ok(rate);
    }
}