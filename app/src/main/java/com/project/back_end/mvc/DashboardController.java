package com.project.back_end.mvc;

import com.project.back_end.services.Service;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Serves the Thymeleaf dashboards. A page is only rendered when the token in the URL is valid for
 * the role; anyone else is sent back to the role-selection page.
 */
@Controller
public class DashboardController {

    private final Service service;

    public DashboardController(Service service) {
        this.service = service;
    }

    /** {@code GET /adminDashboard/{token}} */
    @GetMapping("/adminDashboard/{token}")
    public String adminDashboard(@PathVariable String token) {
        return isValid(token, "admin") ? "admin/adminDashboard" : "redirect:/";
    }

    /** {@code GET /doctorDashboard/{token}} */
    @GetMapping("/doctorDashboard/{token}")
    public String doctorDashboard(@PathVariable String token) {
        return isValid(token, "doctor") ? "doctor/doctorDashboard" : "redirect:/";
    }

    // The shared service returns an empty body for a valid token and an error message otherwise.
    private boolean isValid(String token, String role) {
        Map<String, String> result = service.validateToken(token, role).getBody();
        return result != null && result.isEmpty();
    }
}
