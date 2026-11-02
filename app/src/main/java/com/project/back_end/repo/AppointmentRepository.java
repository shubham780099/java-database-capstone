package com.project.back_end.repo;

import com.project.back_end.models.Appointment;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    /**
     * Appointments of one doctor inside a time window. The doctor and the doctor's available
     * times are fetched in the same query so nothing has to be lazy-loaded later.
     */
    @Query("SELECT DISTINCT a FROM Appointment a "
            + "LEFT JOIN FETCH a.doctor d "
            + "LEFT JOIN FETCH d.availableTimes "
            + "WHERE d.id = :doctorId AND a.appointmentTime BETWEEN :start AND :end")
    List<Appointment> findByDoctorIdAndAppointmentTimeBetween(@Param("doctorId") Long doctorId,
                                                              @Param("start") LocalDateTime start,
                                                              @Param("end") LocalDateTime end);

    /** Same window, narrowed to patients whose name contains the given text (case-insensitive). */
    @Query("SELECT DISTINCT a FROM Appointment a "
            + "LEFT JOIN FETCH a.doctor d "
            + "LEFT JOIN FETCH a.patient p "
            + "WHERE d.id = :doctorId "
            + "AND LOWER(p.name) LIKE LOWER(CONCAT('%', :patientName, '%')) "
            + "AND a.appointmentTime BETWEEN :start AND :end")
    List<Appointment> findByDoctorIdAndPatient_NameContainingIgnoreCaseAndAppointmentTimeBetween(
            @Param("doctorId") Long doctorId,
            @Param("patientName") String patientName,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /** Removes every appointment of a doctor (used before the doctor is deleted). */
    @Modifying
    @Transactional
    @Query("DELETE FROM Appointment a WHERE a.doctor.id = :doctorId")
    void deleteAllByDoctorId(@Param("doctorId") Long doctorId);

    /** All appointments of a patient. */
    List<Appointment> findByPatientId(Long patientId);

    /** A patient's appointments with a given status (0 = scheduled, 1 = completed), earliest first. */
    List<Appointment> findByPatient_IdAndStatusOrderByAppointmentTimeAsc(Long patientId, int status);

    /** A patient's appointments whose doctor's name contains the text (case-insensitive). */
    @Query("SELECT a FROM Appointment a "
            + "WHERE LOWER(a.doctor.name) LIKE LOWER(CONCAT('%', :doctorName, '%')) "
            + "AND a.patient.id = :patientId")
    List<Appointment> filterByDoctorNameAndPatientId(@Param("doctorName") String doctorName,
                                                     @Param("patientId") Long patientId);

    /** Same as above, additionally restricted to one status. */
    @Query("SELECT a FROM Appointment a "
            + "WHERE LOWER(a.doctor.name) LIKE LOWER(CONCAT('%', :doctorName, '%')) "
            + "AND a.patient.id = :patientId "
            + "AND a.status = :status")
    List<Appointment> filterByDoctorNameAndPatientIdAndStatus(@Param("doctorName") String doctorName,
                                                              @Param("patientId") Long patientId,
                                                              @Param("status") int status);

    /** Sets the status of one appointment. */
    @Modifying
    @Transactional
    @Query("UPDATE Appointment a SET a.status = :status WHERE a.id = :id")
    void updateStatus(@Param("status") int status, @Param("id") long id);
}
