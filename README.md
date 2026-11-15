# 🎓 TIET Student Portal
### Thapar Institute of Engineering & Technology, Patiala
> **Your Academic Journey, Simplified.**

An enterprise-style Student Management & Academic Information System inspired by the aesthetic, grading standards, and academic workflows of **Thapar Institute of Engineering & Technology (TIET), Patiala (Est. 1956)**.

Developed in pure Java with **zero external server dependencies** (no Tomcat, no Apache, no manual MySQL installation required).

---

## 📑 Table of Contents
1. [Overview & Highlights](#-overview--highlights)
2. [Prerequisites](#-prerequisites)
3. [How to Run (Step-by-Step)](#-how-to-run-step-by-step)
4. [Login Credentials & Demo Accounts](#-login-credentials--demo-accounts)
5. [Application Features & Walkthrough](#-application-features--walkthrough)
6. [Project Structure](#-project-structure)
7. [Technical Architecture](#-technical-architecture)
8. [Database & Persistence](#-database--persistence)
9. [Security & Validation](#-security--validation)
10. [Troubleshooting & FAQ](#-troubleshooting--faq)
11. [Academic Prototype Disclaimer](#-academic-prototype-disclaimer)

---

## 🌟 Overview & Highlights

- **Zero-Setup Database:** Runs out of the box using an embedded, persistent H2 database engine. Schema creation, table migrations, and sample student data are initialized automatically upon first launch.
- **Standalone Web Server:** Uses a custom, lightweight, multi-threaded Java `ServerSocket` engine (`StandaloneAppRunner.java`) listening on port `8080`.
- **TIET Academic Branding:** Styled with TIET's official palette:
  - **Primary Deep Red:** `#9E1B32`
  - **Accent Red:** `#C6283D`
  - **Soft Red Background:** `#FFF5F5`
  - **Main Canvas:** `#F6F6F7`
  - **Clean Surface:** `#FFFFFF`
  - **Typography & Borders:** Main `#252525`, Secondary `#737373`, Borders `#E8E8E8`
- **Dual-Portal Workflow:** Dedicated, authenticated environments for both **Students** and **Administrators**.
- **Real Database Integration:** Every statistic, student detail, CGPA value, course, and fee status displayed in the UI is queried dynamically from the database.

---

## 💻 Prerequisites

To run this application on your computer, you only need:

1. **Java Development Kit (JDK 17 or higher):**
   - Verify by opening your terminal and typing:
     ```bash
     java -version
     javac -version
     ```
2. **Any Modern Web Browser:**
   - Google Chrome, Microsoft Edge, Mozilla Firefox, Brave, or Safari.

*(No external database software like MySQL Workbench or XAMPP is required! The database is self-contained).*

---

## 🚀 How to Run (Step-by-Step)

### Option A: Using PowerShell (Recommended on Windows)
1. Open **PowerShell** in the project directory (`Student-manage`).
2. Run the startup script:
   ```powershell
   .\run.ps1
   ```

### Option B: Using Command Prompt / Double-Click
1. Double-click **`run.bat`** in the project folder, **OR** open Command Prompt (`cmd`) and run:
   ```cmd
   run.bat
   ```

### What You Will See in the Terminal:
```text
=== Compiling Java files... ===
=== Compilation successful! Starting server... ===

  Open your browser at:  http://localhost:8080/login

  Admin login -> Username: admin  |  Password: admin123
  Student login -> Use Roll Number + Registered Email

  Press Ctrl+C to stop the server.

==================================================================
   TIET Student Portal - Thapar Institute of Engg. & Technology   
   Patiala, Punjab, India | Academic Session 2024-2025            
==================================================================
[DB] Database connected and schema synchronized successfully.
[DB] Storage Location: ./data/student_db
[Server] Application is live on port 8080!
👉 Access Portal at: http://localhost:8080/login
```

3. Open your browser and navigate to:  
   👉 **`http://localhost:8080/login`**

4. **To Stop the Server:** Press `Ctrl + C` in the terminal window.

---

## 🔑 Login Credentials & Demo Accounts

The portal features an integrated role-switcher on the login screen. Select the appropriate tab for your role:

### 1. Administrative Portal (`Admin`)
Full privileges to view the student directory, enroll new students, edit profiles, and delete records.

| Field | Value |
|---|---|
| **Role Tab** | **Administrative Portal** |
| **Username** | `admin` |
| **Password** | `admin123` |

---

### 2. Student Portal (`Student`)
Students log in using their **Roll Number** and **Registered Email** (acts as their secure academic password):

| Roll Number | Registered Email (Password) | Student Name | Enrolled Programme | CGPA | Fee Status |
|---|---|---|---|---|---|
| `CS202401` | `aarav.sharma@college.edu` | Aarav Sharma | B.E. Computer Engineering | 8.75 | Paid |
| `CS202402` | `priya.patel@college.edu` | Priya Patel | B.E. Computer Engineering | 9.20 | Paid |
| `IT202401` | `rohan.mehta@college.edu` | Rohan Mehta | B.E. Information Technology | 7.85 | Pending |
| `EC202401` | `ananya.verma@college.edu` | Ananya Verma | B.E. Electronics & Comm. | 8.90 | Paid |
| `ME202401` | `vikram.singh@college.edu` | Vikram Singh | B.E. Mechanical Engineering | 8.10 | Partial |

> 💡 **Self-Registration:** New students can also click the **"Self-Enroll / Sign Up"** link on the login page (`/register`) to create a brand-new profile that immediately becomes authenticable!

---

## 📱 Application Features & Walkthrough

### 🎓 Student Workspace
When logged in as a student, the user has access to a dedicated personal portal:

1. **Academic Dashboard (`/student-dashboard`):**
   - **Personalized Welcome:** Welcomes the student with their name, roll number, semester, and course.
   - **Live Metric Cards:**
     - **Cumulative GPA (CGPA):** Evaluated out of `10.00` on the TIET grading standard.
     - **Current Semester:** Active academic term registration.
     - **Semester Fee Status:** Color-coded status badge (`Paid`, `Partial`, or `Pending`).
     - **Enrollment Standing:** Good standing verification.
   - **Quick Actions:** One-click shortcuts to Profile, Grade Records, and Academic Desk.
   - **Official Circulars & Notice Board:** Real-time updates regarding End-Semester Examinations (ESE), fee deadlines, and capstone electives.

2. **My Profile (`/student-profile`):**
   - Structured view of the student's personal identity and academic registration.
   - Details: Roll Number, Full Legal Name, Degree Programme, Academic Department, Current Semester, Official Email, Phone Number, CGPA, and Fee Status.
   - Institutional verification notice from the Office of Academic Affairs.

3. **Academic Records (`/academic-records`):**
   - **Cumulative GPA Summary:** Live database-verified CGPA score.
   - **Semester Curriculum Table:** Registered course sheet for the active semester with Course Codes (e.g. `UCS503`, `UCS505`, `UMA010`), Course Titles, Credits, and Graded Status.
   - **TIET 10-Point Grading Scale Guide:** Official reference for letter grades (`A+`, `A`, `B`, `B-`, `C`, `C-`, `D`, `E`, `F`) and grade point equivalents.

---

### 🔑 Administrative Console
When logged in as an administrator (`admin` / `admin123`), the user manages university student records:

1. **Admin Dashboard (`/dashboard`):**
   - Real-time statistics: Total student headcount directly queried from the database.
   - Database health & synchronization indicator.
   - Table of recently enrolled students with quick links.

2. **Student Directory (`/students?action=list`):**
   - Complete directory of all enrolled students.
   - **Real-Time Search Bar:** Instantly query students by name, roll number, or degree programme.
   - Color-coded badges for fee clearance and programmes.
   - Inline actions: **Edit** and **Delete**.

3. **Add Student Record (`/students?action=new`):**
   - Admission form with client-side & server-side validation.
   - Select degree programme from standard TIET courses (COE, ECE, ELE, MEE, CIE, Biotech, MCA).
   - Assign department, semester (1–8), CGPA (0.00–10.00), and fee status.

4. **Edit Student Record (`/students?action=edit&id=X`):**
   - Pre-fills current data and updates records seamlessly.

5. **Delete Student Record (`/students?action=delete&id=X`):**
   - Safe deletion protected with a confirmation dialog showing the student's name and roll number.

---

## 📂 Project Structure

```text
Student-manage/
├── run.ps1                             # PowerShell one-click compiler & launcher
├── run.bat                             # Windows Command Prompt launcher
├── pom.xml                             # Maven build configuration & dependencies
├── README.md                           # Complete documentation guide (this file)
├── schema.sql                          # Reference database schema
├── data/                               # Persistent database directory
│   ├── student_db.mv.db               # Embedded H2 database file (stores all records)
│   └── student_db.lock.db             # Runtime concurrency lock
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/studentmanagement/
│   │   │       ├── StandaloneAppRunner.java   # HTTP Server, router, session manager & HTML renderer
│   │   │       ├── dao/
│   │   │       │   ├── AdminDAO.java          # Admin credential database queries
│   │   │       │   ├── StudentDAO.java        # Student CRUD operations, search & count queries
│   │   │       │   └── StudentAuthDAO.java    # Student authentication queries
│   │   │       ├── model/
│   │   │       │   ├── Admin.java             # Admin entity POJO
│   │   │       │   └── Student.java           # Student entity POJO
│   │   │       └── util/
│   │   │           └── DBUtil.java            # Database connection, table init & auto-seeding
│   │   ├── resources/
│   │   │   └── db.properties                  # Database connection properties
│   │   └── webapp/
│   │       ├── css/
│   │       │   └── style.css                  # TIET color palette & responsive styles
│   │       └── js/
│   │           └── main.js                    # Client validation, password toggle & confirmation modal
```

---

## 🏛️ Technical Architecture

### 1. HTTP Server & Routing Layer (`StandaloneAppRunner.java`)
- Implements a pure-Java HTTP/1.1 server using `java.net.ServerSocket` and an elastic cached `ExecutorService` thread pool.
- Eliminates the complexity and memory overhead of external servlet containers (Tomcat / Jetty / GlassFish).
- Parses HTTP request methods (`GET`, `POST`), headers, cookies, URL parameters, and POST request bodies.
- Serves static assets (`/css/style.css`, `/js/main.js`) with proper MIME types and caching headers.

### 2. Session Management
- Automatically issues secure `JSESSIONID` cookies upon first connection.
- Stores session state in a thread-safe `ConcurrentHashMap`.
- Enforces strict role-based access control:
  - Student views (`/student-dashboard`, `/student-profile`, `/academic-records`) require an active student session.
  - Admin views (`/dashboard`, `/students`) require an active administrator session.
  - Unauthenticated requests are intercepted and redirected to `/login?error=unauthorized`.

### 3. Data Access Object (DAO) Pattern
- Clean separation of business logic and SQL persistence.
- [`AdminDAO.java`](src/main/java/com/studentmanagement/dao/AdminDAO.java): Authenticates administrators.
- [`StudentAuthDAO.java`](src/main/java/com/studentmanagement/dao/StudentAuthDAO.java): Authenticates students by roll number and email.
- [`StudentDAO.java`](src/main/java/com/studentmanagement/dao/StudentDAO.java): Complete CRUD engine with parameterized SQL queries.

---

## 🗄️ Database & Persistence

### Embedded Mode (Default & Zero Setup)
- The application uses **H2 in MySQL-compatible mode** (`MODE=MySQL;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1`).
- Storage is written to `./data/student_db.mv.db` in the project root.
- All student registrations, updates, and additions **persist permanently across server restarts**.

### Switching to External MySQL (Optional)
If your university lab specifically requires connection to a live MySQL Server:
1. Open [`src/main/resources/db.properties`](src/main/resources/db.properties).
2. Uncomment the MySQL configuration lines and provide your credentials:
   ```properties
   db.url=jdbc:mysql://localhost:3306/student_management?useSSL=false&allowPublicKeyRetrieval=true
   db.username=root
   db.password=your_password
   ```
3. Run `schema.sql` inside your MySQL database.

---

## 🛡️ Security & Validation

- **SQL Injection Prevention:** Every database query in all DAO classes strictly uses `java.sql.PreparedStatement` with parameterized placeholders (`?`).
- **Cross-Site Scripting (XSS) Protection:** All dynamic outputs rendered into HTML pass through the `esc()` sanitizer, converting characters like `<`, `>`, `&`, `"`, and `'` into safe HTML entities.
- **Client-Side Form Validation:** Enforced in `main.js`:
  - Required fields check.
  - Email format validation via regex (`name@domain.edu`).
  - CGPA bound validation (`0.00` to `10.00`).
- **Show/Hide Password Toggle:** Allows users to verify entered passwords before submission.

---

## ❓ Troubleshooting & FAQ

### Q1: The terminal says "Port 8080 already in use" or "Address already in use".
- **Solution:** Another program (or a previously running instance of the portal) is using port 8080.
  - Close any previous command prompt or PowerShell window running the server.
  - Alternatively, change `PORT = 8080;` in `StandaloneAppRunner.java` to `PORT = 8081;` and re-run.

### Q2: "javac is not recognized as an internal or external command".
- **Solution:** Java Development Kit (JDK) is not configured in your system's `PATH`.
  - Install JDK 17 or higher.
  - Add your JDK's `bin` directory (e.g. `C:\Program Files\Java\jdk-17\bin`) to your Windows Environment Variables under `Path`.

### Q3: How do I reset the database to original sample records?
- **Solution:** 
  1. Stop the application (`Ctrl + C`).
  2. Delete the `data` directory:
     ```powershell
     Remove-Item -Path "data" -Recurse -Force
     ```
  3. Re-run `.\run.ps1` or `run.bat`. A clean database with default sample records will be initialized automatically.

---

## 📜 Academic Prototype Disclaimer

> **Notice:** This project is an academic prototype developed for demonstration, educational, and laboratory submission purposes inspired by **Thapar Institute of Engineering & Technology (TIET), Patiala**. It is an independent student project and does not claim to be an official university software application or connected to TIET's internal production servers.
