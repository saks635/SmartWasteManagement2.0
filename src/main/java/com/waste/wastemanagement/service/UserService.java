package com.waste.wastemanagement.service;

import com.waste.wastemanagement.model.AppUser;
import com.waste.wastemanagement.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Register user - Clear workers cache when new worker is registered
    @CacheEvict(value = {"workers", "users"}, allEntries = true)
    public AppUser registerUser(String username, String password, String role) {
        // Check if username already exists
        if (userRepository.findByUsername(username) != null) {
            throw new IllegalArgumentException("Username already exists");
        }

        // Validate role
        if (!role.equals("USER") && !role.equals("ADMIN") && !role.equals("WORKER")) {
            throw new IllegalArgumentException("Invalid role. Must be USER, ADMIN, or WORKER");
        }

        // Create and save user
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);

        AppUser saved = userRepository.save(user);
        logger.info("Registered new user: {} with role: {}", username, role);
        return saved;
    }

    // Get all workers - Cache the result
    @Cacheable(value = "workers", key = "'all'")
    public List<AppUser> getAllWorkers() {
        return userRepository.findAll().stream()
                .filter(user -> "WORKER".equals(user.getRole()))
                .collect(Collectors.toList());
    }

    public List<AppUser> getAllUsers() {
        return userRepository.findAll();
    }
}
