package com.waste.wastemanagement.controller;

import com.waste.wastemanagement.dto.ComplaintDTO;
import com.waste.wastemanagement.model.AppUser;
import com.waste.wastemanagement.model.WorkerLocation;
import com.waste.wastemanagement.repository.WorkerLocationRepository;
import com.waste.wastemanagement.service.ComplaintService;
import com.waste.wastemanagement.service.UserService;
import com.waste.wastemanagement.service.WorkerLocationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// This is the main data API controller that the Vue frontend uses
// All endpoints are under /api to clearly separate them from the old Thymeleaf routes
@RestController
@RequestMapping("/api")
public class ApiController {

    private final ComplaintService complaintService;
    private final UserService userService;
    private final WorkerLocationService locationService;
    private final WorkerLocationRepository locationRepository;

    public ApiController(ComplaintService complaintService,
                         UserService userService,
                         WorkerLocationService locationService,
                         WorkerLocationRepository locationRepository) {
        this.complaintService = complaintService;
        this.userService = userService;
        this.locationService = locationService;
        this.locationRepository = locationRepository;
    }

    // ─────────────────────────────────────────────────────────
    // CITIZEN (USER role)
    // ─────────────────────────────────────────────────────────

    // POST /api/complaints
    // Citizen submits a waste complaint
    // Body: { userName, location, issue, latitude, longitude }
    @PostMapping("/complaints")
    public ResponseEntity<?> submitComplaint(@RequestBody Map<String, Object> body) {
        ComplaintDTO dto = new ComplaintDTO();
        dto.setUserName((String) body.get("userName"));
        dto.setIssue((String) body.get("issue"));

        // Build location string with optional coordinates
        String location = (String) body.get("location");
        Object lat = body.get("latitude");
        Object lng = body.get("longitude");
        if (lat != null && lng != null && !lat.toString().isBlank()) {
            location += " (Lat: " + lat + ", Lng: " + lng + ")";
        }
        dto.setLocation(location);

        complaintService.saveComplaint(dto);
        return ResponseEntity.ok(Map.of("success", true, "message", "Complaint submitted successfully!"));
    }

    // ─────────────────────────────────────────────────────────
    // ADMIN role
    // ─────────────────────────────────────────────────────────

    // GET /api/admin/complaints
    // Returns all complaints in the system
    @GetMapping("/admin/complaints")
    public ResponseEntity<List<ComplaintDTO>> getAllComplaints() {
        return ResponseEntity.ok(complaintService.getAllComplaints());
    }

    // GET /api/admin/workers
    // Returns all users with WORKER role
    @GetMapping("/admin/workers")
    public ResponseEntity<List<AppUser>> getAllWorkers() {
        return ResponseEntity.ok(userService.getAllWorkers());
    }

    // POST /api/admin/assign
    // Assigns a worker to a complaint
    // Body: { complaintId, workerName }
    @PostMapping("/admin/assign")
    public ResponseEntity<?> assignWorker(@RequestBody Map<String, String> body) {
        complaintService.assignWorker(body.get("complaintId"), body.get("workerName"));
        return ResponseEntity.ok(Map.of("success", true));
    }

    // GET /api/admin/worker/{username}/locations
    // Returns location history for a specific worker (shown on the GPS map page)
    @GetMapping("/admin/worker/{username}/locations")
    public ResponseEntity<List<WorkerLocation>> getWorkerLocations(@PathVariable String username) {
        List<WorkerLocation> locations = locationRepository
                .findTop10ByWorkerUsernameOrderByTimestampDesc(username);
        return ResponseEntity.ok(locations);
    }

    // ─────────────────────────────────────────────────────────
    // WORKER role
    // ─────────────────────────────────────────────────────────

    // GET /api/worker/complaints
    // Returns complaints assigned to the currently logged-in worker
    @GetMapping("/worker/complaints")
    public ResponseEntity<List<ComplaintDTO>> getMyComplaints(Authentication authentication) {
        String workerName = authentication.getName();
        return ResponseEntity.ok(complaintService.getComplaintsByWorker(workerName));
    }

    // POST /api/worker/clean
    // Worker marks a complaint as cleaned
    // Body: { complaintId }
    @PostMapping("/worker/clean")
    public ResponseEntity<?> markCleaned(@RequestBody Map<String, String> body) {
        complaintService.markCleaned(body.get("complaintId"));
        return ResponseEntity.ok(Map.of("success", true));
    }

    // POST /api/worker/location
    // Worker's browser sends GPS coordinates every 30 seconds
    // Body: { lat, lng }
    @PostMapping("/worker/location")
    public ResponseEntity<?> updateLocation(@RequestBody Map<String, Double> body,
                                             Authentication authentication) {
        Double lat = body.get("lat");
        Double lng = body.get("lng");
        if (lat == null || lng == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "lat and lng are required"));
        }
        locationService.saveLocation(authentication.getName(), lat, lng);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
