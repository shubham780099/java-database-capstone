package com.project.back_end.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * A one-hour appointment between a doctor and a patient.
 * Mapped to the {@code appointment} table with foreign keys to doctor and patient.
 */
@Entity
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @NotNull(message = "doctor cannot be null")
    private Doctor doctor;

    @ManyToOne
    @NotNull(message = "patient cannot be null")
    private Patient patient;

    @Future(message = "Appointment time must be in the future")
    private LocalDateTime appointmentTime;

    // 0 = Scheduled, 1 = Completed
    @NotNull(message = "status cannot be null")
    private int status;

    public Appointment() {
    }

    public Appointment(Doctor doctor, Patient patient, LocalDateTime appointmentTime, int status) {
        this.doctor = doctor;
        this.patient = patient;
        this.appointmentTime = appointmentTime;
        this.status = status;
    }

    // ---- derived values (not persisted) ----

    /** End of the appointment: one hour after it starts. */
    @Transient
    public LocalDateTime getEndTime() {
        return appointmentTime == null ? null : appointmentTime.plusHours(1);
    }

    /** Date part of the appointment time. */
    @Transient
    public LocalDate getAppointmentDate() {
        return appointmentTime == null ? null : appointmentTime.toLocalDate();
    }

    /** Time-of-day part of the appointment time. */
    @Transient
    public LocalTime getAppointmentTimeOnly() {
        return appointmentTime == null ? null : appointmentTime.toLocalTime();
    }

    // ---- getters and setters ----

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Doctor getDoctor() {
        return doctor;
    }

    public void setDoctor(Doctor doctor) {
        this.doctor = doctor;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public LocalDateTime getAppointmentTime() {
        return appointmentTime;
    }

    public void setAppointmentTime(LocalDateTime appointmentTime) {
        this.appointmentTime = appointmentTime;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
