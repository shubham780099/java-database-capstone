package com.project.back_end.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * A doctor who can log in, publish available time slots and receive appointments.
 * Mapped to the {@code doctor} table; the time slots live in {@code doctor_available_times}.
 */
@Entity
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "name cannot be null")
    @Size(min = 3, max = 100, message = "name must be between 3 and 100 characters")
    private String name;

    @NotNull(message = "specialty cannot be null")
    @Size(min = 3, max = 50, message = "specialty must be between 3 and 50 characters")
    private String specialty;

    @NotNull(message = "email cannot be null")
    @Email(message = "email must be a valid email address")
    private String email;

    // Accepted when a doctor is created or logs in, but hidden from every JSON response.
    @NotNull(message = "password cannot be null")
    @Size(min = 6, message = "password must be at least 6 characters")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @NotNull(message = "phone cannot be null")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be 10 digits")
    private String phone;

    // Slots such as "09:00-10:00". Stored in a separate collection table.
    // EAGER keeps the slots available when doctors are serialised outside a transaction
    // (spring.jpa.open-in-view is false in this project).
    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> availableTimes;

    public Doctor() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String specialty) {
        this.specialty = specialty;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public List<String> getAvailableTimes() {
        return availableTimes;
    }

    public void setAvailableTimes(List<String> availableTimes) {
        this.availableTimes = availableTimes;
    }
}
