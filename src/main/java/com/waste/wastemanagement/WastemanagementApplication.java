package com.waste.wastemanagement;

import com.waste.wastemanagement.model.AppUser;
import com.waste.wastemanagement.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class WastemanagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(WastemanagementApplication.class, args);
    }

    // Add some dummy users for testing security
    @Bean
    CommandLineRunner initUsers(UserRepository userRepository, PasswordEncoder encoder) {
        return args -> {
            if (userRepository.findAll().isEmpty()) {
                userRepository.save(new AppUser(null, "admin", encoder.encode("admin123"), "ADMIN"));
                userRepository.save(new AppUser(null, "worker1", encoder.encode("worker123"), "WORKER"));
                userRepository.save(new AppUser(null, "user1", encoder.encode("user123"), "USER"));
            }
        };
    }
}
