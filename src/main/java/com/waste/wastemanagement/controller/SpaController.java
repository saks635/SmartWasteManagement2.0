package com.waste.wastemanagement.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * SPA Fallback Controller
 *
 * In production, the Vue app is built into src/main/resources/static/.
 * When a user refreshes the browser on a deep Vue route (e.g. /admin, /worker),
 * Spring Boot receives the request and must return index.html — not a 404 or
 * a Thymeleaf template — so Vue Router can take over client-side.
 *
 * This controller catches all GET requests that:
 *   - Are NOT under /api/** (REST API)
 *   - Are NOT static assets (handled by Spring's ResourceHttpRequestHandler)
 *   - Are NOT old Thymeleaf routes (/login, /register, /admin/dashboard, etc.)
 *
 * In development mode (Vite on :5173 + Spring Boot on :8080), this controller
 * is harmless — the browser talks to Vite, not Spring Boot for HTML pages.
 */
@Controller
public class SpaController {

    /**
     * Catch-all for Vue Router paths:
     * /              → index.html  (CitizenView)
     * /admin         → index.html  (AdminView)
     * /worker        → index.html  (WorkerView)
     * /choose-role   → index.html  (ChooseRoleView)
     * /login         → index.html  (LoginView) — but /login is also a Thymeleaf route (handled by LoginController first)
     *
     * Spring MVC always prefers more specific mappings, so this only fires
     * when no other @GetMapping or static resource matches.
     */
    @RequestMapping(value = {
        "/",
        "/login",
        "/register",
        "/admin",
        "/worker",
        "/choose-role",
        "/admin/worker/{username}"
    })
    public String spa() {
        // Serve the Vue entry point — Spring Boot finds this in static/index.html
        return "forward:/index.html";
    }
}
