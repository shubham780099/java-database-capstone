// header.js
// Renders the page header. What it shows depends on the role stored in localStorage:
//   admin          -> Add Doctor button + Logout
//   doctor         -> Home + Logout
//   patient        -> Login + Sign Up
//   loggedPatient  -> Home + Appointments + Logout

function renderHeader() {
  const headerDiv = document.getElementById("header");
  if (!headerDiv) {
    return;
  }

  // Role-selection page ("/"): forget any earlier session and show only the logo.
  if (window.location.pathname.endsWith("/")) {
    localStorage.removeItem("userRole");
    localStorage.removeItem("token");
    headerDiv.innerHTML = `
      <header class="header">
        <div class="logo-section">
          <img src="/assets/images/logo/logo.png" alt="Hospital CRM Logo" class="logo-img">
          <span class="logo-title">Hospital CMS</span>
        </div>
      </header>`;
    return;
  }

  const role = localStorage.getItem("userRole");
  const token = localStorage.getItem("token");

  // A logged-in role without a token means the session is gone.
  if ((role === "loggedPatient" || role === "admin" || role === "doctor") && !token) {
    localStorage.removeItem("userRole");
    alert("Session expired or invalid login. Please log in again.");
    window.location.href = "/";
    return;
  }

  let headerContent = `
    <header class="header">
      <div class="logo-section">
        <img src="/assets/images/logo/logo.png" alt="Hospital CRM Logo" class="logo-img">
        <span class="logo-title">Hospital CMS</span>
      </div>
      <nav>`;

  if (role === "admin") {
    // The click handler is attached by adminDashboard.js, which owns the modal helper.
    headerContent += `
        <button id="addDocBtn" class="adminBtn">Add Doctor</button>
        <a href="#" onclick="logout()">Logout</a>`;
  } else if (role === "doctor") {
    headerContent += `
        <button class="adminBtn" onclick="selectRole('doctor')">Home</button>
        <a href="#" onclick="logout()">Logout</a>`;
  } else if (role === "patient") {
    // The click handlers are attached by patientDashboard.js.
    headerContent += `
        <button id="patientLogin" class="adminBtn">Login</button>
        <button id="patientSignup" class="adminBtn">Sign Up</button>`;
  } else if (role === "loggedPatient") {
    headerContent += `
        <button id="home" class="adminBtn" onclick="window.location.href='/pages/loggedPatientDashboard.html'">Home</button>
        <button id="patientAppointments" class="adminBtn" onclick="window.location.href='/pages/patientAppointments.html'">Appointments</button>
        <a href="#" onclick="logoutPatient()">Logout</a>`;
  }

  headerContent += `
      </nav>
    </header>`;

  headerDiv.innerHTML = headerContent;
  attachHeaderButtonListeners();
}

// The header HTML is created dynamically, so listeners must be attached after it is inserted.
// Buttons that open modals are wired by the page modules (openModal lives in an ES module);
// here we only make sure the Home buttons of the role-based headers navigate correctly.
function attachHeaderButtonListeners() {
  const homeBtn = document.getElementById("home");
  if (homeBtn) {
    homeBtn.addEventListener("click", () => {
      window.location.href = "/pages/loggedPatientDashboard.html";
    });
  }
}

// Ends the session for admins and doctors and returns to the role-selection page.
function logout() {
  localStorage.removeItem("token");
  localStorage.removeItem("userRole");
  window.location.href = "/";
}

// Ends a patient's session but keeps the "patient" role so Login / Sign Up show again.
function logoutPatient() {
  localStorage.removeItem("token");
  localStorage.setItem("userRole", "patient");
  window.location.href = "/pages/patientDashboard.html";
}

renderHeader();
