package com.project.back_end.controllers;

import com.project.back_end.models.Appointment;
import com.project.back_end.services.AppointmentService;
import com.project.back_end.services.Service;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.HashMap;
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

/** Appointment endpoints: doctors list them, patients book / update / cancel them. */
@RestController
@RequestMapping("/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final Service service;

    public AppointmentController(AppointmentService appointmentService, Service service) {
        this.appointmentService = appointmentService;
        this.service = service;
    }

    /**
     * {@code GET /appointments/{date}/{patientName}/{token}} - doctor's appointments on a date
     * (yyyy-MM-dd). Use the text "null" as patientName for no name filter. Doctor token required.
     */
    @GetMapping("/{date}/{patientName}/{token}")
    public ResponseEntity<Map<String, Object>> getAppointments(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PathVariable String patientName,
            @PathVariable String token) {

        ResponseEntity<Map<String, String>> validation = service.validateToken(token, "doctor");
        if (!validation.getBody().isEmpty()) {
            return ResponseEntity.status(validation.getStatusCode())
                    .body(new HashMap<String, Object>(validation.getBody()));
        }
        return ResponseEntity.ok(appointmentService.getAppointment(patientName, date, token));
    }

    /** {@code POST /appointments/{token}} - a patient books an appointment. */
    @PostMapping("/{token}")
    public ResponseEntity<Map<String, String>> bookAppointment(@RequestBody @Valid Appointment appointment,
                                                               @PathVariable String token) {
        ResponseEntity<Map<String, String>> validation = service.validateToken(token, "patient");
        if (!validation.getBody().isEmpty()) {
            return validation;
        }

        int slot = service.validateAppointment(appointment);
        if (slot == -1) {
            return message(HttpStatus.BAD_REQUEST, "Invalid doctor id");
        }
        if (slot == 0) {
            return message(HttpStatus.CONFLICT, "The selected time slot is not available");
        }

        if (appointmentService.bookAppointment(appointment) == 1) {
            return message(HttpStatus.CREATED, "Appointment booked successfully");
        }
        return message(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to book the appointment");
    }

    /** {@code PUT /appointments/{token}} - a patient changes the time (or doctor) of an appointment. */
    @PutMapping("/{token}")
    public ResponseEntity<Map<String, String>> updateAppointment(@RequestBody @Valid Appointment appointment,
                                                                 @PathVariable String token) {
        ResponseEntity<Map<String, String>> validation = service.validateToken(token, "patient");
        if (!validation.getBody().isEmpty()) {
            return validation;
        }
        return appointmentService.updateAppointment(appointment);
    }

    /** {@code DELETE /appointments/{id}/{token}} - a patient cancels one of their appointments. */
    @DeleteMapping("/{id}/{token}")
    public ResponseEntity<Map<String, String>> cancelAppointment(@PathVariable long id,
                                                                 @PathVariable String token) {
        ResponseEntity<Map<String, String>> validation = service.validateToken(token, "patient");
        if (!validation.getBody().isEmpty()) {
            return validation;
        }
        return appointmentService.cancelAppointment(id, token);
    }

    private ResponseEntity<Map<String, String>> message(HttpStatus status, String text) {
        Map<String, String> body = new HashMap<>();
        body.put("message", text);
        return ResponseEntity.status(status).body(body);
    }
}
