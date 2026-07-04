package com.waste.wastemanagement.controller;

import com.waste.wastemanagement.model.AppUser;
import com.waste.wastemanagement.repository.UserRepository;
import com.waste.wastemanagement.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

// This controller handles all authentication-related REST API calls
// The Vue frontend talks to these endpoints
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final UserService userService;

    public AuthController(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    // GET /api/auth/me
    // Vue calls this on startup to check if a user is logged in and get their role
    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        // Not logged in
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        String email = authentication.getName();
        // Extract role from Spring Security authorities (e.g., ROLE_USER → USER)
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("USER");

        return ResponseEntity.ok(Map.of("email", email, "role", role));
    }

    // POST /api/auth/register
    // Vue register form submits here with JSON body: { username, password, role }
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String role     = body.get("role");

        // Basic validation
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username and password are required"));
        }

        try {
            userService.registerUser(username, password, role != null ? role : "USER");
            return ResponseEntity.ok(Map.of("success", true, "message", "Registered successfully! Please login."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // POST /api/choose-role
    // Called after a new Google OAuth2 user selects their role
    // Body: { "role": "USER" | "WORKER" | "ADMIN" }
    @PostMapping("/choose-role")
    public ResponseEntity<?> chooseRole(@RequestBody Map<String, String> body,
                                         Authentication authentication,
                                         HttpServletRequest request) {
        String role = body.get("role");

        // Only USER, WORKER, ADMIN are valid roles
        if (!List.of("USER", "WORKER", "ADMIN").contains(role)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid role selected"));
        }

        // Only users with PENDING role (new OAuth2 users) can call this
        if (!authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_PENDING"))) {
            return ResponseEntity.status(403).body(Map.of("error", "You already have a role assigned"));
        }

        String email = authentication.getName();

        // Update role in MongoDB
        AppUser user = userRepository.findByUsername(email);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        user.setRole(role);
        userRepository.save(user);

        // Update the live Spring Security session with the new role
        // Without this, the current request still sees PENDING role
        SimpleGrantedAuthority newAuthority = new SimpleGrantedAuthority("ROLE_" + role);
        OAuth2AuthenticationToken newAuth = new OAuth2AuthenticationToken(
                (DefaultOAuth2User) authentication.getPrincipal(),
                Collections.singleton(newAuthority),
                ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId()
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(newAuth);
        SecurityContextHolder.setContext(context);

        // Save updated context to Redis session so it persists
        request.getSession(true).setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                context
        );

        return ResponseEntity.ok(Map.of("email", email, "role", role));
    }
}
