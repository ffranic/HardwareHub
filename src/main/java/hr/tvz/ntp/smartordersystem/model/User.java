package hr.tvz.ntp.smartordersystem.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * MODEL / ENTITY KLASA
 *
 * Predstavlja korisnika aplikacije i mapira se na tablicu "users" u MySQL bazi.
 *
 * OBRANA:
 * - @Entity -> JPA/Hibernate zna da je ovo entitet koji se sprema u bazu.
 * - @Table -> određuje naziv tablice.
 * - Lombok generira konstruktore, gettere, settere, equals/hashCode i toString.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User {

    /*
     * Primarni ključ tablice users.
     *
     * GenerationType.IDENTITY znači da ID generira sama baza,
     * npr. MySQL AUTO_INCREMENT.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Bean Validation:
     * @NotBlank ne dopušta null, prazan string niti string samo s razmacima.
     *
     * JPA:
     * nullable=false -> stupac u bazi ne smije biti NULL.
     * unique=true -> ne mogu postojati dva korisnika s istim usernameom.
     */
    @NotBlank(message = "Username is required")
    @Column(name = "username", nullable = false, unique = true)
    private String username;

    /*
     * Lozinka je obavezna i sprema se u stupac password.
     *
     * @JsonProperty(access = WRITE_ONLY) je sigurnosno vrlo važan:
     *
     * Jackson može primiti password iz JSON requesta:
     * {
     *   "username": "...",
     *   "password": "..."
     * }
     *
     * ali ga NEĆE vratiti u JSON responseu.
     *
     * Time se sprječava slučajno slanje hashirane lozinke klijentu.
     *
     */
    @NotBlank(message = "Password is required")
    @Column(name = "password", nullable = false)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    /*
     * Email mora:
     * 1. postojati (@NotBlank)
     * 2. odgovarati email formatu (@Email)
     * 3. biti jedinstven u bazi (unique=true)
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Column(name = "email", unique = true)
    private String email;

    /*
     * Role je Java enum, npr. ADMIN ili CUSTOMER.
     *
     * @Enumerated(EnumType.STRING)
     * znači da se u bazi sprema tekst:
     *
     * ADMIN
     * CUSTOMER
     *
     * umjesto numeričkih vrijednosti 0, 1...
     *
     * OBRANA:
     * STRING je sigurniji od ORDINAL-a jer promjena redoslijeda enum
     * vrijednosti ne mijenja značenje već spremljenih podataka.
     */
    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    private Role role;
}