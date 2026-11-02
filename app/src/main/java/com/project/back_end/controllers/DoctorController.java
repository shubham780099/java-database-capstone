package com.project.back_end.controllers;

import com.project.back_end.DTO.Login;
import com.project.back_end.models.Doctor;
import com.project.back_end.services.DoctorService;
import com.project.back_end.services.Service;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Doctor endpoints: list, availability, filter, login, and admin-only add / update / delete. */
@RestController
@RequestMapping("${api.path}doctor")
public class DoctorController {

    private final DoctorService doctorService;
    private final Service service;

    public DoctorController(DoctorService doctorService, Service service) {
        this.doctorService = doctorService;
        this.service = service;
    }

    /**
     * {@code GET /doctor/availability/{user}/{doctorId}/{date}/{token}} - free slots of a doctor on a
     * date (date format yyyy-MM-dd). The token must be valid for the given user role.
     */
    @GetMapping("/availability/{user}/{doctorId}/{date}/{token}")
    public ResponseEntity<Map<String, Object>> getDoctorAvailability(
            @PathVariable String user,
            @PathVariable Long doctorId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PathVariable String token) {

        ResponseEntity<Map<String, String>> validation = service.validateToken(token, user);
        if (!validation.getBody().isEmpty()) {
            return ResponseEntity.status(validation.getStatusCode())
                    .body(new HashMap<String, Object>(validation.getBody()));
        }

        List<String> availability = doctorService.getDoctorAvailability(doctorId, date);
        Map<String, Object> response = new HashMap<>();
        response.put("availability", availability);
        return ResponseEntity.ok(response);
    }

    /** {@code GET /doctor} - every doctor, under the key "doctors". */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getDoctor() {
        Map<String, Object> response = new HashMap<>();
        response.put("doctors", doctorService.getDoctors());
        return ResponseEntity.ok(response);
    }

    /** {@code POST /doctor/{token}} - admin adds a doctor. */
    @PostMapping("/{token}")
    public ResponseEntity<Map<String, String>> saveDoctor(@RequestBody @Valid Doctor doctor,
                                                          @PathVariable String token) {
        ResponseEntity<Map<String, String>> validation = service.validateToken(token, "admin");
        if (!validation.getBody().isEmpty()) {
            return validation;
        }

        int result = doctorService.saveDoctor(doctor);
        if (result == 1) {
            return message(HttpStatus.CREATED, "Doctor added to db");
        }
        if (result == -1) {
            return message(HttpStatus.CONFLICT, "Doctor already exists");
        }
        return message(HttpStatus.INTERNAL_SERVER_ERROR, "Some internal error occurred");
    }

    /** {@code POST /doctor/login} - body {"email": "...", "password": "..."}; returns {"token": "..."}. */
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> doctorLogin(@RequestBody @Valid Login login) {
        return doctorService.validateDoctor(login);
    }

    /** {@code PUT /doctor/{token}} - admin updates a doctor. */
    @PutMapping("/{token}")
    public ResponseEntity<Map<String, String>> updateDoctor(@RequestBody @Valid Doctor doctor,
                                                            @PathVariable String token) {
        ResponseEntity<Map<String, String>> validation = service.validateToken(token, "admin");
        if (!validation.getBody().isEmpty()) {
            return validation;
        }

        int result = doctorService.updateDoctor(doctor);
        if (result == 1) {
            return message(HttpStatus.OK, "Doctor updated");
        }
        if (result == -1) {
            return message(HttpStatus.NOT_FOUND, "Doctor not found");
        }
        return message(HttpStatus.INTERNAL_SERVER_ERROR, "Some internal error occurred");
    }

    /** {@code DELETE /doctor/{id}/{token}} - admin deletes a doctor and the doctor's appointments. */
    @DeleteMapping("/{id}/{token}")
    public ResponseEntity<Map<String, String>> deleteDoctor(@PathVariable long id,
                                                            @PathVariable String token) {
        ResponseEntity<Map<String, String>> validation = service.validateToken(token, "admin");
        if (!validation.getBody().isEmpty()) {
            return validation;
        }

        int result = doctorService.deleteDoctor(id);
        if (result == 1) {
            return message(HttpStatus.OK, "Doctor deleted successfully");
        }
        if (result == -1) {
            return message(HttpStatus.NOT_FOUND, "Doctor not found with id " + id);
        }
        return message(HttpStatus.INTERNAL_SERVER_ERROR, "Some internal error occurred");
    }

    /**
     * {@code GET /doctor/filter/{name}/{time}/{speciality}} - any of the three may be the text
     * "null" to skip that filter. Example: {@code /doctor/filter/null/09:00-10:00/Cardiologist}.
     */
    @GetMapping("/filter/{name}/{time}/{speciality}")
    public ResponseEntity<Map<String, Object>> filter(@PathVariable String name,
                                                      @PathVariable String time,
                                                      @PathVariable String speciality) {
        return ResponseEntity.ok(service.filterDoctor(name, speciality, time));
    }

    private ResponseEntity<Map<String, String>> message(HttpStatus status, String text) {
        Map<String, String> body = new HashMap<>();
        body.put("message", text);
        return ResponseEntity.status(status).body(body);
    }
}
