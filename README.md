# Student Portal Management System

A Java-based web application designed to simplify student information management through secure authentication, personalized dashboards, academic records, and administrative functionalities.

## Overview

The **Student Portal Management System** is an academic web application developed to provide students and administrators with a centralized platform for managing student-related information.

The application offers separate interfaces for students and administrators. Students can access their personal profiles and academic information, while administrators can manage student records through an integrated administrative dashboard.

The project is developed using Core Java, JDBC, HTML, CSS, and JavaScript, with an embedded H2 database for persistent data storage.

This project is developed as an independent academic prototype inspired by university student management workflows.

## Key Features

### Student Module
- Secure student authentication.
- Personalized student dashboard.
- View personal and academic information.
- Semester fee assessment and online fee payment with instant clearance status.
- Access student profile details.
- View available academic records.
- Student self-registration.

### Administration Module
- Dedicated administrator login.
- View registered students.
- One-click student fee payment clearance and status tracking on dashboard and directory.
- Add new student records.
- Update existing student information.
- Delete student records.
- Search students by name, roll number, or programme.
- View student management statistics.

### Application Features
- Role-based access control.
- Database integration using JDBC.
- Persistent data storage using H2.
- Input validation and error handling.
- Responsive user interface.
- Password visibility toggle.
- Session-based authentication.
- Independent application server.

## Technology Stack

| Category | Technologies |
|---|---|
| Programming Language | Java 17+ |
| Frontend | HTML5, CSS3, JavaScript |
| Database | H2 Database |
| Database Connectivity | JDBC |
| Architecture | DAO Pattern |
| HTTP Server | Java ServerSocket |
| Concurrency | Java ExecutorService |
| Build Configuration | Maven (if used by the current build) |
| Development Environment | Visual Studio Code |

## Application Architecture

The application follows a modular architecture that separates data access, application models, database connectivity, and request handling.

### Main Components

**1. Server and Routing Layer**

`StandaloneAppRunner.java`

- Initializes the standalone Java HTTP server.
- Handles incoming HTTP requests.
- Manages application routes.
- Serves frontend resources.
- Handles authentication sessions and role-based access.

**2. Data Access Layer**

The DAO (Data Access Object) pattern separates database operations from application logic.

- `AdminDAO.java` – Administrator-related database operations.
- `StudentDAO.java` – Student record management and CRUD operations.
- `StudentAuthDAO.java` – Student authentication operations.

**3. Model Layer**

Contains Java classes representing application entities.

- `Admin.java`
- `Student.java`

**4. Utility Layer**

`DBUtil.java`

Responsible for database connectivity, initialization, and database-related configuration.

### Request Flow

Browser → Java HTTP Server → Request Handler → DAO Layer → H2 Database

The server processes incoming requests, interacts with the appropriate DAO classes, retrieves or updates database records, and returns the corresponding application response.

## Project Structure

```text
Student-manage/
│
├── run.ps1
├── run.bat
├── pom.xml
├── Dockerfile
├── render.yaml
├── Procfile
├── README.md
│
├── data/
│   └── student_db.mv.db
│
└── src/
    └── main/
        ├── java/
        │   └── com/studentmanagement/
        │       ├── StandaloneAppRunner.java
        │       │
        │       ├── dao/
        │       │   ├── AdminDAO.java
        │       │   ├── StudentDAO.java
        │       │   └── StudentAuthDAO.java
        │       │
        │       ├── model/
        │       │   ├── Admin.java
        │       │   └── Student.java
        │       │
        │       └── util/
        │           └── DBUtil.java
        │
        ├── resources/
        │   └── db.properties
        │
        └── webapp/
            ├── css/
            │   └── style.css
            │
            └── js/
                └── main.js
```

## Getting Started

### Prerequisites

Before running the application, ensure that you have:

- Java Development Kit (JDK 17 or higher).
- Java configured in your system PATH.
- A modern web browser.
- Git (optional, for cloning the repository).

Verify your Java installation:

```bash
java -version
javac -version
```

### Installation

**1. Clone the repository**

```bash
git clone https://github.com/YOUR-USERNAME/student-portal-management-system.git
```

**2. Navigate to the project directory**

```bash
cd student-portal-management-system
```

**3. Start the application**

For Windows PowerShell:

```powershell
.\run.ps1
```

Alternatively, use Command Prompt:

```cmd
run.bat
```

**4. Open the application**

Once the server starts successfully, open your browser and navigate to:

```text
http://localhost:8080/login
```

**5. Stop the application**

Press `Ctrl + C` in the terminal to stop the server.

## Demo Credentials

The application provides separate login interfaces for administrators and students.

### Administrator Login

| Field | Demo Value |
|---|---|
| Username | `admin` |
| Password | `admin123` |

### Student Login

Students can log in using their registered roll number and email address, as configured in the demo database.

| Roll Number | Student Name |
|---|---|
| CS202401 | Aarav Sharma |
| CS202402 | Priya Patel |
| IT202401 | Rohan Mehta |
| EC202401 | Ananya Verma |
| ME202401 | Vikram Singh |

The demo credentials and sample records are intended for local testing only. Do not use these credentials in a production environment.

## Database and Persistence

The application uses an embedded H2 relational database for storing student and administrator information.

### Database Features

- Automatic database initialization.
- Persistent local storage.
- JDBC-based database connectivity.
- Structured relational tables.
- CRUD operations for student records.
- Parameterized SQL queries.

The default database is stored locally in:

```text
./data/student_db.mv.db
```

The application is configured to use H2 in MySQL-compatible mode.

An external MySQL configuration is also documented in `db.properties`, if supported by the current database utility implementation.

## Security and Validation

The application incorporates several security and validation mechanisms:

- Role-based access for students and administrators.
- Session-based authentication.
- Prepared statements for database queries.
- Server-side input validation.
- Client-side form validation.
- HTML output escaping for dynamic content.
- Protected access to student and administrative pages.

**Note:** The demo administrator credentials and student email-based login are for academic demonstration. Production deployment would require stronger password management, secure session configuration, HTTPS, and additional security testing.

## Future Enhancements

Potential improvements for future versions include:

- Enhanced password security and account recovery.
- Academic performance analytics.
- Semester-wise result management.
- Attendance management.
- Course and subject management.
- Improved administrative reporting.
- Deployment to a cloud hosting environment.

## Academic Disclaimer

This project is an independent academic prototype developed for educational and laboratory demonstration purposes. It is inspired by student management and academic workflows at institutions such as Thapar Institute of Engineering & Technology (TIET), Patiala.

It is not an official TIET application and is not affiliated with or connected to the university's internal information systems.

## Author

**MEHAK GARG**

Computer Engineering Student

---

*Developed as an academic software engineering project.*
