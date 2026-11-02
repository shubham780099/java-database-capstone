# Smart Clinic Management System

A full-stack web application that moves a clinic's administration from paper to an online portal. Admins manage doctors, doctors manage their day's appointments and prescriptions, and patients find doctors and book appointments.

- **Back end:** Java 17, Spring Boot 3 (REST controllers + Thymeleaf dashboards), Spring Data JPA, Spring Data MongoDB, JWT authentication
- **Databases:** MySQL (admins, doctors, patients, appointments) and MongoDB (prescriptions)
- **Front end:** HTML, CSS and vanilla JavaScript (ES modules) served by the same Spring Boot application
- **DevOps:** multi-stage Docker build, GitHub Actions for linting and compiling

## What each role can do

| Role | Features |
|------|----------|
| Admin | Log in, add and delete doctors, search and filter the doctor list |
| Doctor | Log in, view appointments by day and patient name, see patient details and records, add prescriptions |
| Patient | Browse doctors without an account, sign up, log in, book / reschedule appointments, view upcoming and past appointments |

The user stories behind these features are in [user_stories.md](user_stories.md).

## Quick start

Prerequisites: JDK 17, Maven, a running MySQL server and a running MongoDB server.

1. **Create the MySQL database**
   ```sql
   CREATE DATABASE cms;
   ```
2. **Configure the connections** in `app/src/main/resources/application.properties` — replace the `<mysql_host>`, `<mysql_password>`, `<mongodb_host>` and `<mongodb_password>` placeholders (see [Configuration](#configuration)). Do not commit real passwords.
3. **Start the application** — Hibernate creates the tables on first start:
   ```bash
   cd app
   mvn spring-boot:run
   ```
4. **Load the sample data and reports** (run once the tables exist):
   ```sql
   USE cms;
   SOURCE sql/sample-data.sql;
   SOURCE sql/stored-procedures.sql;
   ```
   ```javascript
   // MongoDB shell
   load("sql/mongo-prescriptions.js")
   ```
5. **Open** http://localhost:8080 and choose a role.

> `mvn clean install` also runs the Spring context test, which needs both databases to be reachable. Add `-DskipTests` to build without them.

### Sample logins (from `sql/sample-data.sql`)

| Role | Login | Password |
|------|-------|----------|
| Admin | `admin` | `admin@1234` |
| Doctor | `dr.adams@example.com` | `pass12345` |
| Patient | `jane.doe@example.com` | `passJane1` |

The sample appointments are dated April–May 2025: choose one of those dates in the doctor dashboard's date picker to see them.

## Configuration

Each value can be set in `application.properties` or overridden with an environment variable.

| Property | Environment variable | Purpose |
|----------|----------------------|---------|
| `spring.datasource.url` | `SPRING_DATASOURCE_URL` | MySQL JDBC URL, e.g. `jdbc:mysql://localhost:3306/cms` |
| `spring.datasource.username` | `SPRING_DATASOURCE_USERNAME` | MySQL user |
| `spring.datasource.password` | `SPRING_DATASOURCE_PASSWORD` | MySQL password |
| `spring.data.mongodb.uri` | `SPRING_DATA_MONGODB_URI` | MongoDB connection string |
| `jwt.secret` | `JWT_SECRET` | Signing key for login tokens (at least 32 characters; use your own in production) |
| `api.path` | `API_PATH` | Prefix of the admin, doctor and prescription endpoints (default `/`) |

## API overview

| Endpoint | Who | Purpose |
|----------|-----|---------|
| `POST /admin` | admin | Log in, returns a token |
| `POST /doctor/login`, `POST /patient/login` | doctor, patient | Log in, returns a token |
| `POST /patient` | anyone | Patient sign-up |
| `GET /doctor` | anyone | List doctors |
| `GET /doctor/filter/{name}/{time}/{speciality}` | anyone | Filter doctors (use `null` to skip a filter) |
| `GET /doctor/availability/{user}/{doctorId}/{date}/{token}` | any logged-in role | Free slots of a doctor on a date (`yyyy-MM-dd`) |
| `POST /doctor/{token}`, `PUT /doctor/{token}`, `DELETE /doctor/{id}/{token}` | admin | Add, update, delete a doctor |
| `GET /appointments/{date}/{patientName}/{token}` | doctor | The doctor's appointments on a date |
| `POST /appointments/{token}`, `PUT /appointments/{token}`, `DELETE /appointments/{id}/{token}` | patient | Book, change, cancel an appointment |
| `GET /patient/{token}` | patient | The logged-in patient's details |
| `GET /patient/{id}/{user}/{token}` | patient, doctor | A patient's appointments |
| `GET /patient/filter/{condition}/{name}/{token}` | patient | Filter own appointments (`past` / `future`, doctor name) |
| `POST /prescription/{token}`, `GET /prescription/{appointmentId}/{token}` | doctor | Save / read a prescription |

Tokens are JWTs valid for 7 days. Validation errors come back as HTTP 400 with a `message`.

## Docker

```bash
cd app
docker build -t smart-clinic-backend .
docker run -d -p 8080:8080 --name smart-clinic \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://<mysql_host>:3306/cms \
  -e SPRING_DATASOURCE_USERNAME=<user> \
  -e SPRING_DATASOURCE_PASSWORD=<password> \
  -e SPRING_DATA_MONGODB_URI=mongodb://<mongodb_host>:27017/prescriptions \
  smart-clinic-backend
```

The Dockerfile is multi-stage: Maven and JDK 17 build the jar, and only that jar is copied onto a JRE-only image that runs as an unprivileged user.

## Continuous integration

Four GitHub Actions workflows in `.github/workflows` run on every push and pull request:

| Workflow | What it does |
|----------|--------------|
| `compile-backend.yml` | Compiles the Spring Boot back end with Maven on JDK 17 |
| `lint-backend.yml` | Runs Checkstyle (Google style) over the Java sources |
| `lint-frontend.yml` | Lints the HTML, CSS and JavaScript (rules in `.htmlhintrc`, `.stylelintrc.json`, `eslint.config.mjs`) |
| `lint-docker.yml` | Lints the Dockerfile with Hadolint |

## Documentation

- [user_stories.md](user_stories.md) — admin, patient and doctor user stories
- [schema-design.md](schema-design.md) — MySQL tables and the MongoDB collection, with design decisions
- [sql/](sql) — sample data, stored procedures, the report commands, and the MongoDB sample data
