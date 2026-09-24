package hr.tvz.ntp.smartordersystem.service;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*
 * SERVIS ZA ASINKRONO GENERIRANJE I SPREMANJE PDF-a NARUDŽBE.
 *
 * Glavna ideja:
 *
 * kada treba generirati PDF narudžbe, posao se ne izvršava
 * nužno na dretvi koja je pozvala ovu metodu.
 *
 * Umjesto toga zadatak se predaje Thread Pool-u koji ima
 * maksimalno 4 worker dretve.
 *
 * Time generiranje PDF-a može biti odvojeno od glavnog toka
 * obrade zahtjeva.
 */
@Service
public class OrderPdfAsyncService {

    /*
     * Broj dretvi u Thread Pool-u.
     *
     * Maksimalno 4 zadatka mogu se istovremeno izvršavati
     * u ovom pool-u.
     *
     * Ako dođe više od 4 zadatka, ostali čekaju u redu
     * dok neka worker dretva ne postane slobodna.
     */
    private static final int THREAD_POOL_SIZE = 4;

    /*
     * Direktorij u koji spremamo generirane PDF dokumente.
     *
     * Path.of() stvara Java Path objekt koji predstavlja putanju:
     *
     * order-pdfs/
     */
    private static final Path PDF_DIR =
            Path.of("order-pdfs");

    /*
     * PdfService sadrži stvarnu logiku generiranja PDF dokumenta.
     *
     * Ovaj servis NE generira sadržaj PDF-a sam.
     * Njegov posao je pokrenuti generiranje asinkrono
     * i spremiti rezultat na disk.
     */
    private final PdfService pdfService;

    /*
     * ExecutorService upravlja bazenom dretvi.
     *
     * newFixedThreadPool(4):
     *
     * kreira Thread Pool s 4 worker dretve.
     *
     * Ne kreiramo ručno:
     *
     * new Thread(...)
     *
     * za svaki novi posao, nego ponovno koristimo dretve
     * kojima upravlja ExecutorService.
     */
    private final ExecutorService executorService =
            Executors.newFixedThreadPool(
                    THREAD_POOL_SIZE
            );

    /*
     * Constructor injection.
     *
     * Spring će pronaći implementaciju PdfService-a
     * i predati je ovom servisu.
     */
    public OrderPdfAsyncService(
            PdfService pdfService) {

        this.pdfService = pdfService;
    }

    /*
     * GLAVNA ASINKRONA METODA.
     *
     * Prima ID narudžbe i predaje posao Thread Pool-u.
     *
     * VAŽNO:
     * metoda ne čeka da se cijeli PDF generira.
     *
     * submit() predaje zadatak ExecutorService-u,
     * a slobodna worker dretva će ga izvršiti.
     */
    public void generateAndStoreOrderPdf(
            Long orderId) {

        /*
         * Lambda predstavlja zadatak koji treba izvršiti.
         *
         * () -> { ... }
         *
         * Ovaj kod će izvršavati worker dretva iz Thread Pool-a.
         */
        executorService.submit(() -> {

            try {

                /*
                 * Osiguravamo da postoji direktorij:
                 *
                 * order-pdfs/
                 *
                 * Ako već postoji, createDirectories()
                 * neće napraviti problem.
                 */
                Files.createDirectories(
                        PDF_DIR
                );

                /*
                 * PdfService generira PDF.
                 *
                 * Rezultat je byte[] jer je PDF binarna datoteka.
                 *
                 * Primjer:
                 *
                 * orderId = 15
                 *
                 * -> generira PDF za narudžbu 15
                 */
                byte[] pdfBytes =
                        pdfService.generateOrderPdf(
                                orderId
                        );

                /*
                 * Određujemo gdje ćemo PDF spremiti.
                 *
                 * Npr.:
                 *
                 * order-pdfs/order-15.pdf
                 */
                Path outputPath =
                        getPdfPath(orderId);

                /*
                 * Zapisujemo byte[] na disk.
                 *
                 * Dakle:
                 *
                 * byte[] -> .pdf datoteka
                 */
                Files.write(
                        outputPath,
                        pdfBytes
                );

            } catch (Exception e) {

                /*
                 * Exception se hvata unutar worker zadatka.
                 *
                 * Ako generiranje jednog PDF-a ne uspije,
                 * exception se ovdje evidentira.
                 */
                System.err.println(
                        "Could not generate PDF for order "
                                + orderId
                );

                e.printStackTrace();
            }
        });
    }

    /*
     * Provjerava postoji li već spremljeni PDF
     * određene narudžbe.
     *
     * Files.exists(Path) vraća:
     *
     * true  -> postoji
     * false -> ne postoji
     */
    public boolean pdfExists(Long orderId) {

        return Files.exists(
                getPdfPath(orderId)
        );
    }

    /*
     * Čita već spremljeni PDF s diska.
     *
     * PDF je binarna datoteka pa ga učitavamo kao byte[].
     */
    public byte[] readStoredPdf(Long orderId) {

        try {

            /*
             * Files.readAllBytes():
             *
             * čita cijeli sadržaj datoteke
             * i vraća ga kao byte[].
             */
            return Files.readAllBytes(
                    getPdfPath(orderId)
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Could not read stored PDF for order "
                            + orderId,
                    e
            );
        }
    }

    /*
     * Helper metoda koja izrađuje putanju PDF datoteke.
     *
     * PDF_DIR:
     * order-pdfs
     *
     * orderId:
     * 15
     *
     * rezultat:
     * order-pdfs/order-15.pdf
     */
    public Path getPdfPath(Long orderId) {

        return PDF_DIR.resolve(
                "order-" + orderId + ".pdf"
        );
    }

    /*
     * Spring poziva ovu metodu prije uništavanja ovog servisa,
     * odnosno prilikom gašenja aplikacije.
     *
     * ExecutorService treba pravilno ugasiti.
     *
     * shutdown() znači:
     *
     * - više ne prihvaćaj nove zadatke
     * - već predani zadaci mogu završiti
     */
    @PreDestroy
    public void shutdown() {

        executorService.shutdown();
    }
}