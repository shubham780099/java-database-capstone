package com.project.back_end.services;

import com.project.back_end.DTO.Login;
import com.project.back_end.models.Admin;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Shared service used by the controllers: token checks, admin/patient login, appointment slot
 * validation and the combined doctor / patient-appointment filters.
 * (The annotation is fully qualified because this class is itself called {@code Service}.)
 */
@org.springframework.stereotype.Service
public class Service {

    private static final Logger log = LoggerFactory.getLogger(Service.class);

    private final TokenService tokenService;
    private final AdminRepository adminRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final DoctorService doctorService;
    private final PatientService patientService;

    public Service(TokenService tokenService,
                   AdminRepository adminRepository,
                   DoctorRepository doctorRepository,
                   PatientRepository patientRepository,
                   DoctorService doctorService,
                   PatientService patientService) {
        this.tokenService = tokenService;
        this.adminRepository = adminRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.doctorService = doctorService;
        this.patientService = patientService;
    }

    // ------------------------------------------------------------------ tokens

    /**
     * Checks a token for a role ("admin", "doctor" or "patient").
     *
     * @return 200 with an <b>empty</b> body when the token is valid, or 401 with a "message"
     *         when it is invalid or expired - callers treat a non-empty body as an error
     */
    public ResponseEntity<Map<String, String>> validateToken(String token, String user) {
        Map<String, String> response = new HashMap<>();
        if (!tokenService.validateToken(token, user)) {
            response.put("message", "Invalid or expired token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        return ResponseEntity.ok(response);
    }

    // ------------------------------------------------------------------ admin

    /** Checks the admin's username and password and returns a JWT ("token") if they match. */
    public ResponseEntity<Map<String, String>> validateAdmin(Admin receivedAdmin) {
        Map<String, String> response = new HashMap<>();
        try {
            Admin admin = adminRepository.findByUsername(receivedAdmin.getUsername());
            if (admin == null || admin.getPassword() == null
                    || !admin.getPassword().equals(receivedAdmin.getPassword())) {
                response.put("message", "Invalid username or password");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            response.put("token", tokenService.generateToken(admin.getUsername()));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Admin login failed", e);
            response.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ------------------------------------------------------------------ doctors

    /**
     * Filters doctors by any combination of name, specialty and time. A filter that is null, blank
     * or the literal text "null" (what the front end sends for an empty box) is ignored; with no
     * filters at all every doctor is returned.
     */
    public Map<String, Object> filterDoctor(String name, String specialty, String time) {
        boolean hasName = isPresent(name);
        boolean hasSpecialty = isPresent(specialty);
        boolean hasTime = isPresent(time);

        if (hasName && hasSpecialty && hasTime) {
            return doctorService.filterDoctorsByNameSpecilityandTime(name, specialty, time);
        }
        if (hasName && hasSpecialty) {
            return doctorService.filterDoctorByNameAndSpecility(name, specialty);
        }
        if (hasName && hasTime) {
            return doctorService.filterDoctorByNameAndTime(name, time);
        }
        if (hasSpecialty && hasTime) {
            return doctorService.filterDoctorByTimeAndSpecility(specialty, time);
        }
        if (hasName) {
            return doctorService.findDoctorByName(name);
        }
        if (hasSpecialty) {
            return doctorService.filterDoctorBySpecility(specialty);
        }
        if (hasTime) {
            return doctorService.filterDoctorsByTime(time);
        }
        Map<String, Object> all = new HashMap<>();
        all.put("doctors", doctorService.getDoctors());
        return all;
    }

    // ------------------------------------------------------------------ appointments

    /**
     * Checks that the requested appointment time is one of the doctor's free slots on that date.
     *
     * @return 1 the time is available, 0 the time is not available, -1 the doctor does not exist
     */
    public int validateAppointment(Appointment appointment) {
        Optional<Doctor> doctor = doctorRepository.findById(appointment.getDoctor().getId());
        if (doctor.isEmpty()) {
            return -1;
        }
        List<String> freeSlots = doctorService.getDoctorAvailability(
                doctor.get().getId(), appointment.getAppointmentTime().toLocalDate());

        LocalTime requested = appointment.getAppointmentTime().toLocalTime().withSecond(0).withNano(0);
        for (String slot : freeSlots) {
            if (requested.equals(DoctorService.parseSlotStart(slot))) {
                return 1;
            }
        }
        return 0;
    }

    // ------------------------------------------------------------------ patients

    /** @return true if no patient with this email or phone exists yet (so sign-up may continue) */
    public boolean validatePatient(Patient patient) {
        return patientRepository.findByEmailOrPhone(patient.getEmail(), patient.getPhone()) == null;
    }

    /** Checks a patient's email and password and returns a JWT ("token") if they match. */
    public ResponseEntity<Map<String, String>> validatePatientLogin(Login login) {
        Map<String, String> response = new HashMap<>();
        try {
            Patient patient = patientRepository.findByEmail(login.getEmail());
            if (patient == null || patient.getPassword() == null
                    || !patient.getPassword().equals(login.getPassword())) {
                response.put("message", "Invalid email or password");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            response.put("token", tokenService.generateToken(patient.getEmail()));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Patient login failed", e);
            response.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Filters the logged-in patient's appointments by condition ("past"/"future") and/or doctor
     * name. Null / "null" / blank values are ignored; with neither filter all appointments are
     * returned.
     */
    public ResponseEntity<Map<String, Object>> filterPatient(String condition, String name, String token) {
        try {
            Patient patient = patientRepository.findByEmail(tokenService.extractEmail(token));
            if (patient == null) {
                Map<String, Object> body = new HashMap<>();
                body.put("message", "Patient not found for this token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
            }

            boolean hasCondition = isPresent(condition);
            boolean hasName = isPresent(name);

            if (hasCondition && hasName) {
                return patientService.filterByDoctorAndCondition(condition, name, patient.getId());
            }
            if (hasCondition) {
                return patientService.filterByCondition(condition, patient.getId());
            }
            if (hasName) {
                return patientService.filterByDoctor(name, patient.getId());
            }
            return patientService.getPatientAppointment(patient.getId(), token);
        } catch (Exception e) {
            log.error("Could not filter patient appointments", e);
            Map<String, Object> body = new HashMap<>();
            body.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }

    // ------------------------------------------------------------------ helpers

    private boolean isPresent(String value) {
        return value != null && !value.isBlank() && !"null".equalsIgnoreCase(value.trim());
    }
}
