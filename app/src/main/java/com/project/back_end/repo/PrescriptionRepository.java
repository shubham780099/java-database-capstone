package com.project.back_end.repo;

import com.project.back_end.models.Prescription;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PrescriptionRepository extends MongoRepository<Prescription, String> {

    /** Prescriptions issued for one appointment (the query is derived from the method name). */
    List<Prescription> findByAppointmentId(Long appointmentId);
}
