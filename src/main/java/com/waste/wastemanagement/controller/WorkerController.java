package com.waste.wastemanagement.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.waste.wastemanagement.dto.ComplaintDTO;
import com.waste.wastemanagement.service.ComplaintService;
import com.waste.wastemanagement.service.WorkerLocationService;

@Controller
@RequestMapping("/worker")
public class WorkerController {

    private final ComplaintService service;
    private final WorkerLocationService locationService;

    public WorkerController(ComplaintService service, WorkerLocationService locationService) {
        this.service = service;
        this.locationService = locationService;
    }

    // 🔹 Worker Dashboard (auto-detect worker)
    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {

        String workerName = authentication.getName(); // logged-in worker
        List<ComplaintDTO> complaints = service.getComplaintsByWorker(workerName);

        model.addAttribute("complaints", complaints);
        model.addAttribute("workerName", workerName);

        return "worker-dashboard";
    }

    // 🔹 Update worker location (GPS)
    @PostMapping("/location")
    public ResponseEntity<String> updateLocation(@RequestParam double lat, 
                                                 @RequestParam double lng, 
                                                 Authentication authentication) {
        // Validate authentication
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        
        // Validate coordinates
        if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            return ResponseEntity.badRequest().body("Invalid coordinates");
        }
        
        try {
            String workerName = authentication.getName();
            locationService.saveLocation(workerName, lat, lng);
            return ResponseEntity.ok("Location updated");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to save location: " + e.getMessage());
        }
    }

    // 🔹 Upload proof image
    @PostMapping("/upload-proof")
    public String uploadProof(@RequestParam String complaintId, 
                             @RequestParam(required = false) String image) {
        // For now, just mark as having proof - can be extended with file upload later
        return "redirect:/worker/dashboard";
    }

    // 🔹 Mark complaint as cleaned
    @PostMapping("/clean")
    public String markCleaned(@RequestParam String complaintId) {
        service.markCleaned(complaintId);
        return "redirect:/worker/dashboard";
    }
}
