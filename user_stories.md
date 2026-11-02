# User Stories — Smart Clinic Management System

These stories define what the Smart Clinic portal does for its three kinds of users — **admins**, **doctors** and **patients** — and guide the back-end and front-end work. Each story follows the template below. Stories marked *Planned* in their notes are part of the backlog but are not built in the current release.

## User Story Template

**Title:**
_As a [user role], I want [feature/goal], so that [reason]._

**Acceptance Criteria:**
1. [Criteria 1]
2. [Criteria 2]
3. [Criteria 3]

**Priority:** [High/Medium/Low]
**Story Points:** [Estimated Effort in Points]
**Notes:**
- [Additional information or edge cases]

## Overview

| Role | Stories | Built | Planned |
|------|---------|-------|---------|
| Admin | A1 – A6 | 6 | 0 |
| Patient | P1 – P8 | 8 (P8 through the API only) | 0 |
| Doctor | D1 – D7 | 5 | 2 (D6, D7) |

---

# Admin User Stories

## A1 — Admin logs in

**Title:**
_As an admin, I want to log into the portal with my username and password, so that I can manage the platform securely._

**Acceptance Criteria:**
1. Choosing "Admin" on the home page opens a login form that asks for a username and a password.
2. Valid credentials return a token and open the admin dashboard.
3. Invalid credentials show an error message and do not open the dashboard.

**Priority:** High
**Story Points:** 3
**Notes:**
- The token is a JWT that expires after 7 days; the dashboard URL is only served when the token is valid for the admin role, otherwise the user is sent back to the home page.

## A2 — Admin logs out

**Title:**
_As an admin, I want to log out of the portal, so that nobody else can use my session to access the system._

**Acceptance Criteria:**
1. A "Logout" link is visible in the header of the admin dashboard.
2. Clicking it removes the stored token and role and returns to the role-selection page.
3. After logging out, the dashboard cannot be reopened without logging in again.

**Priority:** High
**Story Points:** 1
**Notes:**
- Logging out is client-side (the stored token is discarded); an expired or tampered token is rejected by the server.

## A3 — Admin adds a doctor

**Title:**
_As an admin, I want to add doctors to the portal, so that patients can find them and book appointments._

**Acceptance Criteria:**
1. The "Add Doctor" button opens a form for name, specialization, email, password, phone number and available time slots.
2. The form is validated: name 3–100 characters, specialty 3–50 characters, a valid email, password of at least 6 characters, and a phone number of exactly 10 digits; a clear message is shown when a rule is broken.
3. A doctor whose email already exists is rejected with the message "Doctor already exists"; otherwise the doctor is saved and appears on the dashboard.

**Priority:** High
**Story Points:** 5
**Notes:**
- Only a valid admin token is accepted by the API (`POST /doctor/{token}`).
- The doctor's password is never returned by any API response.

## A4 — Admin deletes a doctor's profile

**Title:**
_As an admin, I want to delete a doctor's profile from the portal, so that doctors who have left no longer appear to patients._

**Acceptance Criteria:**
1. Each doctor card on the admin dashboard has a "Delete" button.
2. The admin must confirm before anything is deleted.
3. After confirmation the doctor and all of the doctor's appointments are removed and the card disappears.

**Priority:** High
**Story Points:** 3
**Notes:**
- The doctor and the appointments are deleted in one transaction, so a failure leaves the data unchanged.
- Only an admin token may call `DELETE /doctor/{id}/{token}`.

## A5 — Admin runs usage statistics from the MySQL CLI

**Title:**
_As an admin, I want to run stored procedures in the MySQL CLI to get appointment statistics, so that I can track how the platform is used._

**Acceptance Criteria:**
1. `GetDoctorWithMostPatientsByMonth(month, year)` returns the doctor who saw the most patients in that month, with the patient count.
2. `GetDoctorWithMostPatientsByYear(year)` returns the same for a whole year.
3. `GetDailyAppointmentReportByDoctor(date)` lists every appointment of that date with doctor, time, status, and the patient's name and phone number.

**Priority:** Medium
**Story Points:** 3
**Notes:**
- The procedures are read-only and live in `sql/stored-procedures.sql`.

## A6 — Admin searches and filters doctors

**Title:**
_As an admin, I want to search and filter the doctor list, so that I can quickly find the profile I need to review or remove._

**Acceptance Criteria:**
1. Typing in the search bar filters doctors by (part of) their name.
2. The list can also be filtered by specialty and by morning (AM) or afternoon (PM) availability, alone or combined with the name.
3. When nothing matches, the message "No doctors found with the given filters." is shown.

**Priority:** Medium
**Story Points:** 2
**Notes:**
- Filtering is done by the server (`GET /doctor/filter/{name}/{time}/{speciality}`); a filter that is not used is sent as `null`.

---

# Patient User Stories

## P1 — Patient browses doctors without logging in

**Title:**
_As a patient, I want to view the list of doctors without logging in, so that I can explore my options before registering._

**Acceptance Criteria:**
1. Choosing "Patient" on the home page opens a dashboard that lists all doctors with name, specialty, email and available times.
2. The list can be searched by name and filtered by specialty and time.
3. Pressing "Book Now" while not logged in tells the patient to log in first.

**Priority:** High
**Story Points:** 2
**Notes:**
- Viewing doctors needs no token; booking does.

## P2 — Patient signs up

**Title:**
_As a patient, I want to sign up with my email and password, so that I can book appointments._

**Acceptance Criteria:**
1. "Sign Up" opens a form for name, email, password, phone number and address.
2. The data is validated: name 3–100 characters, valid email, password of at least 6 characters, phone number of exactly 10 digits, address up to 255 characters.
3. An email or phone number that is already registered is rejected with a clear message; otherwise the account is created and a success message ("Signup successful") is shown.

**Priority:** High
**Story Points:** 3
**Notes:**
- The patient's password is never sent back by the API.

## P3 — Patient logs in

**Title:**
_As a patient, I want to log into the portal, so that I can manage my bookings._

**Acceptance Criteria:**
1. "Login" opens a form for email and password.
2. Valid credentials open the logged-in patient dashboard, where the header shows Home, Appointments and Logout.
3. Invalid credentials show an error and keep the patient on the public dashboard.

**Priority:** High
**Story Points:** 3
**Notes:**
- The login returns a JWT (valid 7 days) that is used for every later request.

## P4 — Patient logs out

**Title:**
_As a patient, I want to log out of the portal, so that my account stays secure on shared devices._

**Acceptance Criteria:**
1. A "Logout" link is shown in the header while logged in.
2. Clicking it removes the token and returns to the public patient dashboard.
3. The header shows "Login" and "Sign Up" again.

**Priority:** High
**Story Points:** 1
**Notes:**
- The role is reset to "patient" so the public pages keep working.

## P5 — Patient books an hour-long appointment

**Title:**
_As a patient, I want to log in and book an hour-long appointment, so that I can consult a doctor._

**Acceptance Criteria:**
1. "Book Now" on a doctor card opens a booking form showing the patient, the doctor, a date picker and the doctor's time slots.
2. Confirming books the appointment for that one-hour slot and shows a success message.
3. A slot that is already taken, or a time in the past, is rejected with a clear message, so two patients can never hold the same slot.

**Priority:** High
**Story Points:** 5
**Notes:**
- Availability is checked on the server before saving (`POST /appointments/{token}`), so two patients cannot book the same slot.
- The API can also list the free slots of a day (`GET /doctor/availability/{user}/{doctorId}/{date}/{token}`); the booking form currently shows all of the doctor's configured slots and relies on the server check.

## P6 — Patient views upcoming appointments

**Title:**
_As a patient, I want to view my upcoming appointments, so that I can prepare accordingly._

**Acceptance Criteria:**
1. The "Appointments" page lists my appointments with doctor name, date and time.
2. The list can be narrowed to upcoming or past appointments, and searched by doctor name.
3. If I have no appointments, "No Appointments Found" is shown.

**Priority:** High
**Story Points:** 3
**Notes:**
- A patient can only read their own appointments; the server rejects a token that belongs to a different patient.

## P7 — Patient reschedules an appointment

**Title:**
_As a patient, I want to change the date or time of a scheduled appointment, so that I can adapt when my plans change._

**Acceptance Criteria:**
1. A scheduled appointment has an edit icon that opens a form filled with the current details.
2. The patient can pick another date and one of the doctor's time slots.
3. Saving updates the appointment when the new slot is free; otherwise a clear message is shown.

**Priority:** Medium
**Story Points:** 3
**Notes:**
- The edit icon is only shown for scheduled appointments.

## P8 — Patient cancels an appointment

**Title:**
_As a patient, I want to cancel an appointment I can no longer attend, so that the slot becomes available to others._

**Acceptance Criteria:**
1. A patient can cancel only their own appointment.
2. Cancelling removes the appointment and frees the time slot.
3. Cancelling someone else's appointment, or an unknown one, is refused with a clear message.

**Priority:** Medium
**Story Points:** 2
**Notes:**
- Built in the API (`DELETE /appointments/{id}/{token}`); a "Cancel" button on the appointments page is not part of the current release.

---

# Doctor User Stories

## D1 — Doctor logs in

**Title:**
_As a doctor, I want to log into the portal, so that I can manage my appointments._

**Acceptance Criteria:**
1. Choosing "Doctor" on the home page opens a login form for email and password.
2. Valid credentials open the doctor dashboard.
3. Invalid credentials show an error and do not open the dashboard.

**Priority:** High
**Story Points:** 3
**Notes:**
- The dashboard is only served for a token that is valid for the doctor role.

## D2 — Doctor logs out

**Title:**
_As a doctor, I want to log out of the portal, so that my patients' data stays protected._

**Acceptance Criteria:**
1. A "Logout" link is shown in the header of the doctor dashboard.
2. Clicking it removes the token and returns to the role-selection page.
3. The dashboard cannot be reopened without logging in again.

**Priority:** High
**Story Points:** 1
**Notes:**
- Same mechanism as the admin logout (A2).

## D3 — Doctor views the appointment calendar

**Title:**
_As a doctor, I want to view my appointments for a chosen day, so that I stay organized._

**Acceptance Criteria:**
1. The dashboard opens with today's appointments in a table.
2. A date picker (and a "Today" button) shows the appointments of any other day, and a search box narrows them by patient name.
3. A day without appointments shows a "No Appointments found" message.

**Priority:** High
**Story Points:** 3
**Notes:**
- A doctor only ever sees their own appointments (`GET /appointments/{date}/{patientName}/{token}` uses the doctor from the token).

## D4 — Doctor views patient details for upcoming appointments

**Title:**
_As a doctor, I want to view the patient details for my appointments, so that I can be prepared._

**Acceptance Criteria:**
1. Each appointment row shows the patient's ID, name, phone number and email.
2. Clicking the patient ID opens that patient's record: the appointments the patient has with this doctor.
3. A doctor never sees appointments the patient has with other doctors.

**Priority:** High
**Story Points:** 2
**Notes:**
- Uses `GET /patient/{id}/doctor/{token}`.

## D5 — Doctor adds a prescription

**Title:**
_As a doctor, I want to add a prescription to an appointment, so that the patient has accurate medication instructions on record._

**Acceptance Criteria:**
1. The prescription icon on an appointment row opens a form for medication, dosage and notes.
2. Saving stores the prescription and marks the appointment as completed.
3. A second prescription for the same appointment is refused with a clear message.

**Priority:** High
**Story Points:** 3
**Notes:**
- Prescriptions are stored in MongoDB because their content is free-form; the appointment is referenced by its ID.

## D6 — Doctor marks unavailability

**Title:**
_As a doctor, I want to mark times when I am unavailable, so that patients only see the slots I can actually take._

**Acceptance Criteria:**
1. A doctor can remove a time slot (or a whole day) from their availability.
2. Patients no longer see the removed slots when booking.
3. Appointments that are already booked are not silently cancelled.

**Priority:** Medium
**Story Points:** 5
**Notes:**
- *Planned — not in the current release.* Today the admin sets a doctor's slots when adding the doctor (and can change them with `PUT /doctor/{token}`); the availability endpoint already leaves out slots that are booked.

## D7 — Doctor updates their profile

**Title:**
_As a doctor, I want to update my profile with my specialization and contact information, so that patients have up-to-date information._

**Acceptance Criteria:**
1. A doctor can change specialization, phone number and email.
2. The same validation rules as when the doctor was created apply.
3. The updated details are shown to patients immediately.

**Priority:** Low
**Story Points:** 3
**Notes:**
- *Planned — not in the current release.* The API supports updating a doctor (`PUT /doctor/{token}`) but only for an admin token; there is no doctor-facing screen yet.
