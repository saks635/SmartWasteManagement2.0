package com.waste.wastemanagement.repository;

import com.waste.wastemanagement.model.AppUser;
import org.springframework.data.mongodb.repository.MongoRepository;

// INTERFACE
public interface UserRepository extends MongoRepository<AppUser, String> {
    AppUser findByUsername(String username); // implemented by Spring automatically
}
