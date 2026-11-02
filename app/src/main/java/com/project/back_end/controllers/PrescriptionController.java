package com.project.back_end.controllers;

import com.project.back_end.models.Prescription;
import com.project.back_end.services.AppointmentService;
import com.project.back_end.services.PrescriptionService;
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

/** Prescription endpoints, available to logged-in doctors only. */
@RestController
@RequestMapping("${api.path}prescription")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;
    private final Service service;
    private final AppointmentService appointmentService;

    public PrescriptionController(PrescriptionService prescriptionService,
                                  Service service,
                                  AppointmentService appointmentService) {
        this.prescriptionService = prescriptionService;
        this.service = service;
        this.appointmentService = appointmentService;
    }

    /**
     * {@code POST /prescription/{token}} - a doctor saves a prescription. When it is saved, the
     * appointment is marked as completed (status 1).
     */
    @PostMapping("/{token}")
    public ResponseEntity<Map<String, String>> savePrescription(@RequestBody @Valid Prescription prescription,
                                                                @PathVariable String token) {
        ResponseEntity<Map<String, String>> validation = service.validateToken(token, "doctor");
        if (!validation.getBody().isEmpty()) {
            return validation;
        }

        ResponseEntity<Map<String, String>> result = prescriptionService.savePrescription(prescription);
        if (result.getStatusCode() == HttpStatus.CREATED) {
            appointmentService.changeStatus(prescription.getAppointmentId(), 1);
        }
        return result;
    }

    /** {@code GET /prescription/{appointmentId}/{token}} - the prescription(s) of an appointment. */
    @GetMapping("/{appointmentId}/{token}")
    public ResponseEntity<Map<String, Object>> getPrescription(@PathVariable Long appointmentId,
                                                               @PathVariable String token) {
        ResponseEntity<Map<String, String>> validation = service.validateToken(token, "doctor");
        if (!validation.getBody().isEmpty()) {
            return ResponseEntity.status(validation.getStatusCode())
                    .body(new HashMap<String, Object>(validation.getBody()));
        }
        return prescriptionService.getPrescription(appointmentId);
    }
}
