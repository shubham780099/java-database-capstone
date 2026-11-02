// adminDashboard.js
// Admin dashboard: lists doctors as cards, filters them, and adds a new doctor through a modal.

import { openModal } from "./components/modals.js";
import { getDoctors, filterDoctors, saveDoctor } from "./services/doctorServices.js";
import { createDoctorCard } from "./components/doctorCard.js";

// modals.js calls adminAddDoctor() by name, and other scripts may call openModal() inline.
window.openModal = openModal;

// "Add Doctor" button (rendered by header.js) opens the add-doctor modal.
document.addEventListener("DOMContentLoaded", () => {
  const addDocBtn = document.getElementById("addDocBtn");
  if (addDocBtn) {
    addDocBtn.addEventListener("click", () => openModal("addDoctor"));
  }
  loadDoctorCards();
});

// Fetches all doctors and shows them as cards.
async function loadDoctorCards() {
  try {
    const doctors = await getDoctors();
    renderDoctorCards(doctors);
  } catch (error) {
    console.error("Failed to load doctors:", error);
  }
}

// Search box and filter dropdowns all re-run the filter.
document.getElementById("searchBar").addEventListener("input", filterDoctorsOnChange);
document.getElementById("filterTime").addEventListener("change", filterDoctorsOnChange);
document.getElementById("filterSpecialty").addEventListener("change", filterDoctorsOnChange);

async function filterDoctorsOnChange() {
  const searchBar = document.getElementById("searchBar").value.trim();
  const filterTime = document.getElementById("filterTime").value;
  const filterSpecialty = document.getElementById("filterSpecialty").value;

  // An empty control means "no filter" -> null (sent to the API as the text "null").
  const name = searchBar.length > 0 ? searchBar : null;
  const time = filterTime.length > 0 ? filterTime : null;
  const specialty = filterSpecialty.length > 0 ? filterSpecialty : null;

  try {
    const response = await filterDoctors(name, time, specialty);
    const doctors = response.doctors || [];

    if (doctors.length > 0) {
      renderDoctorCards(doctors);
    } else {
      document.getElementById("content").innerHTML = "<p>No doctors found with the given filters.</p>";
    }
  } catch (error) {
    console.error("Failed to filter doctors:", error);
    alert("❌ An error occurred while filtering doctors.");
  }
}

// Replaces the content area with one card per doctor.
function renderDoctorCards(doctors) {
  const contentDiv = document.getElementById("content");
  contentDiv.innerHTML = "";

  doctors.forEach((doctor) => {
    contentDiv.appendChild(createDoctorCard(doctor));
  });
}

// Called by the "Save" button of the add-doctor modal (see modals.js).
window.adminAddDoctor = async function () {
  const name = document.getElementById("doctorName").value.trim();
  const specialty = document.getElementById("specialization").value;
  const email = document.getElementById("doctorEmail").value.trim();
  const password = document.getElementById("doctorPassword").value;
  const phone = document.getElementById("doctorPhone").value.trim();
  const availableTimes = Array.from(
    document.querySelectorAll('input[name="availability"]:checked')
  ).map((checkbox) => checkbox.value);

  const token = localStorage.getItem("token");
  if (!token) {
    alert("Admin session expired. Please log in again.");
    return;
  }

  const doctor = { name, specialty, email, password, phone, availableTimes };
  const { success, message } = await saveDoctor(doctor, token);

  if (success) {
    alert(message);
    document.getElementById("modal").style.display = "none";
    window.location.reload();
  } else {
    alert("❌ " + message);
  }
};
