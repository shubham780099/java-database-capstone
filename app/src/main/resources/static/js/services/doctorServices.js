// doctorServices.js
// API calls for doctors: list, delete, create and filter.

import { API_BASE_URL } from "../config/config.js";

const DOCTOR_API = API_BASE_URL + "/doctor";

// Fetches every doctor. Resolves to the "doctors" array (empty on error).
export async function getDoctors() {
  try {
    const response = await fetch(DOCTOR_API);
    const result = await response.json();
    return result.doctors || [];
  } catch (error) {
    console.error("Error :: getDoctors :: ", error);
    return [];
  }
}

// Deletes a doctor (admin token required). Resolves to { success, message }.
export async function deleteDoctor(id, token) {
  try {
    const response = await fetch(`${DOCTOR_API}/${id}/${token}`, {
      method: "DELETE"
    });
    const result = await response.json();
    return { success: response.ok, message: result.message };
  } catch (error) {
    console.error("Error :: deleteDoctor :: ", error);
    return { success: false, message: "Failed to delete the doctor. Please try again." };
  }
}

// Creates a doctor (admin token required). Resolves to { success, message }.
export async function saveDoctor(doctor, token) {
  try {
    const response = await fetch(`${DOCTOR_API}/${token}`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify(doctor)
    });
    const result = await response.json();
    return { success: response.ok, message: result.message };
  } catch (error) {
    console.error("Error :: saveDoctor :: ", error);
    return { success: false, message: "Failed to save the doctor. Please try again." };
  }
}

// Filters doctors by name, time (AM / PM or a slot such as 09:00-10:00) and specialty.
// Pass null for a filter you do not want; the API reads the text "null" as "no filter".
export async function filterDoctors(name, time, specialty) {
  try {
    const url = `${DOCTOR_API}/filter/${encodeURIComponent(name)}/${encodeURIComponent(time)}/${encodeURIComponent(specialty)}`;
    const response = await fetch(url, { method: "GET" });

    if (response.ok) {
      return await response.json();
    }
    console.error("Failed to filter doctors:", response.statusText);
    return { doctors: [] };
  } catch (error) {
    console.error("Error :: filterDoctors :: ", error);
    alert("Something went wrong!");
    return { doctors: [] };
  }
}
