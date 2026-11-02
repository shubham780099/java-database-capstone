package com.project.back_end.repo;

import com.project.back_end.models.Doctor;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    /** Returns the doctor with this email, or null. */
    Doctor findByEmail(String email);

    /** Partial name match: the name contains the search text. */
    @Query("SELECT d FROM Doctor d WHERE d.name LIKE CONCAT('%', :name, '%')")
    List<Doctor> findByNameLike(@Param("name") String name);

    /** Partial, case-insensitive name match combined with an exact (case-insensitive) specialty. */
    @Query("SELECT d FROM Doctor d "
            + "WHERE LOWER(d.name) LIKE LOWER(CONCAT('%', :name, '%')) "
            + "AND LOWER(d.specialty) = LOWER(:specialty)")
    List<Doctor> findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase(@Param("name") String name,
                                                                      @Param("specialty") String specialty);

    /** All doctors of a specialty, ignoring case. */
    List<Doctor> findBySpecialtyIgnoreCase(String specialty);
}
