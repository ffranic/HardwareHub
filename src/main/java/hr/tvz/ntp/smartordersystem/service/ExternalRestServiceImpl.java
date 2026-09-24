package hr.tvz.ntp.smartordersystem.service;

import hr.tvz.ntp.smartordersystem.dto.ExchangeRateDto;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/*
 * SERVIS ZA KOMUNIKACIJU S VANJSKIM REST API-JEM.
 *
 * OBRANA:
 * Ovo pokriva zahtjev u kojem aplikacija mora koristiti udaljeni
 * REST servis.
 *
 * Koristi se službeni Spring RestClient.
 */
@Service
@Transactional
public class ExternalRestServiceImpl implements ExternalRestService {

    /*
     * RestClient je Springov HTTP klijent.
     */
    private final RestClient restClient;

    /*
     * RestClient.Builder dobivamo kroz dependency injection.
     *
     * Builder je definiran kao @Bean u SecurityConfig.
     *
     * Tamo su također definirani connection/read timeouti.
     */
    public ExternalRestServiceImpl(
            RestClient.Builder restClientBuilder) {

        /*
         * baseUrl znači da kasnije ne moramo svaki put pisati
         * cijeli URL.
         */
        this.restClient =
                restClientBuilder
                        .baseUrl(
                                "https://api.frankfurter.dev/v2")
                        .build();
    }

    /*
     * Dohvaća trenutni EUR -> USD tečaj.
     */
    @Override
    public ExchangeRateDto getEurToUsdRate() {

        /*
         * Izvršava:
         *
         * GET
         * https://api.frankfurter.dev/v2/rates?base=EUR&quotes=USD
         */
        List<Map<String, Object>> response =
                restClient
                        .get()
                        .uri(
                                "/rates?base=EUR&quotes=USD")
                        .retrieve()

                        /*
                         * HTTP response body se deserijalizira
                         * u Java List.
                         *
                         * Zbog generičkog runtime type erasure ovdje
                         * se koristi sirovi List.class, pa elemente
                         * tretiramo kao Map.
                         */
                        .body(List.class);

        /*
         * Defensive check:
         * API možda vrati null/prazan response.
         */
        if (response == null
                || response.isEmpty()) {

            throw new RuntimeException(
                    "Invalid response from external exchange rate API."
            );
        }

        /*
         * API vraća listu rate objekata.
         *
         * Za EUR/USD očekujemo prvi zapis.
         */
        Map<String, Object> firstRate =
                response.get(0);

        Object rateObject =
                firstRate.get("rate");

        Object dateObject =
                firstRate.get("date");

        if (rateObject == null) {
            throw new RuntimeException(
                    "USD rate not found in external API response."
            );
        }

        /*
         * Novčane/decimalne vrijednosti pretvaramo u BigDecimal,
         * a ne double.
         */
        BigDecimal rate =
                new BigDecimal(
                        rateObject.toString());

        String date =
                dateObject != null
                        ? dateObject.toString()
                        : "-";

        /*
         * Vanjski API response pretvaramo u vlastiti DTO.
         *
         * Time ostatak aplikacije nije vezan za strukturu
         * Frankfurter API-ja.
         */
        return new ExchangeRateDto(
                "EUR",
                "USD",
                rate,
                date
        );
    }
}