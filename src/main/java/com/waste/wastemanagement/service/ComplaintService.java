package com.waste.wastemanagement.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.waste.wastemanagement.dto.ComplaintDTO;
import com.waste.wastemanagement.model.Complaint;
import com.waste.wastemanagement.repository.ComplaintRepository;

@Service
public class ComplaintService {

    private final ComplaintRepository repository;

    // Constructor injection
    public ComplaintService(ComplaintRepository repository) {
        this.repository = repository;
    }

    // Save new complaint - Clear cache after saving
    @CacheEvict(value = {"complaints", "worker-complaints"}, allEntries = true)
    public ComplaintDTO saveComplaint(ComplaintDTO complaintDTO) {
        Complaint complaint = convertToEntity(complaintDTO);
        complaint.setStatus("NEW"); // default status
        Complaint saved = repository.save(complaint);
        return convertToDTO(saved);
    }

    // Get all complaints - Cache result
    @Cacheable(value = "complaints", key = "'all'")
    public List<ComplaintDTO> getAllComplaints() {
        return repository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Assign worker - Clear cache when assignment changes
    @CacheEvict(value = {"complaints", "worker-complaints"}, allEntries = true)
    public void assignWorker(String id, String worker) {
        Complaint c = repository.findById(id).orElseThrow();
        c.setAssignedWorker(worker);
        c.setStatus("ASSIGNED");
        repository.save(c);
    }

    // Mark as cleaned - Clear cache when status changes
    @CacheEvict(value = {"complaints", "worker-complaints"}, allEntries = true)
    public void markCleaned(String id) {
        Complaint c = repository.findById(id).orElseThrow();
        c.setStatus("CLEANED");
        repository.save(c);
    }

    // Get complaints assigned to a worker - Cache by worker name
    @Cacheable(value = "worker-complaints", key = "#worker")
    public List<ComplaintDTO> getComplaintsByWorker(String worker) {
        return repository.findByAssignedWorker(worker).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private ComplaintDTO convertToDTO(Complaint complaint) {
        ComplaintDTO dto = new ComplaintDTO();
        dto.setId(complaint.getId());
        dto.setUserName(complaint.getUserName());
        dto.setLocation(complaint.getLocation());
        dto.setIssue(complaint.getIssue());
        dto.setStatus(complaint.getStatus());
        dto.setAssignedWorker(complaint.getAssignedWorker());
        return dto;
    }

    private Complaint convertToEntity(ComplaintDTO dto) {
        Complaint complaint = new Complaint();
        complaint.setId(dto.getId());
        complaint.setUserName(dto.getUserName());
        complaint.setLocation(dto.getLocation());
        complaint.setIssue(dto.getIssue());
        complaint.setStatus(dto.getStatus());
        complaint.setAssignedWorker(dto.getAssignedWorker());
        return complaint;
    }
}
