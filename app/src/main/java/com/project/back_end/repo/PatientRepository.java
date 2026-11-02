package com.project.back_end.repo;

import com.project.back_end.models.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    /** Returns the patient with this email, or null. */
    Patient findByEmail(String email);

    /** Returns a patient matching either the email or the phone number, or null. */
    Patient findByEmailOrPhone(String email, String phone);
}
