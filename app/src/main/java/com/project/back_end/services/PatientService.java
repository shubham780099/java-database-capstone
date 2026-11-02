package com.project.back_end.services;

import com.project.back_end.DTO.AppointmentDTO;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.PatientRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for patients: sign-up, profile lookup and reading/filtering their appointments.
 * Appointments are always returned as {@link AppointmentDTO}s, never as entities.
 */
@Service
public class PatientService {

    private static final Logger log = LoggerFactory.getLogger(PatientService.class);

    private static final int STATUS_SCHEDULED = 0; // "future"
    private static final int STATUS_COMPLETED = 1; // "past"

    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final TokenService tokenService;

    public PatientService(PatientRepository patientRepository,
                          AppointmentRepository appointmentRepository,
                          TokenService tokenService) {
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.tokenService = tokenService;
    }

    /** @return 1 if the patient was saved, 0 on error */
    public int createPatient(Patient patient) {
        try {
            patientRepository.save(patient);
            return 1;
        } catch (Exception e) {
            log.error("Could not create patient {}", patient.getEmail(), e);
            return 0;
        }
    }

    /**
     * All appointments of a patient. The patient id in the URL must belong to the patient the
     * token was issued to; otherwise the request is rejected.
     */
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getPatientAppointment(Long id, String token) {
        try {
            Patient patient = patientRepository.findByEmail(tokenService.extractEmail(token));
            if (patient == null || !patient.getId().equals(id)) {
                return error(HttpStatus.UNAUTHORIZED, "Unauthorized access to this patient's appointments");
            }
            return appointmentsResponse(appointmentRepository.findByPatientId(id));
        } catch (Exception e) {
            log.error("Could not load appointments for patient {}", id, e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }

    /**
     * Appointments between one patient and the doctor the token belongs to. Used when a doctor
     * opens a patient's record, so a doctor never sees other doctors' appointments.
     */
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getPatientAppointmentsForDoctor(Long patientId, String token) {
        try {
            String doctorEmail = tokenService.extractEmail(token);
            List<Appointment> mine = appointmentRepository.findByPatientId(patientId).stream()
                    .filter(a -> a.getDoctor() != null && doctorEmail.equalsIgnoreCase(a.getDoctor().getEmail()))
                    .collect(Collectors.toList());
            return appointmentsResponse(mine);
        } catch (Exception e) {
            log.error("Could not load appointments of patient {} for doctor", patientId, e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }

    /** Filters a patient's appointments by condition: "past" (completed) or "future" (scheduled). */
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> filterByCondition(String condition, Long id) {
        Integer status = statusFor(condition);
        if (status == null) {
            return error(HttpStatus.BAD_REQUEST, "Invalid condition. Use 'past' or 'future'.");
        }
        try {
            return appointmentsResponse(
                    appointmentRepository.findByPatient_IdAndStatusOrderByAppointmentTimeAsc(id, status));
        } catch (Exception e) {
            log.error("Could not filter appointments by condition", e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }

    /** Filters a patient's appointments by (part of) the doctor's name. */
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> filterByDoctor(String name, Long patientId) {
        try {
            return appointmentsResponse(appointmentRepository.filterByDoctorNameAndPatientId(name, patientId));
        } catch (Exception e) {
            log.error("Could not filter appointments by doctor", e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }

    /** Filters by doctor name and condition ("past" / "future") together. */
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> filterByDoctorAndCondition(String condition, String name,
                                                                          long patientId) {
        Integer status = statusFor(condition);
        if (status == null) {
            return error(HttpStatus.BAD_REQUEST, "Invalid condition. Use 'past' or 'future'.");
        }
        try {
            return appointmentsResponse(
                    appointmentRepository.filterByDoctorNameAndPatientIdAndStatus(name, patientId, status));
        } catch (Exception e) {
            log.error("Could not filter appointments by doctor and condition", e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }

    /** Profile of the patient the token was issued to. */
    public ResponseEntity<Map<String, Object>> getPatientDetails(String token) {
        try {
            Patient patient = patientRepository.findByEmail(tokenService.extractEmail(token));
            if (patient == null) {
                return error(HttpStatus.NOT_FOUND, "Patient not found");
            }
            Map<String, Object> response = new HashMap<>();
            response.put("patient", patient);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Could not load patient details", e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }

    // ------------------------------------------------------------------ helpers

    private Integer statusFor(String condition) {
        if ("past".equalsIgnoreCase(condition)) {
            return STATUS_COMPLETED;
        }
        if ("future".equalsIgnoreCase(condition)) {
            return STATUS_SCHEDULED;
        }
        return null;
    }

    private ResponseEntity<Map<String, Object>> appointmentsResponse(List<Appointment> appointments) {
        List<AppointmentDTO> dtos = appointments.stream().map(this::toDTO).collect(Collectors.toList());
        Map<String, Object> response = new HashMap<>();
        response.put("appointments", dtos);
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", message);
        return ResponseEntity.status(status).body(response);
    }

    private AppointmentDTO toDTO(Appointment a) {
        return new AppointmentDTO(
                a.getId(),
                a.getDoctor().getId(),
                a.getDoctor().getName(),
                a.getPatient().getId(),
                a.getPatient().getName(),
                a.getPatient().getEmail(),
                a.getPatient().getPhone(),
                a.getPatient().getAddress(),
                a.getAppointmentTime(),
                a.getStatus());
    }
}
