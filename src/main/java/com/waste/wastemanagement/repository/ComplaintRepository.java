package com.waste.wastemanagement.repository;

import com.waste.wastemanagement.model.Complaint;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

// This is an INTERFACE
public interface ComplaintRepository extends MongoRepository<Complaint, String> {
    List<Complaint> findByAssignedWorker(String worker); // auto implemented by Spring
}
