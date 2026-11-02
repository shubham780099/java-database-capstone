// doctorCard.js
// Builds the DOM element for one doctor. The buttons on the card depend on the user's role.

import { deleteDoctor } from "../services/doctorServices.js";
import { getPatientData } from "../services/patientServices.js";

export function createDoctorCard(doctor) {
  const card = document.createElement("div");
  card.classList.add("doctor-card");

  const role = localStorage.getItem("userRole");

  // ---- doctor information ----
  const infoDiv = document.createElement("div");
  infoDiv.classList.add("doctor-info");

  const name = document.createElement("h3");
  name.textContent = doctor.name;

  const specialization = document.createElement("p");
  specialization.textContent = `Specialization: ${doctor.specialty}`;

  const email = document.createElement("p");
  email.textContent = `Email: ${doctor.email}`;

  const availability = document.createElement("p");
  availability.textContent = `Availability: ${(doctor.availableTimes || []).join(", ")}`;

  infoDiv.appendChild(name);
  infoDiv.appendChild(specialization);
  infoDiv.appendChild(email);
  infoDiv.appendChild(availability);

  // ---- action buttons ----
  const actionsDiv = document.createElement("div");
  actionsDiv.classList.add("card-actions");

  if (role === "admin") {
    const removeBtn = document.createElement("button");
    removeBtn.textContent = "Delete";
    removeBtn.addEventListener("click", async () => {
      if (!confirm(`Delete Dr. ${doctor.name}? This also removes the doctor's appointments.`)) {
        return;
      }
      const token = localStorage.getItem("token");
      if (!token) {
        alert("Admin session expired. Please log in again.");
        return;
      }
      const { success, message } = await deleteDoctor(doctor.id, token);
      alert(message);
      if (success) {
        card.remove();
      }
    });
    actionsDiv.appendChild(removeBtn);
  } else if (role === "patient") {
    const bookNow = document.createElement("button");
    bookNow.textContent = "Book Now";
    bookNow.addEventListener("click", () => {
      alert("Patient needs to login first.");
    });
    actionsDiv.appendChild(bookNow);
  } else if (role === "loggedPatient") {
    const bookNow = document.createElement("button");
    bookNow.textContent = "Book Now";
    bookNow.addEventListener("click", async (e) => {
      const token = localStorage.getItem("token");
      if (!token) {
        window.location.href = "/";
        return;
      }
      const patientData = await getPatientData(token);
      if (!patientData) {
        alert("Could not load your details. Please log in again.");
        return;
      }
      // Loaded on demand: loggedPatient.js attaches page-level listeners when it is evaluated,
      // which should only happen on the logged-in patient's page, not on every page with cards.
      const { showBookingOverlay } = await import("../loggedPatient.js");
      showBookingOverlay(e, doctor, patientData);
    });
    actionsDiv.appendChild(bookNow);
  }

  card.appendChild(infoDiv);
  card.appendChild(actionsDiv);
  return card;
}
