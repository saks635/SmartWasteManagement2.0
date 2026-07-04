package com.waste.wastemanagement.service;

import com.waste.wastemanagement.model.AppUser;
import com.waste.wastemanagement.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger logger = LoggerFactory.getLogger(CustomOAuth2UserService.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomOAuth2UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @CacheEvict(value = {"users"}, allEntries = true)
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        
        if (email == null || email.isEmpty()) {
            throw new OAuth2AuthenticationException("Email not found in OAuth2 user attributes");
        }

        // Check if user exists, if not create new user with default role USER
        AppUser user = userRepository.findByUsername(email);
        if (user == null) {
            user = new AppUser();
            user.setUsername(email);
            // For OAuth users, generate a random password (they won't use it)
            user.setPassword(passwordEncoder.encode("OAUTH2_USER_" + System.currentTimeMillis()));
            user.setRole("PENDING"); // Redirect new OAuth2 users to role selection page
            userRepository.save(user);
            logger.info("Auto-registered new OAuth2 user: {} with name: {}", email, name);
        } else {
            logger.info("OAuth2 login for existing user: {}", email);
        }

        // Build authorities based on user role
        String role = user.getRole();
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);

        // Return OAuth2User with custom attributes and authorities
        return new DefaultOAuth2User(
                Collections.singleton(authority),
                oauth2User.getAttributes(),
                "email" // Use email as the name attribute key
        );
    }
}
