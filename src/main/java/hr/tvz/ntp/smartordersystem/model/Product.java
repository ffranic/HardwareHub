package hr.tvz.ntp.smartordersystem.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/*
 * JPA ENTITY KOJI PREDSTAVLJA PROIZVOD U BAZI.
 *
 * Tablica:
 * products
 *
 * OBRANA:
 * Ova klasa je posebno bitna za:
 * - CRUD nad proizvodima
 * - lookup vezu Product -> Category
 * - BLOB spremanje slike
 * - transient calculated/helper polje hasImage
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "products")
public class Product {

    /*
     * Primarni ključ tablice products.
     *
     * GenerationType.IDENTITY znači da ID generira baza,
     * npr. MySQL AUTO_INCREMENT.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    /*
     * Obično tekstualno polje proizvoda.
     *
     * Mapira se na stupac "name".
     */
    @Column(name = "name")
    private String name;


    /*
     * Brand proizvoda.
     */
    @Column(name = "brand")
    private String brand;


    /*
     * LOOKUP POLJE / RELACIJA.
     *
     * OBRANA:
     * Product ne sprema samo tekstualni naziv kategorije,
     * nego referencira zaseban Category entitet.
     *
     * @ManyToOne znači:
     *
     * više proizvoda može pripadati jednoj kategoriji.
     *
     * Primjer:
     *
     * Product Laptop 1 -> Category Laptops
     * Product Laptop 2 -> Category Laptops
     *
     * Zato je odnos:
     *
     * MANY Products -> ONE Category
     */
    @ManyToOne

    /*
     * @JoinColumn određuje foreign key stupac u tablici products.
     *
     * products.category_id
     *
     * referencira:
     *
     * categories.id
     *
     * OBRANA:
     * Upravo preko ovog foreign keya možemo dobiti lookup vrijednost
     * category.name.
     */
    @JoinColumn(name = "category_id")
    private Category category;


    /*
     * Cijenu držimo kao BigDecimal.
     *
     * OBRANA:
     * Za novčane vrijednosti BigDecimal je bolji od double/float
     * zbog preciznosti decimalnih izračuna.
     */
    @Column(name = "price")
    private BigDecimal price;


    /*
     * Tekstualni opis proizvoda.
     */
    @Column(name = "description")
    private String description;


    /*
     * Trenutna količina proizvoda na zalihi.
     */
    @Column(name = "stock")
    private int stock;


    /*
     * Slika proizvoda se ne želi automatski slati u JSON odgovorima.
     *
     * OBRANA:
     * Bez @JsonIgnore, Jackson bi byte[] mogao pokušati uključiti
     * u svaki JSON odgovor za Product, što bi bilo nepotrebno veliko.
     *
     * Zato se slika dohvaća posebnim endpointom:
     *
     * GET /products/{id}/image
     */
    @JsonIgnore

    /*
     * @Lob = Large Object.
     *
     * Hibernate/JPA zna da se radi o velikom binarnom sadržaju.
     */
    @Lob

    /*
     * U MySQL bazi koristi se LONGBLOB.
     *
     * Dakle binarni sadržaj slike sprema se direktno u bazu.
     */
    @Column(
            name = "image",
            columnDefinition = "LONGBLOB"
    )

    /*
     * LAZY učitavanje znači da Hibernate pokušava izbjeći učitavanje
     * velikog image BLOB-a dok stvarno nije potreban.
     *
     * OBRANA:
     * Slike mogu biti velike, pa nema smisla automatski učitavati
     * svaki BLOB kad samo želimo listu proizvoda.
     */
    @Basic(fetch = FetchType.LAZY)
    private byte[] image;


    /*
     * @Transient znači:
     *
     * ovo NIJE stupac u bazi.
     *
     * Hibernate/JPA neće pokušati spremiti "hasImage" u tablicu products.
     *
     * Vrijednost se računa iz postojećeg image polja.
     *
     * OBRANA:
     * Ovo je calculated/helper svojstvo na razini Java objekta.
     */
    @Transient
    public boolean isHasImage() {

        /*
         * true samo ako image postoji i ima barem jedan byte.
         */
        return image != null
                && image.length > 0;
    }
}