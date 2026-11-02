// doctorDashboard.js
// Doctor dashboard: shows the logged-in doctor's appointments for a chosen day,
// with a search box for the patient's name.

import { getAllAppointments } from "./services/appointmentRecordService.js";
import { createPatientRow } from "./components/patientRows.js";

const tableBody = document.getElementById("patientTableBody");
const token = localStorage.getItem("token");

// Today in the browser's local time, as YYYY-MM-DD.
function todayString() {
  const now = new Date();
  const month = String(now.getMonth() + 1).padStart(2, "0");
  const day = String(now.getDate()).padStart(2, "0");
  return `${now.getFullYear()}-${month}-${day}`;
}

let selectedDate = todayString();
let patientName = null; // null -> sent to the API as "null" (no name filter)

// Typing in the search box filters by patient name.
document.getElementById("searchBar").addEventListener("input", (event) => {
  const value = event.target.value.trim();
  patientName = value.length > 0 ? value : "null";
  loadAppointments();
});

// "Today" button jumps back to today's appointments.
document.getElementById("todayButton").addEventListener("click", () => {
  selectedDate = todayString();
  document.getElementById("datePicker").value = selectedDate;
  loadAppointments();
});

// Picking a date loads that day.
document.getElementById("datePicker").addEventListener("change", (event) => {
  selectedDate = event.target.value;
  loadAppointments();
});

// Fetches and displays the appointments for selectedDate (and patientName, if set).
async function loadAppointments() {
  try {
    const result = await getAllAppointments(selectedDate, patientName, token);
    const appointments = result.appointments || [];

    tableBody.innerHTML = "";

    if (appointments.length === 0) {
      tableBody.innerHTML =
        `<tr><td class="noPatientRecord" colspan="5">No Appointments found for today.</td></tr>`;
      return;
    }

    appointments.forEach((appointment) => {
      const patient = {
        id: appointment.patientId,
        name: appointment.patientName,
        phone: appointment.patientPhone,
        email: appointment.patientEmail
      };
      tableBody.appendChild(createPatientRow(patient, appointment.id, appointment.doctorId));
    });
  } catch (error) {
    console.error("Error loading appointments:", error);
    tableBody.innerHTML =
      `<tr><td class="noPatientRecord" colspan="5">Error loading appointments. Try again later.</td></tr>`;
  }
}

document.addEventListener("DOMContentLoaded", () => {
  renderContent(); // from render.js: sends the user to "/" when no role is stored
  document.getElementById("datePicker").value = selectedDate;
  loadAppointments();
});
