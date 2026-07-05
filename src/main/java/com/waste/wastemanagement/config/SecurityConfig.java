package com.waste.wastemanagement.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.waste.wastemanagement.service.CustomOAuth2UserService;
import com.waste.wastemanagement.service.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Map;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final CustomOAuth2UserService oauth2UserService;

    // Jackson ObjectMapper for writing JSON responses
    private final ObjectMapper mapper = new ObjectMapper();

    public SecurityConfig(CustomUserDetailsService userDetailsService,
                          @Lazy CustomOAuth2UserService oauth2UserService) {
        this.userDetailsService = userDetailsService;
        this.oauth2UserService = oauth2UserService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ── URL Access Rules ──────────────────────────────────────────
            .authorizeHttpRequests(auth -> auth
                // Admin API — only ADMIN role
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // Worker API — only WORKER role
                .requestMatchers("/api/worker/**").hasRole("WORKER")
                // Citizen complaint submission — USER or ADMIN only (not WORKER)
                .requestMatchers("/api/complaints").hasAnyRole("USER", "ADMIN")
                // Choose-role — any logged-in user (PENDING role)
                .requestMatchers("/api/auth/choose-role").authenticated()
                // Auth endpoints and OAuth2 — public
                .requestMatchers("/api/auth/**", "/oauth2/**", "/login/oauth2/**").permitAll()
                // All other /api/** — must be logged in
                .requestMatchers("/api/**").authenticated()
                // Vue build output static assets & public SPA routes
                .requestMatchers("/", "/index.html", "/login", "/register").permitAll()
                .requestMatchers("/assets/**", "/*.js", "/*.css", "/*.ico", "/*.svg", "/*.png", "/vite.svg").permitAll()
                
                // Role-based access control for SPA page routes
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/worker/**").hasRole("WORKER")
                .requestMatchers("/choose-role").authenticated()

                // Old Thymeleaf routes — kept working for backward compatibility
                .requestMatchers("/css/**").permitAll()
                .anyRequest().authenticated()
            )

            // ── Form Login (username + password) ─────────────────────────
            // Vue sends POST /api/auth/login with username & password (form encoded)
            .formLogin(form -> form
                .loginProcessingUrl("/api/auth/login") // Vue posts here
                .successHandler((request, response, authentication) -> {
                    // Return JSON with user info instead of redirecting
                    String role = authentication.getAuthorities().stream()
                            .findFirst()
                            .map(a -> a.getAuthority().replace("ROLE_", ""))
                            .orElse("USER");
                    response.setContentType("application/json");
                    response.setStatus(HttpServletResponse.SC_OK);
                    mapper.writeValue(response.getWriter(),
                            Map.of("email", authentication.getName(), "role", role));
                })
                .failureHandler((request, response, exception) -> {
                    // Return 401 JSON on wrong password
                    response.setContentType("application/json");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    mapper.writeValue(response.getWriter(),
                            Map.of("error", "Invalid username or password"));
                })
                .permitAll()
            )

            // ── Google OAuth2 Login ───────────────────────────────────────
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo.userService(oauth2UserService))
                .successHandler((request, response, authentication) -> {
                    // After Google login, redirect browser to the Vue app page
                    // based on the user's role
                    String role = authentication.getAuthorities().stream()
                            .findFirst()
                            .map(a -> a.getAuthority().replace("ROLE_", ""))
                            .orElse("USER");
                    String redirectUrl = switch (role) {
                        case "ADMIN"   -> "http://localhost:5173/admin";
                        case "WORKER"  -> "http://localhost:5173/worker";
                        case "PENDING" -> "http://localhost:5173/choose-role";
                        default        -> "http://localhost:5173/";
                    };
                    response.sendRedirect(redirectUrl);
                })
            )

            // ── Logout ────────────────────────────────────────────────────
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessHandler((request, response, authentication) -> {
                    // Return JSON on logout instead of redirecting
                    response.setContentType("application/json");
                    response.setStatus(HttpServletResponse.SC_OK);
                    mapper.writeValue(response.getWriter(), Map.of("success", true));
                })
                .permitAll()
            )

            // ── Unauthorized Access Handler ───────────────────────────────
            // When not logged in and trying to access a protected endpoint,
            // return 401 JSON instead of redirecting to a login HTML page
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType("application/json");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    mapper.writeValue(response.getWriter(),
                            Map.of("error", "Please login first"));
                })
            )

            .userDetailsService(userDetailsService)
            .csrf(csrf -> csrf.disable()); // Disabled — Vue uses session cookies, not CSRF tokens

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
