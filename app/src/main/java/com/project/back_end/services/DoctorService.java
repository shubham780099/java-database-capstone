package com.project.back_end.services;

import com.project.back_end.DTO.Login;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

/**
 * Business logic for doctors: CRUD, login, availability and the search/filter combinations
 * used by the dashboards.
 */
@Service
public class DoctorService {

    private static final Logger log = LoggerFactory.getLogger(DoctorService.class);
    private static final DateTimeFormatter SLOT_TIME = DateTimeFormatter.ofPattern("H:mm");

    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final TokenService tokenService;

    public DoctorService(DoctorRepository doctorRepository,
                         AppointmentRepository appointmentRepository,
                         TokenService tokenService) {
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
        this.tokenService = tokenService;
    }

    // ------------------------------------------------------------------ availability

    /**
     * Free time slots of a doctor on a date: the doctor's configured slots minus the ones that
     * already have an appointment that day.
     */
    @Transactional(readOnly = true)
    public List<String> getDoctorAvailability(Long doctorId, LocalDate date) {
        Optional<Doctor> doctor = doctorRepository.findById(doctorId);
        if (doctor.isEmpty() || doctor.get().getAvailableTimes() == null) {
            return new ArrayList<>();
        }

        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.atTime(LocalTime.MAX);
        List<Appointment> booked =
                appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(doctorId, dayStart, dayEnd);

        Set<LocalTime> bookedStarts = new HashSet<>();
        for (Appointment appointment : booked) {
            bookedStarts.add(appointment.getAppointmentTime().toLocalTime().withSecond(0).withNano(0));
        }

        return doctor.get().getAvailableTimes().stream()
                .filter(slot -> {
                    LocalTime start = parseSlotStart(slot);
                    return start == null || !bookedStarts.contains(start);
                })
                .collect(Collectors.toList());
    }

    /** Start time of a slot such as "09:00-10:00" (or "9:00 - 10:00"); null if it cannot be parsed. */
    public static LocalTime parseSlotStart(String slot) {
        if (slot == null) {
            return null;
        }
        try {
            return LocalTime.parse(slot.split("-")[0].trim(), SLOT_TIME);
        } catch (Exception e) {
            return null;
        }
    }

    // ------------------------------------------------------------------ create / update / delete

    /** @return 1 saved, -1 a doctor with this email already exists, 0 internal error */
    public int saveDoctor(Doctor doctor) {
        try {
            if (doctorRepository.findByEmail(doctor.getEmail()) != null) {
                return -1;
            }
            doctorRepository.save(doctor);
            return 1;
        } catch (Exception e) {
            log.error("Could not save doctor {}", doctor.getEmail(), e);
            return 0;
        }
    }

    /** @return 1 updated, -1 doctor not found, 0 internal error */
    public int updateDoctor(Doctor doctor) {
        try {
            if (doctor.getId() == null || doctorRepository.findById(doctor.getId()).isEmpty()) {
                return -1;
            }
            doctorRepository.save(doctor);
            return 1;
        } catch (Exception e) {
            log.error("Could not update doctor {}", doctor.getId(), e);
            return 0;
        }
    }

    @Transactional(readOnly = true)
    public List<Doctor> getDoctors() {
        return doctorRepository.findAll();
    }

    /**
     * Deletes a doctor together with all of the doctor's appointments, in one transaction.
     *
     * @return 1 deleted, -1 doctor not found, 0 internal error (nothing is deleted in that case)
     */
    @Transactional
    public int deleteDoctor(long id) {
        try {
            if (!doctorRepository.existsById(id)) {
                return -1;
            }
            appointmentRepository.deleteAllByDoctorId(id);
            doctorRepository.deleteById(id);
            return 1;
        } catch (Exception e) {
            log.error("Could not delete doctor {}", id, e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return 0;
        }
    }

    // ------------------------------------------------------------------ login

    /** Checks a doctor's email and password and, if they match, returns a JWT. */
    public ResponseEntity<Map<String, String>> validateDoctor(Login login) {
        Map<String, String> response = new HashMap<>();
        try {
            Doctor doctor = doctorRepository.findByEmail(login.getEmail());
            if (doctor == null || doctor.getPassword() == null
                    || !doctor.getPassword().equals(login.getPassword())) {
                response.put("message", "Invalid email or password");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            response.put("token", tokenService.generateToken(doctor.getEmail()));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Doctor login failed", e);
            response.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ------------------------------------------------------------------ search and filters

    @Transactional(readOnly = true)
    public Map<String, Object> findDoctorByName(String name) {
        return doctorsResponse(doctorRepository.findByNameLike(name));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> filterDoctorsByNameSpecilityandTime(String name, String specialty, String amOrPm) {
        List<Doctor> doctors = doctorRepository.findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase(name, specialty);
        return doctorsResponse(filterDoctorByTime(doctors, amOrPm));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> filterDoctorByNameAndTime(String name, String amOrPm) {
        return doctorsResponse(filterDoctorByTime(doctorRepository.findByNameLike(name), amOrPm));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> filterDoctorByNameAndSpecility(String name, String specilty) {
        return doctorsResponse(
                doctorRepository.findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase(name, specilty));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> filterDoctorByTimeAndSpecility(String specilty, String amOrPm) {
        return doctorsResponse(filterDoctorByTime(doctorRepository.findBySpecialtyIgnoreCase(specilty), amOrPm));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> filterDoctorBySpecility(String specilty) {
        return doctorsResponse(doctorRepository.findBySpecialtyIgnoreCase(specilty));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> filterDoctorsByTime(String amOrPm) {
        return doctorsResponse(filterDoctorByTime(doctorRepository.findAll(), amOrPm));
    }

    /**
     * Keeps the doctors who have at least one slot that matches. "AM" means the slot starts before
     * noon, "PM" means it starts at or after noon. A concrete slot such as "09:00-10:00" (or just
     * its start time "09:00") matches doctors who offer exactly that slot.
     */
    private List<Doctor> filterDoctorByTime(List<Doctor> doctors, String amOrPm) {
        String wanted = amOrPm == null ? "" : amOrPm.replaceAll("\\s", "");
        return doctors.stream()
                .filter(doctor -> doctor.getAvailableTimes() != null
                        && doctor.getAvailableTimes().stream().anyMatch(slot -> slotMatches(slot, wanted)))
                .collect(Collectors.toList());
    }

    private boolean slotMatches(String slot, String wanted) {
        LocalTime start = parseSlotStart(slot);
        if (start == null) {
            return false;
        }
        if ("AM".equalsIgnoreCase(wanted)) {
            return start.isBefore(LocalTime.NOON);
        }
        if ("PM".equalsIgnoreCase(wanted)) {
            return !start.isBefore(LocalTime.NOON);
        }
        String normalised = slot.replaceAll("\\s", "");
        return normalised.equalsIgnoreCase(wanted) || normalised.startsWith(wanted + "-");
    }

    private Map<String, Object> doctorsResponse(List<Doctor> doctors) {
        Map<String, Object> response = new HashMap<>();
        response.put("doctors", doctors);
        return response;
    }
}
