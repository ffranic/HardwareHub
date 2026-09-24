package hr.tvz.ntp.smartordersystem;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestClient;

@Configuration
public class SecurityConfig {

    private final UserDetailsService userDetailsService;

    public SecurityConfig(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .authenticationProvider(authenticationProvider())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .authorizeHttpRequests(auth -> auth

                        // =========================
                        // PUBLIC RESOURCES
                        // =========================

                        .requestMatchers(
                                "/",
                                "/auth/login",
                                "/auth/logout",
                                "/auth/register",
                                "/css/**",
                                "/js/**",
                                "/images/**"
                        ).permitAll()

                        // Public HTML pages only.
                        // Administration page is intentionally NOT included here.
                        .requestMatchers(
                                "/html/index.html",
                                "/html/signIn.html",
                                "/html/registration.html"
                        ).permitAll()

                        // Public product browsing.
                        // Only GET is public - create/update/delete remain protected.
                        .requestMatchers(HttpMethod.GET, "/products/**").permitAll()

                        // Registration / public user creation if still used by registration page.
                        .requestMatchers(HttpMethod.POST, "/users").permitAll()

                        // Application settings must be readable before login
                        // because the frontend uses language / display settings.
                        .requestMatchers(HttpMethod.GET, "/settings").permitAll()


                        // =========================
                        // ADMIN HTML
                        // =========================

                        .requestMatchers("/html/administration.html").hasRole("ADMIN")


                        // =========================
                        // ADMIN-ONLY API
                        // =========================

                        .requestMatchers("/activity-logs/**").hasRole("ADMIN")
                        .requestMatchers("/admin-edit-locks/**").hasRole("ADMIN")
                        .requestMatchers("/order-archive/**").hasRole("ADMIN")
                        .requestMatchers("/external/**").hasRole("ADMIN")
                        .requestMatchers("/digital-signature/**").hasRole("ADMIN")
                        .requestMatchers("/binary-stock/**").hasRole("ADMIN")
                        .requestMatchers("/analysis/**").hasRole("ADMIN")
                        .requestMatchers("/crypto/**").hasRole("ADMIN")
                        .requestMatchers("/stock-xml/**").hasRole("ADMIN")
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.PUT, "/settings").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.GET, "/users").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/users/**").hasRole("ADMIN")

                        // Product administration.
                        .requestMatchers(HttpMethod.POST, "/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/products/**").hasRole("ADMIN")

                        // Order administration MUST be placed before general /orders/**.
                        .requestMatchers(HttpMethod.GET, "/orders/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/orders/*/status").hasRole("ADMIN")


                        // =========================
                        // ADMIN + CUSTOMER
                        // =========================

                        .requestMatchers("/user/**").hasAnyRole("ADMIN", "CUSTOMER")
                        .requestMatchers("/cart/**").hasAnyRole("ADMIN", "CUSTOMER")
                        .requestMatchers("/orders/**").hasAnyRole("ADMIN", "CUSTOMER")


                        // =========================
                        // EVERYTHING ELSE
                        // =========================

                        .anyRequest().authenticated()
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder()
                .requestFactory(
                        new org.springframework.http.client.SimpleClientHttpRequestFactory() {{
                            setConnectTimeout(5000);
                            setReadTimeout(5000);
                        }}
                );
    }
}