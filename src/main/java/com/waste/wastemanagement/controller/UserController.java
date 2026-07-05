package com.waste.wastemanagement.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.waste.wastemanagement.dto.ComplaintDTO;
import com.waste.wastemanagement.service.ComplaintService;

@Controller
public class UserController {

    private final ComplaintService service;

    // Google Maps API key (optional)
    @Value("${google.maps.api.key:}")
    private String googleMapsApiKey;

    public UserController(ComplaintService service) {
        this.service = service;
    }

    /* Commented out to prevent conflict with Vue SPA homepage served by SpaController
    // Show complaint form
    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("complaint", new ComplaintDTO());

        // Pass Google Maps API key only if configured
        boolean useGoogleMaps = googleMapsApiKey != null && !googleMapsApiKey.trim().isEmpty();
        model.addAttribute("googleMapsApiKey", googleMapsApiKey);
        model.addAttribute("useGoogleMaps", useGoogleMaps);

        return "complaint-form";
    }
    */

    // Submit complaint
    @PostMapping("/submit")
    public String submit(@ModelAttribute ComplaintDTO complaint,
                         @RequestParam(required = false) String latitude,
                         @RequestParam(required = false) String longitude) {

        // Safely handle location with coordinates
        String location = complaint.getLocation();
        if (location == null) {
            location = ""; // Initialize if null
        }

        // Append coordinates if provided and not already added
        if (latitude != null && longitude != null
                && !latitude.isEmpty() && !longitude.isEmpty()
                && !location.contains("(")) {
            location += " (Lat: " + latitude + ", Lng: " + longitude + ")";
            complaint.setLocation(location);
        }

        // Save complaint via service
        service.saveComplaint(complaint);

        // Redirect to success page after POST (PRG pattern)
        return "redirect:/success";
    }

    // Success page
    @GetMapping("/success")
    public String success() {
        return "success";  // Displays success.html
    }

    // Optional: View all complaint statuses
    @GetMapping("/status")
    public String status(Model model) {
        model.addAttribute("complaints", service.getAllComplaints());
        return "status";  // Displays status.html
    }
}
