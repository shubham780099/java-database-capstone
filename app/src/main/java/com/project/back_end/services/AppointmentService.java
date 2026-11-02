package com.project.back_end.services;

import com.project.back_end.DTO.AppointmentDTO;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for appointments: booking, updating, cancelling and listing them.
 * (The Spring annotation is written fully qualified because this package also has a class called
 * {@code Service}.)
 */
@org.springframework.stereotype.Service
public class AppointmentService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentService.class);

    private final AppointmentRepository appointmentRepository;
    private final Service service;
    private final TokenService tokenService;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              Service service,
                              TokenService tokenService,
                              PatientRepository patientRepository,
                              DoctorRepository doctorRepository) {
        this.appointmentRepository = appointmentRepository;
        this.service = service;
        this.tokenService = tokenService;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
    }

    /** Saves a new appointment. @return 1 on success, 0 on error */
    public int bookAppointment(Appointment appointment) {
        try {
            appointmentRepository.save(appointment);
            return 1;
        } catch (Exception e) {
            log.error("Could not book appointment", e);
            return 0;
        }
    }

    /**
     * Moves an existing appointment to another doctor/time. The patient must be the one who booked
     * it, and the new slot must be free (re-saving the same slot is allowed).
     */
    public ResponseEntity<Map<String, String>> updateAppointment(Appointment appointment) {
        try {
            if (appointment.getId() == null) {
                return message(HttpStatus.BAD_REQUEST, "Appointment id is required");
            }
            Optional<Appointment> found = appointmentRepository.findById(appointment.getId());
            if (found.isEmpty()) {
                return message(HttpStatus.NOT_FOUND, "Appointment not found");
            }
            Appointment existing = found.get();

            if (!existing.getPatient().getId().equals(appointment.getPatient().getId())) {
                return message(HttpStatus.FORBIDDEN, "You can only update your own appointments");
            }

            boolean sameSlot = existing.getDoctor().getId().equals(appointment.getDoctor().getId())
                    && existing.getAppointmentTime().equals(appointment.getAppointmentTime());
            if (!sameSlot) {
                int check = service.validateAppointment(appointment);
                if (check == -1) {
                    return message(HttpStatus.BAD_REQUEST, "Invalid doctor id");
                }
                if (check == 0) {
                    return message(HttpStatus.CONFLICT, "Appointment time is not available");
                }
            }

            Optional<Doctor> doctor = doctorRepository.findById(appointment.getDoctor().getId());
            if (doctor.isEmpty()) {
                return message(HttpStatus.BAD_REQUEST, "Invalid doctor id");
            }
            existing.setDoctor(doctor.get());
            existing.setAppointmentTime(appointment.getAppointmentTime());
            appointmentRepository.save(existing);
            return message(HttpStatus.OK, "Appointment updated successfully");
        } catch (Exception e) {
            log.error("Could not update appointment", e);
            return message(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }

    /** Cancels (deletes) an appointment, but only for the patient who booked it. */
    public ResponseEntity<Map<String, String>> cancelAppointment(long id, String token) {
        try {
            Optional<Appointment> found = appointmentRepository.findById(id);
            if (found.isEmpty()) {
                return message(HttpStatus.NOT_FOUND, "Appointment not found");
            }
            Patient patient = patientRepository.findByEmail(tokenService.extractEmail(token));
            if (patient == null || !found.get().getPatient().getId().equals(patient.getId())) {
                return message(HttpStatus.FORBIDDEN, "You can only cancel your own appointments");
            }
            appointmentRepository.delete(found.get());
            return message(HttpStatus.OK, "Appointment cancelled successfully");
        } catch (Exception e) {
            log.error("Could not cancel appointment {}", id, e);
            return message(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }

    /**
     * Appointments of the doctor identified by the token on one date, optionally narrowed to
     * patients whose name contains {@code pname} (null / "null" / blank means no name filter).
     * The result is returned under the key "appointments" as {@link AppointmentDTO}s.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getAppointment(String pname, LocalDate date, String token) {
        Map<String, Object> result = new HashMap<>();
        Doctor doctor = doctorRepository.findByEmail(tokenService.extractEmail(token));
        if (doctor == null) {
            result.put("appointments", List.of());
            return result;
        }

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);
        boolean hasName = pname != null && !pname.isBlank() && !"null".equalsIgnoreCase(pname.trim());

        List<Appointment> appointments = hasName
                ? appointmentRepository.findByDoctorIdAndPatient_NameContainingIgnoreCaseAndAppointmentTimeBetween(
                        doctor.getId(), pname.trim(), start, end)
                : appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(doctor.getId(), start, end);

        result.put("appointments", appointments.stream().map(this::toDTO).collect(Collectors.toList()));
        return result;
    }

    /** Sets the status of an appointment (0 = scheduled, 1 = completed). */
    @Transactional
    public void changeStatus(long id, int status) {
        appointmentRepository.updateStatus(status, id);
    }

    // ------------------------------------------------------------------ helpers

    private ResponseEntity<Map<String, String>> message(HttpStatus status, String text) {
        Map<String, String> body = new HashMap<>();
        body.put("message", text);
        return ResponseEntity.status(status).body(body);
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
