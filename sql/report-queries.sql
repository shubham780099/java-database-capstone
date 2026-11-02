-- report-queries.sql
-- The statements whose output the capstone asks you to submit (Q19-Q23).
-- Run them one by one in the MySQL CLI and copy the terminal output of each.

USE cms;

-- Q19: list all tables
SHOW TABLES;

-- Q20: exactly 5 rows from the patient table
SELECT * FROM patient LIMIT 5;

-- Q21: daily appointment report for one date
CALL GetDailyAppointmentReportByDoctor('2025-04-15');

-- Q22: busiest doctor of a month
CALL GetDoctorWithMostPatientsByMonth(4, 2025);

-- Q23: busiest doctor of a year
CALL GetDoctorWithMostPatientsByYear(2025);

-- Optional checks from the "Adding Databases and Tables" lab
SELECT * FROM doctor LIMIT 5;
SELECT * FROM doctor_available_times LIMIT 5;
SELECT * FROM appointment ORDER BY appointment_time LIMIT 5;
SELECT * FROM admin;
