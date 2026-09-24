package hr.tvz.ntp.smartordersystem.controller;

import hr.tvz.ntp.smartordersystem.dto.SystemAnalysisResultDto;
import hr.tvz.ntp.smartordersystem.service.AnalysisService;
import hr.tvz.ntp.smartordersystem.service.LogHelperService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/*
 * REST CONTROLLER ZA SYSTEM ANALYSIS FUNKCIONALNOST.
 *
 * Uloga:
 * - prima HTTP zahtjev s frontenda
 * - delegira posao AnalysisServiceu
 * - zapisuje audit log
 * - vraća rezultat analize kao JSON
 *
 * OBRANA:
 * Controller ne radi samu analizu ni rad s dretvama.
 * To je odgovornost service sloja.
 */
@RestController
@RequestMapping("/analysis")
public class AnalysisController {

    private final AnalysisService analysisService;
    private final LogHelperService logHelperService;

    /*
     * Constructor injection.
     *
     * Spring pronalazi implementacije:
     * - AnalysisServiceImpl
     * - LogHelperServiceImpl
     */
    public AnalysisController(
            AnalysisService analysisService,
            LogHelperService logHelperService) {

        this.analysisService = analysisService;
        this.logHelperService = logHelperService;
    }

    /*
     * POST /analysis/system
     *
     * Pokreće analizu sustava.
     *
     * OBRANA:
     * Iza ovog endpointa AnalysisServiceImpl pokreće tri paralelna
     * Callable zadatka kroz ExecutorService i koristi ReentrantLock
     * pri zapisivanju u analysis-results.txt.
     */
    @PostMapping("/system")
    public ResponseEntity<SystemAnalysisResultDto> runSystemAnalysis(
            Authentication authentication,
            HttpServletRequest request) {

        /*
         * Controller delegira cijelu poslovnu logiku servisu.
         */
        SystemAnalysisResultDto result =
                analysisService.runSystemAnalysis();

        /*
         * Sigurnosno/administrativno relevantnu akciju zapisujemo
         * u audit log.
         *
         * LogHelperService automatski iz Authentication objekta
         * dohvaća username/role, a iz requesta IP adresu.
         */
        logHelperService.log(
                authentication,
                request,
                "RUN_SYSTEM_ANALYSIS",
                "Ran system analysis using Thread Pool and ReentrantLock"
        );

        /*
         * HTTP 200 + DTO kao JSON response.
         */
        return ResponseEntity.ok(result);
    }
}