package hr.tvz.ntp.smartordersystem.controller;

import hr.tvz.ntp.smartordersystem.dto.StockXmlItemDto;
import hr.tvz.ntp.smartordersystem.service.LogHelperService;
import hr.tvz.ntp.smartordersystem.service.StockXmlService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/*
 * REST CONTROLLER ZA PRODUCT-STOCK XML DATOTEKU.
 *
 * OBRANA:
 * Ovdje se kroz HTTP endpointove jasno demonstrira CRUD nad XML-om.
 *
 * CREATE/WRITE -> POST /stock-xml/export
 * READ         -> GET  /stock-xml
 * UPDATE       -> PUT  /stock-xml/{productId}
 * DELETE       -> DELETE /stock-xml/{productId}
 *
 * /snapshot dodatno regenerira XML iz trenutnog stanja baze.
 */
@RestController
@RequestMapping("/stock-xml")
public class StockXmlController {

    private final StockXmlService stockXmlService;
    private final LogHelperService logHelperService;

    public StockXmlController(
            StockXmlService stockXmlService,
            LogHelperService logHelperService) {

        this.stockXmlService =
                stockXmlService;

        this.logHelperService =
                logHelperService;
    }

    /*
     * GET /stock-xml/snapshot
     *
     * Regenerira XML iz trenutnog stanja baze i odmah vraća rezultat.
     *
     * Dakle ovo nije samo READ postojećeg filea.
     * Prvo se radi DB -> XML export.
     */
    @GetMapping("/snapshot")
    public ResponseEntity<List<StockXmlItemDto>>
    refreshAndReadSnapshot(
            Authentication authentication,
            HttpServletRequest request) {

        List<StockXmlItemDto> items =
                stockXmlService
                        .exportFromDatabaseToXml();

        logHelperService.log(
                authentication,
                request,
                "REFRESH_PRODUCT_STOCK_XML_SNAPSHOT",
                "Refreshed product stock XML snapshot from current database state"
        );

        return ResponseEntity.ok(items);
    }

    /*
     * POST /stock-xml/export
     *
     * CREATE / WRITE operacija.
     *
     * Dohvaća Products iz baze i kompletno generira product-stock.xml.
     */
    @PostMapping("/export")
    public ResponseEntity<List<StockXmlItemDto>>
    exportFromDatabaseToXml(
            Authentication authentication,
            HttpServletRequest request) {

        List<StockXmlItemDto> items =
                stockXmlService
                        .exportFromDatabaseToXml();

        logHelperService.log(
                authentication,
                request,
                "EXPORT_PRODUCT_STOCK_XML",
                "Exported product stock from database to XML file"
        );

        return ResponseEntity.ok(items);
    }

    /*
     * GET /stock-xml
     *
     * READ operacija.
     *
     * Čita postojeći product-stock.xml,
     * bez regeneriranja iz baze.
     */
    @GetMapping
    public ResponseEntity<List<StockXmlItemDto>>
    readFromXml() {

        return ResponseEntity.ok(
                stockXmlService.readFromXml()
        );
    }

    /*
     * PUT /stock-xml/{productId}
     *
     * UPDATE operacija nad XML datotekom.
     *
     * VAŽNO:
     * Ovo NE mijenja Product u MySQL bazi.
     * Mijenja samo odgovarajući zapis u XML snapshotu.
     */
    @PutMapping("/{productId}")
    public ResponseEntity<StockXmlItemDto>
    updateXmlItem(
            @PathVariable Long productId,
            @RequestBody StockXmlItemDto updatedItem,
            Authentication authentication,
            HttpServletRequest request) {

        StockXmlItemDto item =
                stockXmlService
                        .updateXmlItem(
                                productId,
                                updatedItem
                        );

        logHelperService.log(
                authentication,
                request,
                "UPDATE_PRODUCT_STOCK_XML",
                "Updated product stock XML item for product id "
                        + productId
        );

        return ResponseEntity.ok(item);
    }

    /*
     * DELETE /stock-xml/{productId}
     *
     * DELETE operacija iz XML datoteke.
     *
     * Ponovno:
     * Product u bazi se time NE briše.
     */
    @DeleteMapping("/{productId}")
    public ResponseEntity<Void>
    deleteXmlItem(
            @PathVariable Long productId,
            Authentication authentication,
            HttpServletRequest request) {

        stockXmlService
                .deleteXmlItem(productId);

        logHelperService.log(
                authentication,
                request,
                "DELETE_PRODUCT_STOCK_XML",
                "Deleted product stock XML item for product id "
                        + productId
        );

        return ResponseEntity
                .noContent()
                .build();
    }
}