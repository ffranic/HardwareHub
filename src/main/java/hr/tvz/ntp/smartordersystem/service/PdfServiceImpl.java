package hr.tvz.ntp.smartordersystem.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import hr.tvz.ntp.smartordersystem.model.Order;
import hr.tvz.ntp.smartordersystem.model.OrderItem;
import hr.tvz.ntp.smartordersystem.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.NoSuchElementException;

/*
 * SERVIS ZA GENERIRANJE PDF POTVRDE NARUDŽBE.
 *
 * Koristi OpenPDF / com.lowagie API.
 *
 * Ulaz:
 * orderId
 *
 * Izlaz:
 * byte[] koji predstavlja gotovu PDF datoteku.
 *
 * Prednost byte[] pristupa:
 * controller ga može direktno vratiti kroz HTTP response,
 * bez potrebe za privremenim spremanjem PDF-a na disk.
 */
@Service
public class PdfServiceImpl implements PdfService {

    private final OrderRepository orderRepository;

    public PdfServiceImpl(
            OrderRepository orderRepository) {

        this.orderRepository =
                orderRepository;
    }

    /*
     * GENERIRANJE PDF-A ZA JEDNU NARUDŽBU.
     */
    @Override
    public byte[] generateOrderPdf(Long orderId) {

        /*
         * Koristimo poseban repository query koji FETCH-a OrderIteme
         * i njihove Product reference.
         *
         * OBRANA:
         * Time izbjegavamo LazyInitializationException tijekom
         * generiranja PDF-a.
         */
        Order order =
                orderRepository
                        .findByIdWithItems(orderId)
                        .orElseThrow(() ->
                                new NoSuchElementException(
                                        "Order not found"));

        /*
         * ByteArrayOutputStream drži cijeli PDF u memoriji.
         *
         * try-with-resources automatski zatvara stream.
         */
        try (ByteArrayOutputStream out =
                     new ByteArrayOutputStream()) {

            /*
             * Kreiramo A4 dokument.
             */
            Document document =
                    new Document(PageSize.A4);

            /*
             * PdfWriter povezuje Document s output streamom.
             */
            PdfWriter.getInstance(
                    document,
                    out
            );

            /*
             * Dokument mora biti otvoren prije dodavanja sadržaja.
             */
            document.open();

            /*
             * Definicija fontova.
             */
            Font titleFont =
                    new Font(
                            Font.HELVETICA,
                            18,
                            Font.BOLD
                    );

            Font normalFont =
                    new Font(
                            Font.HELVETICA,
                            12
                    );

            Font boldFont =
                    new Font(
                            Font.HELVETICA,
                            12,
                            Font.BOLD
                    );

            /*
             * Naslov PDF-a.
             */
            Paragraph title =
                    new Paragraph(
                            "Order Confirmation",
                            titleFont
                    );

            title.setAlignment(
                    Element.ALIGN_CENTER);

            title.setSpacingAfter(20);

            document.add(title);

            /*
             * Osnovni podaci narudžbe.
             */
            document.add(
                    new Paragraph(
                            "Order ID: "
                                    + order.getId(),
                            normalFont));

            document.add(
                    new Paragraph(
                            "Customer: "
                                    + order.getUser()
                                    .getUsername(),
                            normalFont));

            document.add(
                    new Paragraph(
                            "Email: "
                                    + order.getUser()
                                    .getEmail(),
                            normalFont));

            document.add(
                    new Paragraph(
                            "Status: "
                                    + order.getStatus(),
                            normalFont));

            /*
             * Datum formatiramo u korisniku čitljiv format.
             */
            document.add(
                    new Paragraph(
                            "Order date: "
                                    + order.getOrder_date()
                                    .format(
                                            DateTimeFormatter
                                                    .ofPattern(
                                                            "dd.MM.yyyy. HH:mm")),
                            normalFont));

            /*
             * Prazan red.
             */
            document.add(
                    Chunk.NEWLINE);

            /*
             * Tablica s 5 stupaca:
             *
             * Product
             * Brand
             * Qty
             * Unit price
             * Subtotal
             */
            PdfPTable table =
                    new PdfPTable(5);

            /*
             * Tablica zauzima cijelu raspoloživu širinu.
             */
            table.setWidthPercentage(100);

            table.setSpacingBefore(10);

            /*
             * Relativne širine stupaca.
             *
             * Product je širi od Qty itd.
             */
            table.setWidths(
                    new float[]{
                            3,
                            2,
                            1,
                            2,
                            2
                    }
            );

            /*
             * Header red.
             */
            addHeaderCell(
                    table,
                    "Product");

            addHeaderCell(
                    table,
                    "Brand");

            addHeaderCell(
                    table,
                    "Qty");

            addHeaderCell(
                    table,
                    "Unit price");

            addHeaderCell(
                    table,
                    "Subtotal");

            /*
             * Za novac koristimo BigDecimal.
             */
            BigDecimal total =
                    BigDecimal.ZERO;

            /*
             * Za svaku OrderItem stavku dodajemo red.
             */
            for (OrderItem item :
                    order.getOrderItems()) {

                /*
                 * Subtotal:
                 *
                 * cijena u trenutku kupnje * količina
                 *
                 * VAŽNO:
                 * ne koristimo trenutni Product.price.
                 */
                BigDecimal subtotal =
                        item.getPrice_at_order_time()
                                .multiply(
                                        BigDecimal.valueOf(
                                                item.getQuantity()));

                total =
                        total.add(subtotal);

                table.addCell(
                        item.getProduct()
                                .getName());

                table.addCell(
                        item.getProduct()
                                .getBrand());

                table.addCell(
                        String.valueOf(
                                item.getQuantity()));

                table.addCell(
                        item.getPrice_at_order_time()
                                .toString()
                                + " EUR");

                table.addCell(
                        subtotal.toString()
                                + " EUR");
            }

            document.add(table);

            document.add(
                    Chunk.NEWLINE);

            /*
             * Konačni total.
             */
            Paragraph totalParagraph =
                    new Paragraph(
                            "Total: "
                                    + total
                                    + " EUR",
                            boldFont
                    );

            totalParagraph.setAlignment(
                    Element.ALIGN_RIGHT);

            document.add(
                    totalParagraph);

            /*
             * close() finalizira PDF strukturu.
             *
             * Nakon ovoga ByteArrayOutputStream sadrži kompletan PDF.
             */
            document.close();

            return out.toByteArray();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Error generating PDF",
                    e
            );
        }
    }

    /*
     * Helper za header ćelije.
     *
     * Centralizira styling da ga ne dupliciramo 5 puta.
     */
    private void addHeaderCell(
            PdfPTable table,
            String text) {

        PdfPCell header =
                new PdfPCell();

        /*
         * Phrase = tekst + font.
         */
        header.setPhrase(
                new Phrase(
                        text,
                        new Font(
                                Font.HELVETICA,
                                12,
                                Font.BOLD)
                )
        );

        header.setHorizontalAlignment(
                Element.ALIGN_CENTER);

        header.setPadding(8);

        table.addCell(header);
    }
}