package com.project.back_end.controllers;

import com.project.back_end.DTO.Login;
import com.project.back_end.models.Patient;
import com.project.back_end.services.PatientService;
import com.project.back_end.services.Service;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Patient endpoints: sign-up, login, profile, and reading / filtering appointments. */
@RestController
@RequestMapping("/patient")
public class PatientController {

    private final PatientService patientService;
    private final Service service;

    public PatientController(PatientService patientService, Service service) {
        this.patientService = patientService;
        this.service = service;
    }

    /** {@code GET /patient/{token}} - details of the logged-in patient, under the key "patient". */
    @GetMapping("/{token}")
    public ResponseEntity<Map<String, Object>> getPatient(@PathVariable String token) {
        ResponseEntity<Map<String, String>> validation = service.validateToken(token, "patient");
        if (!validation.getBody().isEmpty()) {
            return ResponseEntity.status(validation.getStatusCode())
                    .body(new HashMap<String, Object>(validation.getBody()));
        }
        return patientService.getPatientDetails(token);
    }

    /** {@code POST /patient} - sign-up. */
    @PostMapping
    public ResponseEntity<Map<String, String>> createPatient(@RequestBody @Valid Patient patient) {
        if (!service.validatePatient(patient)) {
            return message(HttpStatus.CONFLICT, "Patient with email id or phone no already exist");
        }
        if (patientService.createPatient(patient) == 1) {
            return message(HttpStatus.CREATED, "Signup successful");
        }
        return message(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
    }

    /** {@code POST /patient/login} - body {"email": "...", "password": "..."}; returns {"token": "..."}. */
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Login login) {
        return service.validatePatientLogin(login);
    }

    /**
     * {@code GET /patient/{id}/{user}/{token}} - a patient's appointments, under the key
     * "appointments". {@code user} is the role of the caller: "patient" (reading their own
     * appointments) or "doctor" (opening a patient's record; only that doctor's appointments are
     * returned).
     */
    @GetMapping("/{id}/{user}/{token}")
    public ResponseEntity<Map<String, Object>> getPatientAppointment(@PathVariable Long id,
                                                                     @PathVariable String user,
                                                                     @PathVariable String token) {
        ResponseEntity<Map<String, String>> validation = service.validateToken(token, user);
        if (!validation.getBody().isEmpty()) {
            return ResponseEntity.status(validation.getStatusCode())
                    .body(new HashMap<String, Object>(validation.getBody()));
        }
        if ("doctor".equalsIgnoreCase(user)) {
            return patientService.getPatientAppointmentsForDoctor(id, token);
        }
        return patientService.getPatientAppointment(id, token);
    }

    /**
     * {@code GET /patient/filter/{condition}/{name}/{token}} - filter the logged-in patient's
     * appointments. condition is "past" or "future", name is part of a doctor's name; either may be
     * the text "null" to skip that filter.
     */
    @GetMapping("/filter/{condition}/{name}/{token}")
    public ResponseEntity<Map<String, Object>> filterPatientAppointment(@PathVariable String condition,
                                                                        @PathVariable String name,
                                                                        @PathVariable String token) {
        ResponseEntity<Map<String, String>> validation = service.validateToken(token, "patient");
        if (!validation.getBody().isEmpty()) {
            return ResponseEntity.status(validation.getStatusCode())
                    .body(new HashMap<String, Object>(validation.getBody()));
        }
        return service.filterPatient(condition, name, token);
    }

    private ResponseEntity<Map<String, String>> message(HttpStatus status, String text) {
        Map<String, String> body = new HashMap<>();
        body.put("message", text);
        return ResponseEntity.status(status).body(body);
    }
}
