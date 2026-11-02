-- stored-procedures.sql
-- Reporting procedures for the Smart Clinic database (`cms`).
--   SOURCE sql/stored-procedures.sql;
-- Safe to run more than once (each procedure is dropped first).

USE cms;

-- 1. Daily appointment report: every appointment on one date, grouped by doctor.
DROP PROCEDURE IF EXISTS GetDailyAppointmentReportByDoctor;

DELIMITER $$

CREATE PROCEDURE GetDailyAppointmentReportByDoctor(
    IN report_date DATE
)
BEGIN
    SELECT
        d.name AS doctor_name,
        a.appointment_time,
        a.status,
        p.name AS patient_name,
        p.phone AS patient_phone
    FROM
        appointment a
    JOIN
        doctor d ON a.doctor_id = d.id
    JOIN
        patient p ON a.patient_id = p.id
    WHERE
        DATE(a.appointment_time) = report_date
    ORDER BY
        d.name, a.appointment_time;
END$$

DELIMITER ;

-- 2. Doctor who saw the most patients in a given month and year.
DROP PROCEDURE IF EXISTS GetDoctorWithMostPatientsByMonth;

DELIMITER $$

CREATE PROCEDURE GetDoctorWithMostPatientsByMonth(
    IN input_month INT,
    IN input_year INT
)
BEGIN
    SELECT
        doctor_id,
        COUNT(patient_id) AS patients_seen
    FROM
        appointment
    WHERE
        MONTH(appointment_time) = input_month
        AND YEAR(appointment_time) = input_year
    GROUP BY
        doctor_id
    ORDER BY
        patients_seen DESC
    LIMIT 1;
END $$

DELIMITER ;

-- 3. Doctor who saw the most patients in a given year.
DROP PROCEDURE IF EXISTS GetDoctorWithMostPatientsByYear;

DELIMITER $$

CREATE PROCEDURE GetDoctorWithMostPatientsByYear(
    IN input_year INT
)
BEGIN
    SELECT
        doctor_id,
        COUNT(patient_id) AS patients_seen
    FROM
        appointment
    WHERE
        YEAR(appointment_time) = input_year
    GROUP BY
        doctor_id
    ORDER BY
        patients_seen DESC
    LIMIT 1;
END $$

DELIMITER ;
