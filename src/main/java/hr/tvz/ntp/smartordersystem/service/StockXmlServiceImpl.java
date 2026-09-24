package hr.tvz.ntp.smartordersystem.service;

import hr.tvz.ntp.smartordersystem.dto.StockXmlItemDto;
import hr.tvz.ntp.smartordersystem.dto.StockXmlWrapperDto;
import hr.tvz.ntp.smartordersystem.model.Product;
import hr.tvz.ntp.smartordersystem.repository.ProductRepository;
import jakarta.transaction.Transactional;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.NoSuchElementException;

/*
 * SERVIS ZA OBRADU PRODUCT STOCK PODATAKA U XML FORMATU.
 *
 * Datoteka:
 *
 * product-stock.xml
 *
 * Podržava:
 *
 * CREATE/WRITE -> exportFromDatabaseToXml()
 * READ         -> readFromXml()
 * UPDATE       -> updateXmlItem()
 * DELETE       -> deleteXmlItem()
 *
 * OBRANA:
 * Ovo je puni CRUD nad XML datotekom.
 *
 * JAXB se koristi za:
 *
 * Java objekt -> XML  (Marshaller)
 * XML -> Java objekt  (Unmarshaller)
 */
@Service
@Transactional
public class StockXmlServiceImpl implements StockXmlService {

    private static final String XML_FILE =
            "product-stock.xml";

    private final ProductRepository productRepository;

    public StockXmlServiceImpl(
            ProductRepository productRepository) {

        this.productRepository =
                productRepository;
    }

    /*
     * CREATE / WRITE.
     *
     * Trenutno stanje proizvoda iz baze izvozi u XML snapshot.
     */
    @Override
    public List<StockXmlItemDto> exportFromDatabaseToXml() {

        /*
         * Product entityje pretvaramo u XML DTO objekte.
         *
         * XML ne mora znati ništa o JPA entitetima.
         */
        List<StockXmlItemDto> items =
                productRepository
                        .findAll()
                        .stream()
                        .map(this::mapProductToDto)
                        .toList();

        /*
         * DTO lista -> XML file.
         */
        writeXml(items);

        return items;
    }

    /*
     * READ.
     */
    @Override
    public List<StockXmlItemDto> readFromXml() {

        return readXml();
    }

    /*
     * UPDATE JEDNOG XML ZAPISA.
     */
    @Override
    public StockXmlItemDto updateXmlItem(
            Long productId,
            StockXmlItemDto updatedItem) {

        /*
         * XML prvo deserijaliziramo u Java listu.
         */
        List<StockXmlItemDto> items =
                readXml();

        /*
         * Tražimo element prema productId.
         */
        for (int i = 0;
             i < items.size();
             i++) {

            StockXmlItemDto existing =
                    items.get(i);

            if (existing
                    .getProductId()
                    .equals(productId)) {

                /*
                 * ID se ne mijenja kroz update.
                 *
                 * Path ID je autoritativan.
                 */
                updatedItem.setProductId(
                        productId);

                /*
                 * Partial update pristup:
                 *
                 * ako novo ime nije poslano,
                 * ostaje staro.
                 */
                if (updatedItem.getProductName() == null
                        || updatedItem
                        .getProductName()
                        .isBlank()) {

                    updatedItem.setProductName(
                            existing.getProductName());
                }

                if (updatedItem.getBrand() == null
                        || updatedItem
                        .getBrand()
                        .isBlank()) {

                    updatedItem.setBrand(
                            existing.getBrand());
                }

                if (updatedItem.getCategoryName() == null
                        || updatedItem
                        .getCategoryName()
                        .isBlank()) {

                    updatedItem.setCategoryName(
                            existing.getCategoryName());
                }

                if (updatedItem.getPrice() == null) {

                    updatedItem.setPrice(
                            existing.getPrice());
                }

                /*
                 * Zamjenjujemo postojeći objekt unutar liste.
                 */
                items.set(
                        i,
                        updatedItem);

                /*
                 * Cijelu novu listu ponovno zapisujemo u XML.
                 */
                writeXml(items);

                return updatedItem;
            }
        }

        throw new NoSuchElementException(
                "Stock XML item not found for product id: "
                        + productId
        );
    }

    /*
     * DELETE XML ZAPISA.
     */
    @Override
    public void deleteXmlItem(
            Long productId) {

        List<StockXmlItemDto> items =
                readXml();

        /*
         * removeIf:
         *
         * uklanja svaki zapis s odgovarajućim productId.
         *
         * vraća true ako je nešto stvarno uklonjeno.
         */
        boolean removed =
                items.removeIf(
                        item ->
                                item.getProductId()
                                        .equals(productId)
                );

        if (!removed) {

            throw new NoSuchElementException(
                    "Stock XML item not found for product id: "
                            + productId
            );
        }

        /*
         * Ponovno zapisujemo XML bez obrisanog elementa.
         */
        writeXml(items);
    }

    /*
     * Product entity -> StockXmlItemDto.
     *
     * Namjerno spremamo samo podatke relevantne za stock snapshot.
     */
    private StockXmlItemDto mapProductToDto(
            Product product) {

        return new StockXmlItemDto(
                product.getId(),
                product.getName(),
                product.getBrand(),

                /*
                 * Category je relacija.
                 *
                 * Ako postoji, u XML spremamo samo njezin naziv.
                 */
                product.getCategory() != null
                        ? product
                          .getCategory()
                          .getName()
                        : "-",

                product.getPrice(),
                product.getStock()
        );
    }

    /*
     * XML -> Java.
     */
    private List<StockXmlItemDto> readXml() {

        try {

            File file =
                    getOrCreateFile();

            /*
             * Defensive check za prazan file.
             */
            if (file.length() == 0) {
                return List.of();
            }

            /*
             * JAXBContext zna koje Java klase sudjeluju u XML mapiranju.
             *
             * Root objekt je StockXmlWrapperDto.
             */
            JAXBContext context =
                    JAXBContext.newInstance(
                            StockXmlWrapperDto.class);

            /*
             * Unmarshaller:
             *
             * XML -> Java objekt.
             */
            Unmarshaller unmarshaller =
                    context.createUnmarshaller();

            StockXmlWrapperDto wrapper =
                    (StockXmlWrapperDto)
                            unmarshaller.unmarshal(file);

            if (wrapper.getItems() == null) {
                return List.of();
            }

            return wrapper.getItems();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Could not read product stock XML file.",
                    e
            );
        }
    }

    /*
     * Java -> XML.
     */
    private void writeXml(
            List<StockXmlItemDto> items) {

        try {

            File file =
                    getOrCreateFile();

            /*
             * JAXB treba jedan root element.
             *
             * Zato List ne marshallamo direktno,
             * nego je zamotamo u StockXmlWrapperDto.
             */
            StockXmlWrapperDto wrapper =
                    new StockXmlWrapperDto(items);

            JAXBContext context =
                    JAXBContext.newInstance(
                            StockXmlWrapperDto.class);

            /*
             * Marshaller:
             *
             * Java objekt -> XML.
             */
            Marshaller marshaller =
                    context.createMarshaller();

            /*
             * Pretty-print XML.
             *
             * Bez ovoga bi XML mogao završiti u jednom dugom redu.
             */
            marshaller.setProperty(
                    Marshaller.JAXB_FORMATTED_OUTPUT,
                    true
            );

            /*
             * Serijalizacija Java wrappera u file.
             */
            marshaller.marshal(
                    wrapper,
                    file
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Could not write product stock XML file.",
                    e
            );
        }
    }

    /*
     * Osigurava postojanje validne XML datoteke.
     */
    private File getOrCreateFile()
            throws Exception {

        File file =
                new File(XML_FILE);

        if (!file.exists()) {

            /*
             * Ne stvaramo samo prazan 0-byte file.
             *
             * Napravimo validan XML s praznom listom.
             */
            writeEmptyXml(file);
        }

        return file;
    }

    /*
     * Kreira početni validni XML dokument bez stockItem zapisa.
     */
    private void writeEmptyXml(
            File file)
            throws Exception {

        StockXmlWrapperDto wrapper =
                new StockXmlWrapperDto(
                        List.of());

        JAXBContext context =
                JAXBContext.newInstance(
                        StockXmlWrapperDto.class);

        Marshaller marshaller =
                context.createMarshaller();

        marshaller.setProperty(
                Marshaller.JAXB_FORMATTED_OUTPUT,
                true
        );

        marshaller.marshal(
                wrapper,
                file
        );
    }
}