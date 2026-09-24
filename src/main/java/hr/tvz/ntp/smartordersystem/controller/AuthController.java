package hr.tvz.ntp.smartordersystem.controller;

import hr.tvz.ntp.smartordersystem.model.User;
import hr.tvz.ntp.smartordersystem.repository.UserRepository;
import hr.tvz.ntp.smartordersystem.service.PasswordPreHashService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * REST CONTROLLER ZA AUTENTIKACIJU.
 *
 * Implementira:
 * - login
 * - logout
 * - dohvat trenutno prijavljenog korisnika
 *
 * OBRANA:
 * Ovdje se koristi session-based autentikacija.
 * Nakon uspješnog logina Authentication objekt se sprema
 * u Spring SecurityContext, a SecurityContext u HttpSession.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPreHashService passwordPreHashService;

    public AuthController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            PasswordPreHashService passwordPreHashService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPreHashService = passwordPreHashService;
    }

    /*
     * POST /auth/login
     *
     * Prima JSON oblika:
     *
     * {
     *   "username": "...",
     *   "password": "..."
     * }
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {

        String username = request.get("username");
        String password = request.get("password");

        /*
         * Pronalazimo korisnika prema usernameu.
         *
         * Namjerno koristimo generičku poruku:
         * "Invalid username or password"
         *
         * kako se napadaču ne bi otkrivalo postoji li username.
         */
        User user =
                userRepository.findByUsername(username)
                        .orElseThrow(() ->
                                new BadCredentialsException(
                                        "Invalid username or password"));

        /*
         * Lozinka se prvo obrađuje kroz:
         *
         * raw password
         * + variable salt
         * + pepper
         * -> SHA-256
         *
         * OBRANA:
         * U bazu nije spremljen ovaj SHA-256 rezultat,
         * nego BCrypt hash tog pre-hasha.
         */
        String preHashedPassword =
                passwordPreHashService
                        .preHashPassword(
                                username,
                                password);

        /*
         * BCrypt provjera.
         *
         * matches(rawEquivalent, storedBcryptHash)
         *
         * PasswordEncoder zna pročitati salt iz samog BCrypt hasha.
         */
        if (!passwordEncoder.matches(
                preHashedPassword,
                user.getPassword())) {

            throw new BadCredentialsException(
                    "Invalid username or password"
            );
        }

        /*
         * Spring Security radi s GrantedAuthority objektima.
         *
         * Naš enum:
         * ADMIN
         *
         * pretvaramo u:
         * ROLE_ADMIN
         *
         * jer hasRole("ADMIN") interno očekuje prefix ROLE_.
         */
        List<GrantedAuthority> authorities =
                List.of(
                        new SimpleGrantedAuthority(
                                "ROLE_" + user.getRole().name())
                );

        /*
         * Kreiramo Authentication objekt koji predstavlja
         * uspješno autentificiranog korisnika.
         *
         * principal = username
         * credentials = null
         * authorities = role
         *
         * Password više nije potreban nakon uspješne autentikacije.
         */
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        user.getUsername(),
                        null,
                        authorities
                );

        /*
         * Kreiramo novi SecurityContext.
         *
         * SecurityContext predstavlja sigurnosno stanje
         * trenutnog korisnika/requesta.
         */
        SecurityContext context =
                SecurityContextHolder.createEmptyContext();

        context.setAuthentication(authentication);

        /*
         * Postavljamo context u SecurityContextHolder
         * za trenutnu dretvu/request.
         */
        SecurityContextHolder.setContext(context);

        /*
         * Dohvaća postojeću session ili kreira novu.
         *
         * true -> kreiraj session ako još ne postoji.
         */
        HttpSession session =
                httpRequest.getSession(true);

        /*
         * Ovo je vrlo bitna linija.
         *
         * SecurityContext se ručno sprema u HttpSession pod ključem
         * kojeg očekuje Spring Security.
         *
         * Zato će Spring u sljedećem HTTP requestu znati
         * tko je prijavljen.
         */
        session.setAttribute(
                HttpSessionSecurityContextRepository
                        .SPRING_SECURITY_CONTEXT_KEY,
                context
        );

        /*
         * Vraćamo podatke o korisniku, ali NE password.
         */
        return ResponseEntity.ok(
                Map.of(
                        "id", user.getId(),
                        "username", user.getUsername(),
                        "email", user.getEmail(),
                        "role", user.getRole()
                )
        );
    }

    /*
     * POST /auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            HttpServletRequest request) {

        /*
         * false znači:
         * nemoj kreirati novu session ako ne postoji.
         */
        HttpSession session =
                request.getSession(false);

        /*
         * Invalidiranjem session briše se server-side session state.
         */
        if (session != null) {
            session.invalidate();
        }

        /*
         * Dodatno čistimo SecurityContext trenutne dretve.
         */
        SecurityContextHolder.clearContext();

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Logout successful"
                )
        );
    }

    /*
     * GET /auth/me
     *
     * Frontend koristi ovaj endpoint da provjeri:
     * - je li korisnik prijavljen
     * - njegov ID
     * - username
     * - email
     * - role
     */
    @GetMapping("/me")
    public ResponseEntity<?> me(
            Authentication authentication) {

        /*
         * Ako SecurityContext nema Authentication,
         * korisnik nije prijavljen.
         */
        if (authentication == null
                || !authentication.isAuthenticated()) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(
                            Map.of(
                                    "error",
                                    "Not authenticated"
                            )
                    );
        }

        /*
         * Principal je username jer smo ga tako postavili kod login-a.
         */
        String username =
                authentication.getName();

        /*
         * Authentication ne sadrži sve poslovne podatke o Useru,
         * pa korisnika ponovno dohvaćamo iz baze.
         */
        User user =
                userRepository
                        .findByUsername(username)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "User not found"));

        Map<String, Object> response =
                new HashMap<>();

        response.put(
                "id",
                user.getId());

        response.put(
                "username",
                user.getUsername());

        response.put(
                "email",
                user.getEmail());

        response.put(
                "role",
                user.getRole().name());

        return ResponseEntity.ok(response);
    }
}