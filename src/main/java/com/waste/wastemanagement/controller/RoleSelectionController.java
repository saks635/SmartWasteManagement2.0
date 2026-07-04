package com.waste.wastemanagement.controller;

import com.waste.wastemanagement.model.AppUser;
import com.waste.wastemanagement.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.List;

@Controller
public class RoleSelectionController {

    private final UserRepository userRepository;

    public RoleSelectionController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Show role selection page (only for PENDING users)
    @GetMapping("/choose-role")
    public String showChooseRole(Authentication authentication, Model model) {
        // If user already has a real role, redirect them to the right page
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        if (!authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_PENDING"))) {
            return "redirect:/"; // Already has a role
        }

        // Pass the user's name/email to the page for personalization
        String name = authentication.getName();
        model.addAttribute("userEmail", name);
        return "choose-role";
    }

    // Handle role selection form submission
    @PostMapping("/choose-role")
    public String selectRole(@RequestParam String role,
                             Authentication authentication,
                             HttpServletRequest request,
                             HttpServletResponse response) {

        // Validate submitted role
        List<String> validRoles = List.of("USER", "WORKER", "ADMIN");
        if (!validRoles.contains(role)) {
            return "redirect:/choose-role?error=invalid";
        }

        // Only PENDING users can select a role
        if (!authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_PENDING"))) {
            return "redirect:/";
        }

        String email = authentication.getName();

        // Update role in MongoDB
        AppUser user = userRepository.findByUsername(email);
        if (user == null) {
            return "redirect:/choose-role?error=notfound";
        }
        user.setRole(role);
        userRepository.save(user);

        // Update the SecurityContext so the current session reflects the new role
        SimpleGrantedAuthority newAuthority = new SimpleGrantedAuthority("ROLE_" + role);
        OAuth2AuthenticationToken newAuth = new OAuth2AuthenticationToken(
                (DefaultOAuth2User) authentication.getPrincipal(),
                Collections.singleton(newAuthority),
                ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId()
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(newAuth);
        SecurityContextHolder.setContext(context);

        // Persist the updated security context to the Redis-backed session
        request.getSession(true).setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                context
        );

        // Redirect based on chosen role
        return switch (role) {
            case "ADMIN"  -> "redirect:/admin/dashboard";
            case "WORKER" -> "redirect:/worker/dashboard";
            default       -> "redirect:/";
        };
    }
}
