package com.waste.wastemanagement.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.waste.wastemanagement.dto.ComplaintDTO;
import com.waste.wastemanagement.model.AppUser;
import com.waste.wastemanagement.service.ComplaintService;
import com.waste.wastemanagement.service.UserService;
import com.waste.wastemanagement.service.WorkerLocationService;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final ComplaintService complaintService;
    private final WorkerLocationService locationService;
    private final UserService userService;
    
    @Value("${google.maps.api.key:}")
    private String googleMapsApiKey;

    public AdminController(ComplaintService complaintService,
                           WorkerLocationService locationService,
                           UserService userService) {
        this.complaintService = complaintService;
        this.locationService = locationService;
        this.userService = userService;
    }

    // 🔹 Admin Dashboard – view all complaints
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        List<ComplaintDTO> complaints = complaintService.getAllComplaints();
        List<AppUser> workers = userService.getAllWorkers();
        model.addAttribute("complaints", complaints);
        model.addAttribute("workers", workers);
        return "admin-dashboard";
    }

    // 🔹 Assign worker to complaint
    @PostMapping("/assign")
    public String assignWorker(@RequestParam String complaintId,
                               @RequestParam String workerName) {
        complaintService.assignWorker(complaintId, workerName);
        return "redirect:/admin/dashboard";
    }

    // 🔹 View worker GPS tracking on map
    @GetMapping("/worker/{username}")
    public String viewWorker(@PathVariable String username, Model model) {
        model.addAttribute("locations", locationService.getLastLocations(username));
        model.addAttribute("workerName", username);
        // Only pass API key if it's configured (not empty)
        if (googleMapsApiKey != null && !googleMapsApiKey.trim().isEmpty()) {
            model.addAttribute("googleMapsApiKey", googleMapsApiKey);
            model.addAttribute("useGoogleMaps", true);
        } else {
            model.addAttribute("useGoogleMaps", false);
        }
        return "admin-worker-map";
    }
}
