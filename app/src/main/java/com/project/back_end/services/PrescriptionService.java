package com.project.back_end.services;

import com.project.back_end.models.Prescription;
import com.project.back_end.repo.PrescriptionRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/** Saves and retrieves prescriptions (stored in MongoDB). */
@Service
public class PrescriptionService {

    private static final Logger log = LoggerFactory.getLogger(PrescriptionService.class);

    private final PrescriptionRepository prescriptionRepository;

    public PrescriptionService(PrescriptionRepository prescriptionRepository) {
        this.prescriptionRepository = prescriptionRepository;
    }

    /**
     * Saves a prescription unless one already exists for the same appointment.
     *
     * @return 201 Created on success, 400 if the appointment already has a prescription,
     *         500 on an internal error
     */
    public ResponseEntity<Map<String, String>> savePrescription(Prescription prescription) {
        Map<String, String> response = new HashMap<>();
        try {
            List<Prescription> existing = prescriptionRepository.findByAppointmentId(prescription.getAppointmentId());
            if (!existing.isEmpty()) {
                response.put("message", "Prescription already exists for this appointment");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            prescriptionRepository.save(prescription);
            response.put("message", "Prescription saved");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Could not save prescription for appointment {}", prescription.getAppointmentId(), e);
            response.put("message", "Some internal error occurred while saving the prescription");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Prescriptions for an appointment, returned under the key "prescription" (an empty list when
     * nothing has been issued yet).
     */
    public ResponseEntity<Map<String, Object>> getPrescription(Long appointmentId) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Prescription> prescriptions = prescriptionRepository.findByAppointmentId(appointmentId);
            response.put("prescription", prescriptions);
            if (prescriptions.isEmpty()) {
                response.put("message", "No prescription found for this appointment");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Could not load prescription for appointment {}", appointmentId, e);
            response.put("message", "Some internal error occurred while fetching the prescription");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
