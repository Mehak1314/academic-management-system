package com.studentmanagement;

import com.studentmanagement.dao.AdminDAO;
import com.studentmanagement.dao.StudentAuthDAO;
import com.studentmanagement.dao.StudentDAO;
import com.studentmanagement.model.Admin;
import com.studentmanagement.model.Student;
import com.studentmanagement.util.DBUtil;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.util.*;
import java.util.concurrent.*;

/**
 * Standalone Application Runner for TIET Student Portal
 * Thapar Institute of Engineering & Technology, Patiala
 * Built using standard ServerSocket (zero external servlet container required).
 */
public class StandaloneAppRunner {

    private static final int DEFAULT_PORT = 8080;
    private static final Map<String, Map<String, Object>> SESSIONS = new ConcurrentHashMap<>();
    private static final ExecutorService THREAD_POOL = Executors.newCachedThreadPool();

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        String envPort = System.getenv("PORT");
        if (envPort != null && !envPort.trim().isEmpty()) {
            try {
                port = Integer.parseInt(envPort.trim());
            } catch (Exception ignored) {}
        }

        System.out.println("==================================================================");
        System.out.println("   TIET Student Portal - Thapar Institute of Engg. & Technology   ");
        System.out.println("   Patiala, Punjab, India | Academic Session 2024-2025            ");
        System.out.println("==================================================================");

        try (Connection conn = DBUtil.getConnection()) {
            System.out.println("[DB] Database connected and schema synchronized successfully.");
            System.out.println("[DB] Storage Location: ./data/student_db");
        } catch (Exception e) {
            System.err.println("[DB Error] " + e.getMessage());
        }

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[Server] Application is live on port " + port + "!");
            System.out.println("👉 Access Portal at: http://localhost:" + port + "/login");
            System.out.println("==================================================================");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                THREAD_POOL.submit(() -> handleClient(clientSocket));
            }
        } catch (Exception e) {
            System.err.println("[Server Error] " + e.getMessage());
        }
    }

    private static void handleClient(Socket socket) {
        try (socket;
             InputStream in = socket.getInputStream();
             OutputStream out = socket.getOutputStream()) {

            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String requestLine = reader.readLine();
            if (requestLine == null || requestLine.isEmpty()) return;

            String[] reqParts = requestLine.split(" ");
            if (reqParts.length < 2) return;
            String method = reqParts[0];
            String fullUri = reqParts[1];

            // Parse headers
            Map<String, String> headers = new HashMap<>();
            String line;
            int contentLength = 0;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                int colon = line.indexOf(':');
                if (colon > 0) {
                    String k = line.substring(0, colon).trim().toLowerCase();
                    String v = line.substring(colon + 1).trim();
                    headers.put(k, v);
                    if ("content-length".equals(k)) {
                        try { contentLength = Integer.parseInt(v); } catch (Exception ignored) {}
                    }
                }
            }

            // Read POST body if present
            String body = "";
            if (contentLength > 0) {
                char[] buf = new char[contentLength];
                int read = 0;
                while (read < contentLength) {
                    int r = reader.read(buf, read, contentLength - read);
                    if (r == -1) break;
                    read += r;
                }
                body = new String(buf, 0, read);
            }

            // Session identification via JSESSIONID cookie
            String cookieHeader = headers.get("cookie");
            String sessionId = null;
            if (cookieHeader != null) {
                for (String c : cookieHeader.split(";")) {
                    String[] kv = c.trim().split("=");
                    if ("JSESSIONID".equalsIgnoreCase(kv[0]) && kv.length > 1) {
                        sessionId = kv[1];
                    }
                }
            }
            boolean isNewSession = false;
            if (sessionId == null || !SESSIONS.containsKey(sessionId)) {
                sessionId = UUID.randomUUID().toString();
                SESSIONS.put(sessionId, new ConcurrentHashMap<>());
                isNewSession = true;
            }
            Map<String, Object> session = SESSIONS.get(sessionId);

            // Separate path and query string
            String path = fullUri;
            String query = "";
            int qIdx = fullUri.indexOf('?');
            if (qIdx >= 0) {
                path = fullUri.substring(0, qIdx);
                query = fullUri.substring(qIdx + 1);
            }

            // Static assets: CSS and JS
            if (path.startsWith("/css/") || path.startsWith("/js/")) {
                serveFile(out, path, isNewSession ? sessionId : null);
                return;
            }

            // Root route redirection
            if (path.equals("/") || path.equals("/index.jsp")) {
                if (session.get("admin") != null) {
                    sendRedirect(out, "/dashboard", isNewSession ? sessionId : null);
                } else if (session.get("loggedStudent") != null) {
                    sendRedirect(out, "/student-dashboard", isNewSession ? sessionId : null);
                } else {
                    sendRedirect(out, "/login", isNewSession ? sessionId : null);
                }
                return;
            }

            // Logout route
            if (path.equals("/logout")) {
                session.clear();
                sendRedirect(out, "/login?message=logged_out", isNewSession ? sessionId : null);
                return;
            }

            AdminDAO adminDAO = new AdminDAO();
            StudentAuthDAO studentAuthDAO = new StudentAuthDAO();
            StudentDAO studentDAO = new StudentDAO();

            // Self-Registration Route for Students
            if (path.equals("/register")) {
                if ("POST".equalsIgnoreCase(method)) {
                    Map<String, String> form = parseParams(body);
                    Student s = new Student(
                        form.get("rollNumber"), form.get("name"), form.get("email"),
                        form.get("phone"), form.get("department"), form.get("semester"),
                        form.get("course"), 8.00, "Pending"
                    );
                    try {
                        if (studentDAO.selectStudentByRollNumber(s.getRollNumber()) != null) {
                            sendHtml(out, 200, renderRegisterHtml("Roll Number '" + s.getRollNumber() + "' is already registered in the TIET system.", s), isNewSession ? sessionId : null);
                            return;
                        }
                        studentDAO.insertStudent(s);
                        sendRedirect(out, "/login?role=student&message=registered", isNewSession ? sessionId : null);
                        return;
                    } catch (Exception e) {
                        sendHtml(out, 500, renderRegisterHtml("Enrollment Error: " + e.getMessage(), s), isNewSession ? sessionId : null);
                        return;
                    }
                } else {
                    sendHtml(out, 200, renderRegisterHtml(null, null), isNewSession ? sessionId : null);
                    return;
                }
            }

            // Login Route
            if (path.equals("/login")) {
                Map<String, String> qMap = parseParams(query);
                String roleParam = qMap.getOrDefault("role", "student");

                if ("POST".equalsIgnoreCase(method)) {
                    Map<String, String> form = parseParams(body);
                    String role = form.getOrDefault("role", "student");
                    String u = form.get("username");
                    String p = form.get("password");

                    try {
                        if ("student".equalsIgnoreCase(role)) {
                            Student s = studentAuthDAO.authenticateStudent(u, p);
                            if (s != null) {
                                session.put("loggedStudent", s);
                                sendRedirect(out, "/student-dashboard", isNewSession ? sessionId : null);
                                return;
                            } else {
                                sendHtml(out, 200, renderLoginHtml("Invalid credentials. Please verify your Roll Number and registered email.", role, u, null), isNewSession ? sessionId : null);
                                return;
                            }
                        } else {
                            Admin a = adminDAO.validateAdmin(u, p);
                            if (a != null) {
                                session.put("admin", a.getUsername());
                                sendRedirect(out, "/dashboard", isNewSession ? sessionId : null);
                                return;
                            } else {
                                sendHtml(out, 200, renderLoginHtml("Invalid administrative username or password.", role, u, null), isNewSession ? sessionId : null);
                                return;
                            }
                        }
                    } catch (Exception e) {
                        sendHtml(out, 500, renderLoginHtml("System Database error: " + e.getMessage(), role, u, null), isNewSession ? sessionId : null);
                        return;
                    }
                } else {
                    String msg = null;
                    if (query.contains("message=logged_out")) msg = "You have been securely signed out from the TIET portal.";
                    else if (query.contains("message=registered")) msg = "Admission profile registered successfully. You may now sign in.";
                    else if (query.contains("error=unauthorized")) msg = "Authentication required. Please log in to access this portal section.";
                    sendHtml(out, 200, renderLoginHtml(null, roleParam, null, msg), isNewSession ? sessionId : null);
                    return;
                }
            }

            // -------------------------------------------------------------
            // STUDENT PORTAL ROUTES (Gatekeeper: loggedStudent required)
            // -------------------------------------------------------------
            if (path.equals("/student-dashboard") || path.equals("/student-profile") || path.equals("/academic-records") || path.equals("/pay-fee")) {
                Student s = (Student) session.get("loggedStudent");
                if (s == null) {
                    sendRedirect(out, "/login?role=student&error=unauthorized", isNewSession ? sessionId : null);
                    return;
                }

                // Handle Online Fee Payment by Student
                if (path.equals("/pay-fee")) {
                    try {
                        studentDAO.updateFeeStatus(s.getId(), "Paid");
                        s.setFeeStatus("Paid");
                        session.put("loggedStudent", s);
                        session.put("studentFlashSuccess", "Fee payment of ₹1,60,000 successfully processed! Transaction ID: #TIET-PAY-" + (System.currentTimeMillis() % 1000000) + ". Your semester fee status is now Fully Paid.");
                    } catch (Exception e) {
                        session.put("studentFlashError", "Payment processing error: " + e.getMessage());
                    }
                    sendRedirect(out, "/student-dashboard", isNewSession ? sessionId : null);
                    return;
                }

                // Refresh data from database
                try {
                    Student fresh = studentDAO.selectStudent(s.getId());
                    if (fresh != null) {
                        s = fresh;
                        session.put("loggedStudent", fresh);
                    }
                } catch (Exception ignored) {}

                if (path.equals("/student-dashboard")) {
                    String flashSuccess = (String) session.remove("studentFlashSuccess");
                    String flashError = (String) session.remove("studentFlashError");
                    sendHtml(out, 200, renderStudentDashboardHtml(s, flashSuccess, flashError), isNewSession ? sessionId : null);
                    return;
                } else if (path.equals("/student-profile")) {
                    sendHtml(out, 200, renderStudentProfileHtml(s), isNewSession ? sessionId : null);
                    return;
                } else if (path.equals("/academic-records")) {
                    sendHtml(out, 200, renderAcademicRecordsHtml(s), isNewSession ? sessionId : null);
                    return;
                }
            }

            // -------------------------------------------------------------
            // ADMINISTRATIVE ROUTES (Gatekeeper: admin session required)
            // -------------------------------------------------------------
            if (session.get("admin") == null) {
                sendRedirect(out, "/login?role=admin&error=unauthorized", isNewSession ? sessionId : null);
                return;
            }
            String adminUser = (String) session.get("admin");

            // Route: Admin Dashboard
            if (path.equals("/dashboard")) {
                try {
                    int count = studentDAO.getStudentCount();
                    List<Student> recent = studentDAO.selectAllStudents();
                    if (recent.size() > 5) {
                        recent = recent.subList(0, 5);
                    }
                    String flash = (String) session.remove("flashSuccess");
                    sendHtml(out, 200, renderAdminDashboardHtml(adminUser, count, recent, flash), isNewSession ? sessionId : null);
                } catch (Exception e) {
                    sendHtml(out, 500, "Dashboard Error: " + e.getMessage(), null);
                }
                return;
            }

            // Route: Admin Student CRUD
            if (path.equals("/students")) {
                Map<String, String> qMap = parseParams(query);
                String action = qMap.getOrDefault("action", "list");

                if ("POST".equalsIgnoreCase(method)) {
                    Map<String, String> form = parseParams(body);
                    String postAction = form.getOrDefault("action", "insert");
                    try {
                        double cgpa = 8.00;
                        try {
                            if (form.get("cgpa") != null) cgpa = Double.parseDouble(form.get("cgpa"));
                        } catch (Exception ignored) {}

                        if ("insert".equals(postAction)) {
                            Student s = new Student(
                                form.get("rollNumber"), form.get("name"), form.get("email"),
                                form.get("phone"), form.get("department"), form.get("semester"),
                                form.get("course"), cgpa, form.get("feeStatus")
                            );
                            if (studentDAO.selectStudentByRollNumber(s.getRollNumber()) != null) {
                                sendHtml(out, 200, renderAddStudentHtml(adminUser, "Roll Number '" + s.getRollNumber() + "' already exists in database.", s), isNewSession ? sessionId : null);
                                return;
                            }
                            studentDAO.insertStudent(s);
                            session.put("flashSuccess", "Student record for '" + s.getName() + "' (" + s.getRollNumber() + ") added successfully.");
                            sendRedirect(out, "/students?action=list", isNewSession ? sessionId : null);
                            return;
                        } else if ("update".equals(postAction)) {
                            int id = Integer.parseInt(form.get("id"));
                            Student s = new Student(
                                id, form.get("rollNumber"), form.get("name"), form.get("email"),
                                form.get("phone"), form.get("department"), form.get("semester"),
                                form.get("course"), cgpa, form.get("feeStatus")
                            );
                            Student existing = studentDAO.selectStudentByRollNumber(s.getRollNumber());
                            if (existing != null && existing.getId() != id) {
                                sendHtml(out, 200, renderEditStudentHtml(adminUser, "Roll Number is already allocated to another student.", s), isNewSession ? sessionId : null);
                                return;
                            }
                            studentDAO.updateStudent(s);
                            session.put("flashSuccess", "Student record for '" + s.getName() + "' updated successfully.");
                            sendRedirect(out, "/students?action=list", isNewSession ? sessionId : null);
                            return;
                        }
                    } catch (Exception e) {
                        sendHtml(out, 500, "Update Error: " + e.getMessage(), null);
                        return;
                    }
                } else {
                    try {
                        if ("new".equals(action)) {
                            sendHtml(out, 200, renderAddStudentHtml(adminUser, null, null), isNewSession ? sessionId : null);
                        } else if ("edit".equals(action)) {
                            int id = Integer.parseInt(qMap.get("id"));
                            Student s = studentDAO.selectStudent(id);
                            sendHtml(out, 200, renderEditStudentHtml(adminUser, null, s), isNewSession ? sessionId : null);
                        } else if ("delete".equals(action)) {
                            int id = Integer.parseInt(qMap.get("id"));
                            studentDAO.deleteStudent(id);
                            session.put("flashSuccess", "Student record successfully deleted from the directory.");
                            sendRedirect(out, "/students?action=list", isNewSession ? sessionId : null);
                        } else if ("payFee".equals(action)) {
                            int id = Integer.parseInt(qMap.get("id"));
                            Student s = studentDAO.selectStudent(id);
                            if (s != null) {
                                studentDAO.updateFeeStatus(id, "Paid");
                                session.put("flashSuccess", "Fee payment of ₹1,60,000 recorded as 'Paid' for " + s.getName() + " (" + s.getRollNumber() + ").");
                            }
                            String from = qMap.getOrDefault("from", "dashboard");
                            if ("list".equalsIgnoreCase(from)) {
                                sendRedirect(out, "/students?action=list", isNewSession ? sessionId : null);
                            } else {
                                sendRedirect(out, "/dashboard", isNewSession ? sessionId : null);
                            }
                            return;
                        } else if ("search".equals(action)) {
                            String kw = qMap.getOrDefault("keyword", "");
                            List<Student> list = studentDAO.searchStudents(kw);
                            sendHtml(out, 200, renderViewStudentsHtml(adminUser, list, kw, null), isNewSession ? sessionId : null);
                        } else {
                            String flash = (String) session.remove("flashSuccess");
                            List<Student> list = studentDAO.selectAllStudents();
                            sendHtml(out, 200, renderViewStudentsHtml(adminUser, list, "", flash), isNewSession ? sessionId : null);
                        }
                    } catch (Exception e) {
                        sendHtml(out, 500, "Operation Error: " + e.getMessage(), null);
                    }
                    return;
                }
            }

            sendHtml(out, 404, "<h3>404 Not Found</h3><p>The requested portal resource does not exist.</p>", null);

        } catch (Exception ignored) {
            // connection dropped or closed
        }
    }

    // =========================================================================
    // HTML RENDERING METHODS (TIET University Branding & Visual Style)
    // =========================================================================

    /**
     * University Branded Login Page
     */
    private static String renderLoginHtml(String error, String role, String username, String info) {
        boolean isStudent = !"admin".equalsIgnoreCase(role);
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        sb.append("<title>TIET Student Portal - Thapar Institute of Engineering &amp; Technology</title>");
        sb.append("<link rel='stylesheet' href='/css/style.css'>");
        sb.append("<script src='/js/main.js'></script></head><body class='login-body'>");

        sb.append("<div class='login-wrapper'><div class='login-box'>");
        sb.append("<div class='login-header'>");
        sb.append("<div class='login-crest'>TIET</div>");
        sb.append("<h1>TIET Student Portal</h1>");
        sb.append("<div class='subtitle'>Thapar Institute of Engg. &amp; Tech., Patiala</div>");
        sb.append("<div class='tagline'>Your Academic Journey, Simplified.</div>");
        sb.append("</div>");

        // Role Switcher Tabs
        sb.append("<div class='role-tabs'>");
        sb.append("<button type='button' id='studentTab' class='role-tab ").append(isStudent ? "active" : "").append("' onclick=\"switchLoginRole('student');\">&#127891; Student Portal</button>");
        sb.append("<button type='button' id='adminTab' class='role-tab ").append(!isStudent ? "active" : "").append("' onclick=\"switchLoginRole('admin');\">&#128273; Administrative Portal</button>");
        sb.append("</div>");

        sb.append("<div class='login-form-body'>");
        if (error != null) sb.append("<div class='alert alert-error'><span>&#9888;</span> ").append(esc(error)).append("</div>");
        if (info != null) sb.append("<div class='alert alert-success'><span>&#10004;</span> ").append(esc(info)).append("</div>");

        sb.append("<form action='/login' method='post'>");
        sb.append("<input type='hidden' id='loginRole' name='role' value='").append(isStudent ? "student" : "admin").append("'>");

        sb.append("<div class='form-group'><label class='form-label' id='usernameLabel'>").append(isStudent ? "Roll Number / Registration ID" : "Admin Username").append(" <span class='required'>*</span></label>");
        sb.append("<input type='text' id='usernameInput' name='username' class='form-control' placeholder='").append(isStudent ? "Enter roll number (e.g. 102103045 / CS202401)" : "Enter administrator username").append("' value='").append(username != null ? esc(username) : "").append("' required autofocus></div>");

        sb.append("<div class='form-group'><label class='form-label' id='passwordLabel'>").append(isStudent ? "Registered Email / Security Key" : "Security Password").append(" <span class='required'>*</span></label>");
        sb.append("<div class='input-with-action'>");
        sb.append("<input type='").append(isStudent ? "text" : "password").append("' id='passwordInput' name='password' class='form-control' placeholder='").append(isStudent ? "Enter registered email address" : "Enter password").append("' required>");
        sb.append("<button type='button' id='pwdToggleBtn' class='btn-toggle-pwd' style='").append(isStudent ? "display:none;" : "").append("' onclick=\"togglePasswordVisibility('passwordInput', this);\" title='Toggle visibility'>&#128064;</button>");
        sb.append("</div></div>");

        sb.append("<button type='submit' class='btn btn-primary' style='width: 100%; padding: 11px; margin-top: 6px;'>Sign In to Portal &rarr;</button>");
        sb.append("</form>");

        // Self Registration Option
        sb.append("<div id='studentRegisterBox' style='text-align:center; margin-top: 18px; font-size: 13px; color: var(--text-secondary); ").append(!isStudent ? "display:none;" : "").append("'>");
        sb.append("New admission student? <a href='/register' style='color: var(--primary-red); font-weight: 600; text-decoration: none;'>Self-Enroll / Sign Up &rarr;</a>");
        sb.append("</div>");

        sb.append("<div style='margin-top: 24px; padding-top: 16px; border-top: 1px solid var(--border-color); text-align: center; font-size: 11.5px; color: var(--text-muted);'>");
        sb.append("Academic Prototype &bull; Inspired by TIET Patiala &bull; Est. 1956");
        sb.append("</div>");

        sb.append("</div></div></div></body></html>");
        return sb.toString();
    }

    /**
     * Student Self Registration Page
     */
    private static String renderRegisterHtml(String error, Student s) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        sb.append("<title>Student Self-Enrollment - TIET Student Portal</title><link rel='stylesheet' href='/css/style.css'>");
        sb.append("<script src='/js/main.js'></script></head><body class='login-body'>");

        sb.append("<div class='login-wrapper' style='max-width: 620px;'><div class='login-box'>");
        sb.append("<div class='login-header'>");
        sb.append("<div class='login-crest'>TIET</div>");
        sb.append("<h1>New Student Admission</h1>");
        sb.append("<div class='subtitle'>Thapar Institute of Engg. &amp; Tech., Patiala</div>");
        sb.append("<div class='tagline'>Register your academic profile into the institutional repository</div>");
        sb.append("</div>");

        sb.append("<div class='login-form-body'>");
        if (error != null) sb.append("<div class='alert alert-error'><span>&#9888;</span> ").append(esc(error)).append("</div>");

        sb.append("<form action='/register' method='post' onsubmit='return validateStudentForm(this);'>");
        sb.append("<div class='form-grid'>");
        sb.append("<div class='form-group'><label class='form-label'>Roll Number / Registration No. <span class='required'>*</span></label><input type='text' name='rollNumber' class='form-control' placeholder='e.g. 102103045' value='").append(s != null ? esc(s.getRollNumber()) : "").append("' required autofocus></div>");
        sb.append("<div class='form-group'><label class='form-label'>Full Legal Name <span class='required'>*</span></label><input type='text' name='name' class='form-control' placeholder='e.g. Gurpreet Singh' value='").append(s != null ? esc(s.getName()) : "").append("' required></div>");
        sb.append("<div class='form-group'><label class='form-label'>Official Email Address <span class='required'>*</span></label><input type='email' name='email' class='form-control' placeholder='e.g. gsingh@thapar.edu' value='").append(s != null ? esc(s.getEmail()) : "").append("' required></div>");
        sb.append("<div class='form-group'><label class='form-label'>Contact Phone Number <span class='required'>*</span></label><input type='text' name='phone' class='form-control' placeholder='e.g. 9876543210' value='").append(s != null ? esc(s.getPhone()) : "").append("' required></div>");

        sb.append("<div class='form-group'><label class='form-label'>Degree Programme <span class='required'>*</span></label><select name='course' class='form-control' required>");
        sb.append("<option value=''>-- Select Enrolled Programme --</option>");
        String[] programmes = {
            "B.E. Computer Engineering",
            "B.E. Computer Science and Business Systems",
            "B.E. Electronics & Communication Engg.",
            "B.E. Electrical Engineering",
            "B.E. Mechanical Engineering",
            "B.E. Civil Engineering",
            "B.E. Biotechnology",
            "MCA / M.Tech Computer Science"
        };
        for (String p : programmes) {
            String sel = (s != null && p.equalsIgnoreCase(s.getCourse())) ? "selected" : "";
            sb.append("<option value='").append(p).append("' ").append(sel).append(">").append(p).append("</option>");
        }
        sb.append("</select></div>");

        sb.append("<div class='form-group'><label class='form-label'>Academic Department <span class='required'>*</span></label><select name='department' class='form-control' required>");
        sb.append("<option value=''>-- Select Department --</option>");
        String[] depts = {
            "Computer Science & Engineering",
            "Electronics & Communication Engg.",
            "Electrical & Instrumentation Engg.",
            "Mechanical Engineering",
            "Civil Engineering",
            "Biotechnology & Chemical Engg."
        };
        for (String d : depts) {
            String sel = (s != null && d.equalsIgnoreCase(s.getDepartment())) ? "selected" : "";
            sb.append("<option value='").append(d).append("' ").append(sel).append(">").append(d).append("</option>");
        }
        sb.append("</select></div>");

        sb.append("<div class='form-group full-width'><label class='form-label'>Current Academic Semester <span class='required'>*</span></label><select name='semester' class='form-control' required>");
        sb.append("<option value=''>-- Select Semester --</option>");
        for (int i = 1; i <= 8; i++) {
            String sem = "Semester " + i;
            String sel = (s != null && sem.equalsIgnoreCase(s.getSemester())) ? "selected" : "";
            sb.append("<option value='").append(sem).append("' ").append(sel).append(">").append(sem).append("</option>");
        }
        sb.append("</select></div></div>");

        sb.append("<button type='submit' class='btn btn-primary' style='width: 100%; padding: 11px; margin-top: 10px;'>Submit Admission Enrollment &rarr;</button>");
        sb.append("</form>");

        sb.append("<div style='text-align:center; margin-top: 18px; font-size: 13px; color: var(--text-secondary);'>");
        sb.append("Already enrolled? <a href='/login?role=student' style='color: var(--primary-red); font-weight: 600; text-decoration: none;'>Sign In to Student Portal</a>");
        sb.append("</div>");

        sb.append("</div></div></div></body></html>");
        return sb.toString();
    }

    /**
     * Student Dashboard (Primary Student Landing View)
     */
    private static String renderStudentDashboardHtml(Student s, String flashSuccess, String flashError) {
        StringBuilder sb = new StringBuilder();

        // Flash Notifications
        if (flashSuccess != null) {
            sb.append("<div class='alert alert-success'><span>&#10004;</span> ").append(esc(flashSuccess)).append("</div>");
        }
        if (flashError != null) {
            sb.append("<div class='alert alert-error'><span>&#9888;</span> ").append(esc(flashError)).append("</div>");
        }

        // 1. Welcome Banner
        sb.append("<div class='academic-banner'>");
        sb.append("<div class='banner-content'>");
        sb.append("<h2>Welcome back, ").append(esc(s.getName())).append("</h2>");
        sb.append("<p>Roll No: <strong>").append(esc(s.getRollNumber())).append("</strong> &bull; ").append(esc(s.getCourse())).append(" &bull; ").append(esc(s.getSemester())).append("</p>");
        sb.append("</div>");
        sb.append("<div class='banner-badge'>&#127891; TIET Session 2024-2025</div>");
        sb.append("</div>");

        // 2. Metrics / KPI Grid using Actual Database Fields
        sb.append("<div class='stats-grid'>");

        // Cumulative GPA
        sb.append("<div class='stat-box'>");
        sb.append("<div class='stat-info'>");
        sb.append("<h4>Cumulative GPA (CGPA)</h4>");
        sb.append("<div class='stat-value' style='color: var(--primary-red);'>").append(String.format(Locale.US, "%.2f", s.getCgpa())).append("</div>");
        sb.append("<div class='stat-sub'>TIET 10-point scale on record</div>");
        sb.append("</div>");
        sb.append("<div class='stat-icon'>&#128200;</div>");
        sb.append("</div>");

        // Current Semester
        sb.append("<div class='stat-box'>");
        sb.append("<div class='stat-info'>");
        sb.append("<h4>Enrolled Semester</h4>");
        sb.append("<div class='stat-value'>").append(esc(s.getSemester())).append("</div>");
        sb.append("<div class='stat-sub'>Regular Academic Term</div>");
        sb.append("</div>");
        sb.append("<div class='stat-icon'>&#128197;</div>");
        sb.append("</div>");

        // Fee Clearance Status
        String feeClass = "Paid".equalsIgnoreCase(s.getFeeStatus()) ? "badge-paid" : ("Partial".equalsIgnoreCase(s.getFeeStatus()) ? "badge-partial" : "badge-pending");
        sb.append("<div class='stat-box'>");
        sb.append("<div class='stat-info'>");
        sb.append("<h4>Semester Fee Status</h4>");
        sb.append("<div class='stat-value' style='font-size: 20px;'><span class='badge-tag ").append(feeClass).append("'>").append(esc(s.getFeeStatus())).append("</span></div>");
        sb.append("<div class='stat-sub'>Finance &amp; Accounts Section</div>");
        sb.append("</div>");
        sb.append("<div class='stat-icon'>&#128179;</div>");
        sb.append("</div>");

        // Academic Standing
        sb.append("<div class='stat-box'>");
        sb.append("<div class='stat-info'>");
        sb.append("<h4>Enrollment Status</h4>");
        sb.append("<div class='stat-value' style='font-size: 20px; color: #065F46;'><span class='badge-tag badge-paid'>Active &bull; Good Standing</span></div>");
        sb.append("<div class='stat-sub'>Registered Full-Time Student</div>");
        sb.append("</div>");
        sb.append("<div class='stat-icon'>&#9989;</div>");
        sb.append("</div>");

        sb.append("</div>"); // stats-grid

        // 3. Semester Fee Assessment & Online Payment Table (with "Fees Pay" Column)
        boolean isPaid = "Paid".equalsIgnoreCase(s.getFeeStatus());
        sb.append("<div class='card'>");
        sb.append("<div class='card-header'>");
        sb.append("<h3>&#128179; Semester Fee Assessment &amp; Payment Status</h3>");
        sb.append("<span class='badge-tag ").append(feeClass).append("'>Status: ").append(esc(s.getFeeStatus())).append("</span>");
        sb.append("</div>");
        sb.append("<div class='card-body' style='padding: 0;'>");
        sb.append("<div class='table-responsive'><table class='data-table'>");
        sb.append("<thead><tr><th>Fee Component</th><th>Term</th><th>Due Date</th><th>Fee Amount</th><th>Status</th><th style='text-align: center;'>Fees Pay</th></tr></thead>");
        sb.append("<tbody>");

        // Component 1
        sb.append("<tr>");
        sb.append("<td><strong>Academic Tuition &amp; Laboratory Fee</strong><div style='font-size: 11.5px; color: var(--text-secondary);'>Core instructional, laboratory and computing facility charges</div></td>");
        sb.append("<td>").append(esc(s.getSemester())).append("</td>");
        sb.append("<td>15-Dec-2024</td>");
        sb.append("<td><strong>₹1,45,000</strong></td>");
        sb.append("<td><span class='badge-tag ").append(feeClass).append("'>").append(esc(s.getFeeStatus())).append("</span></td>");
        sb.append("<td style='text-align: center;'>");
        if (isPaid) {
            sb.append("<span class='badge-tag badge-paid'>&#10004; Fully Paid</span>");
        } else {
            sb.append("<a href='/pay-fee' class='btn btn-primary btn-sm' onclick=\"return confirm('Proceed to pay tuition fee of ₹1,45,000 online via TIET Payment Gateway?');\">&#128179; Pay Fees Online</a>");
        }
        sb.append("</td></tr>");

        // Component 2
        sb.append("<tr>");
        sb.append("<td><strong>University Examination &amp; Library Development Fee</strong><div style='font-size: 11.5px; color: var(--text-secondary);'>Central library digital access and semester assessment levy</div></td>");
        sb.append("<td>").append(esc(s.getSemester())).append("</td>");
        sb.append("<td>15-Dec-2024</td>");
        sb.append("<td><strong>₹15,000</strong></td>");
        sb.append("<td><span class='badge-tag ").append(feeClass).append("'>").append(esc(s.getFeeStatus())).append("</span></td>");
        sb.append("<td style='text-align: center;'>");
        if (isPaid) {
            sb.append("<span class='badge-tag badge-paid'>&#10004; Fully Paid</span>");
        } else {
            sb.append("<a href='/pay-fee' class='btn btn-primary btn-sm' onclick=\"return confirm('Proceed to complete examination fee payment of ₹15,000?');\">&#128179; Pay Fees Online</a>");
        }
        sb.append("</td></tr>");

        sb.append("</tbody></table></div>");
        sb.append("</div></div>");

        // 4. Quick Navigation Grid
        sb.append("<div class='quick-nav-grid'>");

        sb.append("<a href='/student-profile' class='quick-nav-card'>");
        sb.append("<div>");
        sb.append("<h4>&#128100; My Academic Profile</h4>");
        sb.append("<p>Inspect your personal records, registered department, primary email, contact phone number and verified university credentials.</p>");
        sb.append("</div>");
        sb.append("<div class='action-label'>Open Profile Details &rarr;</div>");
        sb.append("</a>");

        sb.append("<a href='/academic-records' class='quick-nav-card'>");
        sb.append("<div>");
        sb.append("<h4>&#128220; Academic Records &amp; Grades</h4>");
        sb.append("<p>Review your semester-wise academic standing, cumulative CGPA evaluation, grading scale standards, and course curriculum.</p>");
        sb.append("</div>");
        sb.append("<div class='action-label'>View Academic Records &rarr;</div>");
        sb.append("</a>");

        sb.append("<div class='quick-nav-card' style='cursor: default;'>");
        sb.append("<div>");
        sb.append("<h4>&#127891; Thapar Academic Desk</h4>");
        sb.append("<p>Office of the Dean of Academic Affairs (DOAA), Thapar Institute of Engineering &amp; Technology, Bhadson Road, Patiala, Punjab.</p>");
        sb.append("</div>");
        sb.append("<div style='font-size: 11.5px; color: var(--text-secondary);'>Official Academic Support: doaa@thapar.edu</div>");
        sb.append("</div>");

        sb.append("</div>"); // quick-nav-grid

        // 4. University Notice Board Card
        sb.append("<div class='card'>");
        sb.append("<div class='card-header'>");
        sb.append("<h3>&#128227; Official Notices &amp; Academic Updates</h3>");
        sb.append("<span class='badge-tag'>Notice Board 2024</span>");
        sb.append("</div>");
        sb.append("<div class='card-body' style='padding: 0;'>");
        sb.append("<div class='table-responsive'><table class='data-table'>");
        sb.append("<thead><tr><th>Date</th><th>Subject / Circular</th><th>Department</th><th>Status</th></tr></thead>");
        sb.append("<tbody>");
        sb.append("<tr><td>12-Nov-2024</td><td><strong>End-Semester Examination (ESE) Schedule Notification</strong><div style='font-size: 12px; color: var(--text-secondary);'>Consult your academic timetable on WebKiosk / COE portal.</div></td><td>Controller of Examinations</td><td><span class='badge-tag'>Active</span></td></tr>");
        sb.append("<tr><td>04-Nov-2024</td><td><strong>Fee Payment Verification &amp; Semester Clearance</strong><div style='font-size: 12px; color: var(--text-secondary);'>Final date for partial fee settlement at the University Accounts Wing.</div></td><td>Finance &amp; Accounts</td><td><span class='badge-tag'>Urgent</span></td></tr>");
        sb.append("<tr><td>28-Oct-2024</td><td><strong>Capstone Project &amp; Elective Course Advisory</strong><div style='font-size: 12px; color: var(--text-secondary);'>Departmental elective choices confirmation for next academic term.</div></td><td>Academic Affairs</td><td><span class='badge-tag'>Information</span></td></tr>");
        sb.append("</tbody></table></div>");
        sb.append("</div></div>");

        return renderStudentLayoutHtml(s, "Student Academic Dashboard", "dashboard", sb.toString());
    }

    /**
     * My Profile View
     */
    private static String renderStudentProfileHtml(Student s) {
        StringBuilder sb = new StringBuilder();

        sb.append("<div class='profile-card'>");
        sb.append("<div class='profile-banner'></div>");
        sb.append("<div class='profile-header-content'>");
        sb.append("<div class='profile-avatar-large'>&#127891;</div>");
        sb.append("<div class='profile-names'>");
        sb.append("<h2>").append(esc(s.getName())).append("</h2>");
        sb.append("<p>").append(esc(s.getCourse())).append(" &bull; ").append(esc(s.getDepartment())).append("</p>");
        sb.append("</div></div>");

        // Profile Attributes Grid
        sb.append("<div class='profile-info-grid'>");

        sb.append("<div class='info-item'><span class='info-label'>Roll Number / Reg ID</span><span class='info-value'>").append(esc(s.getRollNumber())).append("</span></div>");
        sb.append("<div class='info-item'><span class='info-label'>Enrolled Degree Programme</span><span class='info-value' style='color: var(--primary-red);'>").append(esc(s.getCourse())).append("</span></div>");
        sb.append("<div class='info-item'><span class='info-label'>Academic Department</span><span class='info-value'>").append(esc(s.getDepartment())).append("</span></div>");
        sb.append("<div class='info-item'><span class='info-label'>Current Semester</span><span class='info-value'>").append(esc(s.getSemester())).append("</span></div>");

        sb.append("<div class='info-item'><span class='info-label'>Official Student Email</span><span class='info-value'>").append(esc(s.getEmail())).append("</span></div>");
        sb.append("<div class='info-item'><span class='info-label'>Contact Phone Number</span><span class='info-value'>").append(esc(s.getPhone())).append("</span></div>");
        sb.append("<div class='info-item'><span class='info-label'>Cumulative GPA (CGPA)</span><span class='info-value' style='color: var(--primary-red);'>").append(String.format(Locale.US, "%.2f", s.getCgpa())).append(" / 10.00</span></div>");

        String feeClass = "Paid".equalsIgnoreCase(s.getFeeStatus()) ? "badge-paid" : ("Partial".equalsIgnoreCase(s.getFeeStatus()) ? "badge-partial" : "badge-pending");
        sb.append("<div class='info-item'><span class='info-label'>Fee Clearance Status</span><span class='info-value'><span class='badge-tag ").append(feeClass).append("'>").append(esc(s.getFeeStatus())).append("</span></span></div>");

        sb.append("</div></div>");

        // Enrollment & Academic Verification Details
        sb.append("<div class='card'>");
        sb.append("<div class='card-header'>");
        sb.append("<h3>&#128221; Institutional Enrollment Information</h3>");
        sb.append("<span class='badge-tag badge-paid'>Verified TIET Academic Record</span>");
        sb.append("</div>");
        sb.append("<div class='card-body' style='line-height: 1.7; color: var(--text-secondary);'>");
        sb.append("<p>Student <strong>").append(esc(s.getName())).append("</strong> is enrolled as a bona fide student in the <strong>").append(esc(s.getCourse())).append("</strong> programme under the <strong>").append(esc(s.getDepartment())).append("</strong>, Thapar Institute of Engineering &amp; Technology, Patiala.</p>");
        sb.append("<div style='margin-top: 16px; padding: 14px; background: #FAFAFA; border: 1px solid var(--border-color); border-radius: var(--radius-sm); font-size: 13px;'>");
        sb.append("<strong>Note:</strong> Official transcript issuances, grade revisions, and certificate clearances are managed through the Office of the Registrar and WebKiosk. For any demographic corrections, contact the Departmental Academic Coordinator.");
        sb.append("</div>");
        sb.append("</div></div>");

        return renderStudentLayoutHtml(s, "My Profile", "profile", sb.toString());
    }

    /**
     * Academic Records & Semester-Wise Evaluation View
     */
    private static String renderAcademicRecordsHtml(Student s) {
        StringBuilder sb = new StringBuilder();

        // 1. Performance Summary Card
        sb.append("<div class='card'>");
        sb.append("<div class='card-header'>");
        sb.append("<h3>&#128202; Cumulative Academic Performance</h3>");
        sb.append("<span class='badge-tag'>TIET 10-Point Evaluation Scale</span>");
        sb.append("</div>");
        sb.append("<div class='card-body'>");
        sb.append("<div class='stats-grid' style='margin-bottom: 0;'>");

        sb.append("<div class='stat-box'>");
        sb.append("<div class='stat-info'>");
        sb.append("<h4>Cumulative GPA (CGPA)</h4>");
        sb.append("<div class='stat-value' style='color: var(--primary-red);'>").append(String.format(Locale.US, "%.2f", s.getCgpa())).append("</div>");
        sb.append("<div class='stat-sub'>Overall scale: 10.00</div>");
        sb.append("</div>");
        sb.append("<div class='stat-icon'>&#127891;</div>");
        sb.append("</div>");

        sb.append("<div class='stat-box'>");
        sb.append("<div class='stat-info'>");
        sb.append("<h4>Academic Standing</h4>");
        sb.append("<div class='stat-value' style='font-size: 20px; color: #065F46;'><span class='badge-tag badge-paid'>Good Standing</span></div>");
        sb.append("<div class='stat-sub'>No active academic warnings</div>");
        sb.append("</div>");
        sb.append("<div class='stat-icon'>&#9989;</div>");
        sb.append("</div>");

        sb.append("<div class='stat-box'>");
        sb.append("<div class='stat-info'>");
        sb.append("<h4>Current Registration</h4>");
        sb.append("<div class='stat-value'>").append(esc(s.getSemester())).append("</div>");
        sb.append("<div class='stat-sub'>Term: July - Dec 2024</div>");
        sb.append("</div>");
        sb.append("<div class='stat-icon'>&#128218;</div>");
        sb.append("</div>");

        sb.append("</div></div></div>");

        // 2. Registered Course Curriculum Table for Current Semester
        sb.append("<div class='card'>");
        sb.append("<div class='card-header'>");
        sb.append("<h3>&#128218; Registered Courses &bull; ").append(esc(s.getSemester())).append("</h3>");
        sb.append("<span class='badge-tag'>Session 2024-2025</span>");
        sb.append("</div>");
        sb.append("<div class='card-body' style='padding: 0;'>");
        sb.append("<div class='table-responsive'><table class='data-table'>");
        sb.append("<thead><tr><th>Course Code</th><th>Course Title</th><th>Type</th><th>Credits</th><th>Evaluation Status</th></tr></thead>");
        sb.append("<tbody>");

        // Display standard TIET curriculum courses according to department / semester
        sb.append("<tr><td><strong>UCS503</strong></td><td>Software Engineering &amp; Object Oriented Design</td><td>Core Theory</td><td>4.0</td><td><span class='badge-tag badge-paid'>Graded / Completed</span></td></tr>");
        sb.append("<tr><td><strong>UCS505</strong></td><td>Computer Networks &amp; Protocols</td><td>Core Theory + Lab</td><td>4.5</td><td><span class='badge-tag badge-paid'>Graded / Completed</span></td></tr>");
        sb.append("<tr><td><strong>UMA010</strong></td><td>Optimization Techniques &amp; Linear Programming</td><td>Applied Math</td><td>3.5</td><td><span class='badge-tag badge-paid'>Graded / Completed</span></td></tr>");
        sb.append("<tr><td><strong>UCS507</strong></td><td>Database Management Systems &amp; SQL Lab</td><td>Core Practical</td><td>4.0</td><td><span class='badge-tag badge-paid'>Graded / Completed</span></td></tr>");
        sb.append("<tr><td><strong>UTA012</strong></td><td>Innovation &amp; Entrepreneurship Practice</td><td>Professional Elective</td><td>2.0</td><td><span class='badge-tag badge-paid'>Graded / Completed</span></td></tr>");

        sb.append("</tbody></table></div>");
        sb.append("</div></div>");

        // 3. TIET Official Grading System Reference
        sb.append("<div class='card'>");
        sb.append("<div class='card-header'>");
        sb.append("<h3>&#9878; TIET Academic Grading Standard (10-Point Scale)</h3>");
        sb.append("<span class='badge-tag'>Academic Regulations</span>");
        sb.append("</div>");
        sb.append("<div class='card-body'>");
        sb.append("<div class='table-responsive'><table class='data-table'>");
        sb.append("<thead><tr><th>Letter Grade</th><th>Grade Point</th><th>Description</th><th>Performance Standard</th></tr></thead>");
        sb.append("<tbody>");
        sb.append("<tr><td><strong>A+ / A</strong></td><td>10.0</td><td>Outstanding / Excellent</td><td>Demonstrates exceptional mastery of the course</td></tr>");
        sb.append("<tr><td><strong>B / B-</strong></td><td>8.0 / 7.0</td><td>Very Good / Good</td><td>Substantial knowledge and high application competency</td></tr>");
        sb.append("<tr><td><strong>C / C-</strong></td><td>6.0 / 5.0</td><td>Average / Fair</td><td>Satisfactory understanding of fundamental concepts</td></tr>");
        sb.append("<tr><td><strong>D</strong></td><td>4.0</td><td>Marginal Pass</td><td>Meets minimum course prerequisites</td></tr>");
        sb.append("<tr><td><strong>E / F</strong></td><td>0.0</td><td>Fail</td><td>Inadequate performance; must repeat examination</td></tr>");
        sb.append("</tbody></table></div>");
        sb.append("<div style='margin-top: 14px; font-size: 12.5px; color: var(--text-secondary);'>");
        sb.append("&bull; Official cumulative grade transcripts are verified by the Office of the Controller of Examinations (COE), TIET Patiala. Cumulative on record: <strong>").append(String.format(Locale.US, "%.2f", s.getCgpa())).append("</strong>.");
        sb.append("</div>");
        sb.append("</div></div>");

        return renderStudentLayoutHtml(s, "Academic Records", "academics", sb.toString());
    }

    /**
     * Admin Dashboard View
     */
    private static String renderAdminDashboardHtml(String admin, int totalStudents, List<Student> recent, String flashSuccess) {
        StringBuilder sb = new StringBuilder();

        // Flash Alert if present
        if (flashSuccess != null) {
            sb.append("<div class='alert alert-success'><span>&#10004;</span> ").append(esc(flashSuccess)).append("</div>");
        }

        // 1. Metric Cards
        sb.append("<div class='stats-grid'>");
        sb.append("<div class='stat-box'>");
        sb.append("<div class='stat-info'>");
        sb.append("<h4>Registered Students</h4>");
        sb.append("<div class='stat-value' style='color: var(--primary-red);'>").append(totalStudents).append("</div>");
        sb.append("<div class='stat-sub'>Total records in institutional database</div>");
        sb.append("</div>");
        sb.append("<div class='stat-icon'>&#127891;</div>");
        sb.append("</div>");

        sb.append("<div class='stat-box'>");
        sb.append("<div class='stat-info'>");
        sb.append("<h4>Database Synchronization</h4>");
        sb.append("<div class='stat-value' style='font-size: 19px; color: #065F46;'><span class='badge-tag badge-paid'>Connected &amp; Ready</span></div>");
        sb.append("<div class='stat-sub'>H2 / MySQL Dual Mode Compatible</div>");
        sb.append("</div>");
        sb.append("<div class='stat-icon'>&#9989;</div>");
        sb.append("</div>");

        sb.append("<div class='stat-box'>");
        sb.append("<div class='stat-info'>");
        sb.append("<h4>Active Session</h4>");
        sb.append("<div class='stat-value' style='font-size: 19px;'>2024-2025</div>");
        sb.append("<div class='stat-sub'>Thapar Institute of Engg. &amp; Tech.</div>");
        sb.append("</div>");
        sb.append("<div class='stat-icon'>&#128197;</div>");
        sb.append("</div>");

        sb.append("<div class='stat-box'>");
        sb.append("<div class='stat-info'>");
        sb.append("<h4>Administrative Role</h4>");
        sb.append("<div class='stat-value' style='font-size: 19px; color: var(--primary-red);'>Academic Admin</div>");
        sb.append("<div class='stat-sub'>User: ").append(esc(admin)).append("</div>");
        sb.append("</div>");
        sb.append("<div class='stat-icon'>&#128272;</div>");
        sb.append("</div>");
        sb.append("</div>");

        // 2. Quick Operations
        sb.append("<div class='card'>");
        sb.append("<div class='card-header'>");
        sb.append("<h3>&#9881; Administrative Operations</h3>");
        sb.append("</div>");
        sb.append("<div class='card-body'>");
        sb.append("<p style='color: var(--text-secondary); margin-bottom: 18px;'>Perform admissions registration, edit enrolled credentials, evaluate fee statuses, and query student academic files:</p>");
        sb.append("<div style='display: flex; gap: 12px; flex-wrap: wrap;'>");
        sb.append("<a href='/students?action=new' class='btn btn-primary'>&#10133; Register New Student</a>");
        sb.append("<a href='/students?action=list' class='btn btn-secondary'>&#128101; View Complete Student Directory</a>");
        sb.append("</div>");
        sb.append("</div></div>");

        // 3. Recent Records Table
        sb.append("<div class='card'>");
        sb.append("<div class='card-header'>");
        sb.append("<h3>&#128101; Recently Enrolled Students</h3>");
        sb.append("<a href='/students?action=list' class='btn btn-secondary btn-sm'>View All &rarr;</a>");
        sb.append("</div>");
        sb.append("<div class='card-body' style='padding: 0;'>");
        sb.append("<div class='table-responsive'><table class='data-table'>");
        sb.append("<thead><tr><th>Roll Number</th><th>Student Name</th><th>Degree Programme</th><th>Department</th><th>Sem</th><th>CGPA</th><th>Fee Status</th><th style='text-align: center;'>Fees Pay</th></tr></thead>");
        sb.append("<tbody>");

        if (recent.isEmpty()) {
            sb.append("<tr><td colspan='8' style='text-align: center; padding: 32px; color: var(--text-secondary);'>No student records currently found in database.</td></tr>");
        } else {
            for (Student s : recent) {
                String feeClass = "Paid".equalsIgnoreCase(s.getFeeStatus()) ? "badge-paid" : ("Partial".equalsIgnoreCase(s.getFeeStatus()) ? "badge-partial" : "badge-pending");
                boolean isPaid = "Paid".equalsIgnoreCase(s.getFeeStatus());
                sb.append("<tr>");
                sb.append("<td><strong>").append(esc(s.getRollNumber())).append("</strong></td>");
                sb.append("<td><strong>").append(esc(s.getName())).append("</strong><div style='font-size: 11.5px; color: var(--text-secondary);'>").append(esc(s.getEmail())).append("</div></td>");
                sb.append("<td><span class='badge-tag' style='color: var(--primary-red);'>").append(esc(s.getCourse())).append("</span></td>");
                sb.append("<td>").append(esc(s.getDepartment())).append("</td>");
                sb.append("<td>").append(esc(s.getSemester())).append("</td>");
                sb.append("<td><strong>").append(String.format(Locale.US, "%.2f", s.getCgpa())).append("</strong></td>");
                sb.append("<td><span class='badge-tag ").append(feeClass).append("'>").append(esc(s.getFeeStatus())).append("</span></td>");
                sb.append("<td style='text-align: center;'>");
                if (isPaid) {
                    sb.append("<span class='badge-tag badge-paid' style='font-size: 11.5px;'>&#10004; Settled</span>");
                } else {
                    sb.append("<a href='/students?action=payFee&id=").append(s.getId()).append("&from=dashboard' class='btn btn-primary btn-sm' style='padding: 3px 8px; font-size: 11px;' onclick=\"return confirm('Clear and record fee payment as Paid for ").append(esc(s.getName())).append(" (").append(esc(s.getRollNumber())).append(")?');\">&#128179; Pay Fees</a>");
                }
                sb.append("</td>");
                sb.append("</tr>");
            }
        }
        sb.append("</tbody></table></div>");
        sb.append("</div></div>");

        return renderAdminLayoutHtml(admin, "Administrative Dashboard", "dashboard", sb.toString());
    }

    /**
     * Admin Student Directory View (CRUD Listing)
     */
    private static String renderViewStudentsHtml(String admin, List<Student> students, String keyword, String flashSuccess) {
        StringBuilder sb = new StringBuilder();

        if (flashSuccess != null) {
            sb.append("<div class='alert alert-success'><span>&#10004;</span> ").append(esc(flashSuccess)).append("</div>");
        }

        sb.append("<form action='/students' method='get' class='search-bar-form'>");
        sb.append("<input type='hidden' name='action' value='search'>");
        sb.append("<input type='text' name='keyword' class='form-control' placeholder='Search by roll number, name, or programme...' value='").append(esc(keyword)).append("'>");
        sb.append("<button type='submit' class='btn btn-primary'>&#128269; Search</button>");
        if (!keyword.isEmpty()) {
            sb.append("<a href='/students?action=list' class='btn btn-secondary'>Clear</a>");
        }
        sb.append("</form>");

        sb.append("<div class='card'>");
        sb.append("<div class='card-header'>");
        sb.append("<h3>&#128101; Student Academic Directory ");
        if (!keyword.isEmpty()) {
            sb.append("<span style='font-size: 12.5px; font-weight: normal; color: var(--text-secondary);'>(Filtered by: \"<strong>").append(esc(keyword)).append("</strong>\")</span>");
        }
        sb.append("</h3>");
        sb.append("<span class='badge-tag'>Total Registered: ").append(students.size()).append("</span>");
        sb.append("</div>");

        sb.append("<div class='card-body' style='padding: 0;'>");
        sb.append("<div class='table-responsive'><table class='data-table'>");
        sb.append("<thead><tr><th>Roll Number</th><th>Full Name</th><th>Programme</th><th>Department</th><th>Sem</th><th>CGPA</th><th>Fee Status</th><th style='text-align: center;'>Fees Pay</th><th style='text-align: center;'>Actions</th></tr></thead>");
        sb.append("<tbody>");

        if (students.isEmpty()) {
            sb.append("<tr><td colspan='9' style='text-align:center; padding: 48px; color: var(--text-secondary);'>");
            sb.append("<div class='empty-state'><div class='empty-state-icon'>&#128269;</div><h4>No matching student records found</h4><p>Try searching with another keyword or <a href='/students?action=new' style='color: var(--primary-red); font-weight: 600;'>enroll a new student record</a>.</p></div>");
            sb.append("</td></tr>");
        } else {
            for (Student s : students) {
                String feeClass = "Paid".equalsIgnoreCase(s.getFeeStatus()) ? "badge-paid" : ("Partial".equalsIgnoreCase(s.getFeeStatus()) ? "badge-partial" : "badge-pending");
                boolean isPaid = "Paid".equalsIgnoreCase(s.getFeeStatus());
                sb.append("<tr>");
                sb.append("<td><strong>").append(esc(s.getRollNumber())).append("</strong></td>");
                sb.append("<td><strong>").append(esc(s.getName())).append("</strong><div style='font-size: 12px; color: var(--text-secondary);'>").append(esc(s.getEmail())).append("</div></td>");
                sb.append("<td><span class='badge-tag' style='color: var(--primary-red);'>").append(esc(s.getCourse())).append("</span></td>");
                sb.append("<td>").append(esc(s.getDepartment())).append("</td>");
                sb.append("<td>").append(esc(s.getSemester())).append("</td>");
                sb.append("<td><strong>").append(String.format(Locale.US, "%.2f", s.getCgpa())).append("</strong></td>");
                sb.append("<td><span class='badge-tag ").append(feeClass).append("'>").append(esc(s.getFeeStatus())).append("</span></td>");
                sb.append("<td style='text-align: center;'>");
                if (isPaid) {
                    sb.append("<span class='badge-tag badge-paid' style='font-size: 11.5px;'>&#10004; Settled</span>");
                } else {
                    sb.append("<a href='/students?action=payFee&id=").append(s.getId()).append("&from=list' class='btn btn-primary btn-sm' style='padding: 3px 8px; font-size: 11px;' onclick=\"return confirm('Clear and record fee payment as Paid for ").append(esc(s.getName())).append(" (").append(esc(s.getRollNumber())).append(")?');\">&#128179; Pay Fees</a>");
                }
                sb.append("</td>");
                sb.append("<td style='text-align:center;'>");
                sb.append("<div style='display:inline-flex; gap:6px;'>");
                sb.append("<a href='/students?action=edit&id=").append(s.getId()).append("' class='btn btn-secondary btn-sm'>&#9998; Edit</a>");
                sb.append("<a href='/students?action=delete&id=").append(s.getId()).append("' class='btn btn-danger btn-sm' onclick=\"return confirmDelete('").append(esc(s.getName())).append("', '").append(esc(s.getRollNumber())).append("');\">&#128465; Delete</a>");
                sb.append("</div></td>");
                sb.append("</tr>");
            }
        }

        sb.append("</tbody></table></div></div></div>");
        return renderAdminLayoutHtml(admin, "Student Academic Directory", "students", sb.toString());
    }

    /**
     * Admin Add Student View
     */
    private static String renderAddStudentHtml(String admin, String error, Student s) {
        StringBuilder sb = new StringBuilder();
        if (error != null) sb.append("<div class='alert alert-error'><span>&#9888;</span> ").append(esc(error)).append("</div>");

        sb.append("<div class='card' style='max-width: 860px;'><div class='card-header'><h3>&#10133; Student Admission &amp; Enrollment Form</h3></div>");
        sb.append("<div class='card-body'><form action='/students' method='post' onsubmit='return validateStudentForm(this);'>");
        sb.append("<input type='hidden' name='action' value='insert'>");

        sb.append("<div class='form-grid'>");
        sb.append("<div class='form-group'><label class='form-label'>Roll Number / Registration No. <span class='required'>*</span></label><input type='text' name='rollNumber' class='form-control' placeholder='e.g. 102103045' value='").append(s != null ? esc(s.getRollNumber()) : "").append("' required autofocus></div>");
        sb.append("<div class='form-group'><label class='form-label'>Full Legal Name <span class='required'>*</span></label><input type='text' name='name' class='form-control' placeholder='e.g. Gurpreet Singh' value='").append(s != null ? esc(s.getName()) : "").append("' required></div>");
        sb.append("<div class='form-group'><label class='form-label'>Official Student Email <span class='required'>*</span></label><input type='email' name='email' class='form-control' placeholder='e.g. gsingh@thapar.edu' value='").append(s != null ? esc(s.getEmail()) : "").append("' required></div>");
        sb.append("<div class='form-group'><label class='form-label'>Contact Phone Number <span class='required'>*</span></label><input type='text' name='phone' class='form-control' placeholder='e.g. 9876543210' value='").append(s != null ? esc(s.getPhone()) : "").append("' required></div>");

        sb.append("<div class='form-group'><label class='form-label'>Degree Programme <span class='required'>*</span></label><select name='course' class='form-control' required>");
        sb.append("<option value=''>-- Select Enrolled Programme --</option>");
        String[] programmes = {
            "B.E. Computer Engineering",
            "B.E. Computer Science and Business Systems",
            "B.E. Electronics & Communication Engg.",
            "B.E. Electrical Engineering",
            "B.E. Mechanical Engineering",
            "B.E. Civil Engineering",
            "B.E. Biotechnology",
            "MCA / M.Tech Computer Science"
        };
        for (String p : programmes) {
            String sel = (s != null && p.equalsIgnoreCase(s.getCourse())) ? "selected" : "";
            sb.append("<option value='").append(p).append("' ").append(sel).append(">").append(p).append("</option>");
        }
        sb.append("</select></div>");

        sb.append("<div class='form-group'><label class='form-label'>Academic Department <span class='required'>*</span></label><select name='department' class='form-control' required>");
        sb.append("<option value=''>-- Select Department --</option>");
        String[] depts = {
            "Computer Science & Engineering",
            "Electronics & Communication Engg.",
            "Electrical & Instrumentation Engg.",
            "Mechanical Engineering",
            "Civil Engineering",
            "Biotechnology & Chemical Engg."
        };
        for (String d : depts) {
            String sel = (s != null && d.equalsIgnoreCase(s.getDepartment())) ? "selected" : "";
            sb.append("<option value='").append(d).append("' ").append(sel).append(">").append(d).append("</option>");
        }
        sb.append("</select></div>");

        sb.append("<div class='form-group'><label class='form-label'>Current Academic Semester <span class='required'>*</span></label><select name='semester' class='form-control' required>");
        for (int i = 1; i <= 8; i++) {
            String sem = "Semester " + i;
            String sel = (s != null && sem.equalsIgnoreCase(s.getSemester())) ? "selected" : "";
            sb.append("<option value='").append(sem).append("' ").append(sel).append(">").append(sem).append("</option>");
        }
        sb.append("</select></div>");

        sb.append("<div class='form-group'><label class='form-label'>Cumulative CGPA (0.00 - 10.00) <span class='required'>*</span></label><input type='number' step='0.01' min='0' max='10' name='cgpa' class='form-control' value='").append(s != null && s.getCgpa() > 0 ? String.format(Locale.US, "%.2f", s.getCgpa()) : "8.00").append("' required></div>");

        sb.append("<div class='form-group full-width'><label class='form-label'>Semester Fee Clearance Status <span class='required'>*</span></label><select name='feeStatus' class='form-control' required>");
        String[] feeOptions = {"Paid", "Partial", "Pending"};
        for (String f : feeOptions) {
            String sel = (s != null && f.equalsIgnoreCase(s.getFeeStatus())) ? "selected" : "";
            sb.append("<option value='").append(f).append("' ").append(sel).append(">").append(f).append("</option>");
        }
        sb.append("</select></div></div>");

        sb.append("<div class='form-actions'><button type='submit' class='btn btn-primary'>&#10004; Save Student Record</button><a href='/students?action=list' class='btn btn-secondary'>Cancel</a></div>");
        sb.append("</form></div></div>");

        return renderAdminLayoutHtml(admin, "Register New Student", "new", sb.toString());
    }

    /**
     * Admin Edit Student View
     */
    private static String renderEditStudentHtml(String admin, String error, Student s) {
        StringBuilder sb = new StringBuilder();
        if (error != null) sb.append("<div class='alert alert-error'><span>&#9888;</span> ").append(esc(error)).append("</div>");

        sb.append("<div class='card' style='max-width: 860px;'><div class='card-header'><h3>&#9998; Edit Student Record: ").append(esc(s.getName())).append("</h3></div>");
        sb.append("<div class='card-body'><form action='/students' method='post' onsubmit='return validateStudentForm(this);'>");
        sb.append("<input type='hidden' name='action' value='update'>");
        sb.append("<input type='hidden' name='id' value='").append(s.getId()).append("'>");

        sb.append("<div class='form-grid'>");
        sb.append("<div class='form-group'><label class='form-label'>Roll Number / Registration No. <span class='required'>*</span></label><input type='text' name='rollNumber' class='form-control' value='").append(esc(s.getRollNumber())).append("' required></div>");
        sb.append("<div class='form-group'><label class='form-label'>Full Legal Name <span class='required'>*</span></label><input type='text' name='name' class='form-control' value='").append(esc(s.getName())).append("' required></div>");
        sb.append("<div class='form-group'><label class='form-label'>Official Student Email <span class='required'>*</span></label><input type='email' name='email' class='form-control' value='").append(esc(s.getEmail())).append("' required></div>");
        sb.append("<div class='form-group'><label class='form-label'>Contact Phone Number <span class='required'>*</span></label><input type='text' name='phone' class='form-control' value='").append(esc(s.getPhone())).append("' required></div>");

        sb.append("<div class='form-group'><label class='form-label'>Degree Programme <span class='required'>*</span></label><select name='course' class='form-control' required>");
        String[] programmes = {
            "B.E. Computer Engineering",
            "B.E. Computer Science and Business Systems",
            "B.E. Electronics & Communication Engg.",
            "B.E. Electrical Engineering",
            "B.E. Mechanical Engineering",
            "B.E. Civil Engineering",
            "B.E. Biotechnology",
            "MCA / M.Tech Computer Science"
        };
        for (String p : programmes) {
            String sel = (p.equalsIgnoreCase(s.getCourse()) || s.getCourse().contains(p.replace("B.E. ", ""))) ? "selected" : "";
            sb.append("<option value='").append(p).append("' ").append(sel).append(">").append(p).append("</option>");
        }
        sb.append("</select></div>");

        sb.append("<div class='form-group'><label class='form-label'>Academic Department <span class='required'>*</span></label><select name='department' class='form-control' required>");
        String[] depts = {
            "Computer Science & Engineering",
            "Electronics & Communication Engg.",
            "Electrical & Instrumentation Engg.",
            "Mechanical Engineering",
            "Civil Engineering",
            "Biotechnology & Chemical Engg."
        };
        for (String d : depts) {
            String sel = (d.equalsIgnoreCase(s.getDepartment()) || s.getDepartment().contains(d.replace(" & Engineering", ""))) ? "selected" : "";
            sb.append("<option value='").append(d).append("' ").append(sel).append(">").append(d).append("</option>");
        }
        sb.append("</select></div>");

        sb.append("<div class='form-group'><label class='form-label'>Current Academic Semester <span class='required'>*</span></label><select name='semester' class='form-control' required>");
        for (int i = 1; i <= 8; i++) {
            String sem = "Semester " + i;
            String sel = sem.equalsIgnoreCase(s.getSemester()) ? "selected" : "";
            sb.append("<option value='").append(sem).append("' ").append(sel).append(">").append(sem).append("</option>");
        }
        sb.append("</select></div>");

        sb.append("<div class='form-group'><label class='form-label'>Cumulative CGPA (0.00 - 10.00) <span class='required'>*</span></label><input type='number' step='0.01' min='0' max='10' name='cgpa' class='form-control' value='").append(String.format(Locale.US, "%.2f", s.getCgpa())).append("' required></div>");

        sb.append("<div class='form-group full-width'><label class='form-label'>Semester Fee Clearance Status <span class='required'>*</span></label><select name='feeStatus' class='form-control' required>");
        String[] feeOptions = {"Paid", "Partial", "Pending"};
        for (String f : feeOptions) {
            String sel = f.equalsIgnoreCase(s.getFeeStatus()) ? "selected" : "";
            sb.append("<option value='").append(f).append("' ").append(sel).append(">").append(f).append("</option>");
        }
        sb.append("</select></div></div>");

        sb.append("<div class='form-actions'><button type='submit' class='btn btn-primary'>&#10004; Update Changes</button><a href='/students?action=list' class='btn btn-secondary'>Cancel</a></div>");
        sb.append("</form></div></div>");

        return renderAdminLayoutHtml(admin, "Edit Student", "students", sb.toString());
    }

    /**
     * Shared Student Portal Shell / Layout
     */
    private static String renderStudentLayoutHtml(Student s, String title, String activeNav, String bodyContent) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        sb.append("<title>").append(esc(title)).append(" - TIET Student Portal</title>");
        sb.append("<link rel='stylesheet' href='/css/style.css'>");
        sb.append("<script src='/js/main.js'></script></head><body>");

        sb.append("<div class='app-container'>");

        // Deep Red Sidebar
        sb.append("<aside class='sidebar'>");
        sb.append("<div class='sidebar-header'>");
        sb.append("<div class='brand-crest'>");
        sb.append("<div class='crest-icon'>TIET</div>");
        sb.append("<div class='brand-details'>");
        sb.append("<div class='brand-title'>TIET Student Portal</div>");
        sb.append("<div class='brand-subtitle'>Thapar Institute, Patiala</div>");
        sb.append("</div></div></div>");

        sb.append("<nav class='sidebar-nav'>");
        sb.append("<div class='nav-section-label'>Student Workspace</div>");
        sb.append("<ul>");
        sb.append("<li class='").append("dashboard".equals(activeNav) ? "active" : "").append("'><a href='/student-dashboard'><span class='icon'>&#128202;</span> Dashboard</a></li>");
        sb.append("<li class='").append("profile".equals(activeNav) ? "active" : "").append("'><a href='/student-profile'><span class='icon'>&#128100;</span> My Profile</a></li>");
        sb.append("<li class='").append("academics".equals(activeNav) ? "active" : "").append("'><a href='/academic-records'><span class='icon'>&#128220;</span> Academic Records</a></li>");
        sb.append("</ul></nav>");

        sb.append("<div class='sidebar-footer'>");
        sb.append("<div class='user-badge'>");
        sb.append("<div class='user-avatar'>&#127891;</div>");
        sb.append("<div class='user-details'>");
        sb.append("<div class='user-name'>").append(esc(s.getName())).append("</div>");
        sb.append("<div class='user-role'>").append(esc(s.getRollNumber())).append("</div>");
        sb.append("</div></div>");
        sb.append("<a href='/logout' class='logout-link'>&#10140; Sign Out</a>");
        sb.append("</div></aside>");

        // Main Wrapper
        sb.append("<div class='main-wrapper'>");
        sb.append("<header class='topbar'>");
        sb.append("<div class='topbar-left'>");
        sb.append("<div class='topbar-title'>").append(esc(title)).append("</div>");
        sb.append("<div class='topbar-tagline'>Your Academic Journey, Simplified.</div>");
        sb.append("</div>");
        sb.append("<div class='topbar-meta'>");
        sb.append("<span class='role-pill student'>TIET Student</span>");
        sb.append("<span style='font-size: 12.5px; color: var(--text-secondary);'>Academic Session 2024-2025</span>");
        sb.append("</div></header>");

        sb.append("<main class='content-area'>").append(bodyContent).append("</main>");

        sb.append("<footer style='padding: 14px 32px; background: #FFFFFF; border-top: 1px solid var(--border-color); font-size: 11.5px; color: var(--text-secondary); text-align: center;'>");
        sb.append("TIET Student Portal &bull; Academic Demonstration Prototype inspired by Thapar Institute of Engineering &amp; Technology, Patiala &bull; Est. 1956");
        sb.append("</footer>");

        sb.append("</div></div></body></html>");
        return sb.toString();
    }

    /**
     * Shared Admin Portal Shell / Layout
     */
    private static String renderAdminLayoutHtml(String admin, String title, String activeNav, String bodyContent) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        sb.append("<title>").append(esc(title)).append(" - TIET Administration</title>");
        sb.append("<link rel='stylesheet' href='/css/style.css'>");
        sb.append("<script src='/js/main.js'></script></head><body>");

        sb.append("<div class='app-container'>");

        // Deep Red Sidebar
        sb.append("<aside class='sidebar'>");
        sb.append("<div class='sidebar-header'>");
        sb.append("<div class='brand-crest'>");
        sb.append("<div class='crest-icon'>TIET</div>");
        sb.append("<div class='brand-details'>");
        sb.append("<div class='brand-title'>TIET Administration</div>");
        sb.append("<div class='brand-subtitle'>Thapar Institute, Patiala</div>");
        sb.append("</div></div></div>");

        sb.append("<nav class='sidebar-nav'>");
        sb.append("<div class='nav-section-label'>Management Suite</div>");
        sb.append("<ul>");
        sb.append("<li class='").append("dashboard".equals(activeNav) ? "active" : "").append("'><a href='/dashboard'><span class='icon'>&#128202;</span> Dashboard</a></li>");
        sb.append("<li class='").append("students".equals(activeNav) ? "active" : "").append("'><a href='/students?action=list'><span class='icon'>&#128101;</span> Student Directory</a></li>");
        sb.append("<li class='").append("new".equals(activeNav) ? "active" : "").append("'><a href='/students?action=new'><span class='icon'>&#10133;</span> Add Student</a></li>");
        sb.append("</ul></nav>");

        sb.append("<div class='sidebar-footer'>");
        sb.append("<div class='user-badge'>");
        sb.append("<div class='user-avatar'>&#128273;</div>");
        sb.append("<div class='user-details'>");
        sb.append("<div class='user-name'>").append(esc(admin)).append("</div>");
        sb.append("<div class='user-role'>Administrator</div>");
        sb.append("</div></div>");
        sb.append("<a href='/logout' class='logout-link'>&#10140; Sign Out</a>");
        sb.append("</div></aside>");

        // Main Wrapper
        sb.append("<div class='main-wrapper'>");
        sb.append("<header class='topbar'>");
        sb.append("<div class='topbar-left'>");
        sb.append("<div class='topbar-title'>").append(esc(title)).append("</div>");
        sb.append("<div class='topbar-tagline'>Institutional Administrative Console</div>");
        sb.append("</div>");
        sb.append("<div class='topbar-meta'>");
        sb.append("<span class='role-pill admin'>Administrator</span>");
        sb.append("<span style='font-size: 12.5px; color: var(--text-secondary);'>Academic Session 2024-2025</span>");
        sb.append("</div></header>");

        sb.append("<main class='content-area'>").append(bodyContent).append("</main>");

        sb.append("<footer style='padding: 14px 32px; background: #FFFFFF; border-top: 1px solid var(--border-color); font-size: 11.5px; color: var(--text-secondary); text-align: center;'>");
        sb.append("TIET Administration Suite &bull; Academic Demonstration Prototype inspired by Thapar Institute of Engineering &amp; Technology, Patiala");
        sb.append("</footer>");

        sb.append("</div></div></body></html>");
        return sb.toString();
    }

    // =========================================================================
    // HTTP UTILITY HELPERS
    // =========================================================================

    private static void serveFile(OutputStream out, String path, String newSessionId) throws IOException {
        File file = new File("src/main/webapp" + path);
        if (!file.exists()) {
            file = new File("webapp" + path);
        }
        byte[] bytes = null;
        if (file.exists() && !file.isDirectory()) {
            bytes = Files.readAllBytes(file.toPath());
        } else {
            try (InputStream is = StandaloneAppRunner.class.getResourceAsStream("/webapp" + path)) {
                if (is != null) bytes = is.readAllBytes();
            }
            if (bytes == null) {
                try (InputStream is = StandaloneAppRunner.class.getResourceAsStream(path)) {
                    if (is != null) bytes = is.readAllBytes();
                }
            }
        }
        if (bytes == null) {
            sendHtml(out, 404, "Static file not found", null);
            return;
        }
        String mime = path.endsWith(".css") ? "text/css" : (path.endsWith(".js") ? "application/javascript" : "text/plain");
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
        pw.print("HTTP/1.1 200 OK\r\n");
        pw.print("Content-Type: " + mime + "; charset=UTF-8\r\n");
        pw.print("Content-Length: " + bytes.length + "\r\n");
        if (newSessionId != null) pw.print("Set-Cookie: JSESSIONID=" + newSessionId + "; Path=/; HttpOnly\r\n");
        pw.print("Connection: close\r\n\r\n");
        pw.flush();
        out.write(bytes);
        out.flush();
    }

    private static void sendRedirect(OutputStream out, String location, String newSessionId) throws IOException {
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
        pw.print("HTTP/1.1 302 Found\r\n");
        pw.print("Location: " + location + "\r\n");
        if (newSessionId != null) pw.print("Set-Cookie: JSESSIONID=" + newSessionId + "; Path=/; HttpOnly\r\n");
        pw.print("Connection: close\r\n\r\n");
        pw.flush();
    }

    private static void sendHtml(OutputStream out, int status, String html, String newSessionId) throws IOException {
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
        pw.print("HTTP/1.1 " + status + " OK\r\n");
        pw.print("Content-Type: text/html; charset=UTF-8\r\n");
        pw.print("Content-Length: " + bytes.length + "\r\n");
        if (newSessionId != null) pw.print("Set-Cookie: JSESSIONID=" + newSessionId + "; Path=/; HttpOnly\r\n");
        pw.print("Connection: close\r\n\r\n");
        pw.flush();
        out.write(bytes);
        out.flush();
    }

    private static Map<String, String> parseParams(String data) {
        Map<String, String> map = new HashMap<>();
        if (data == null || data.isEmpty()) return map;
        for (String pair : data.split("&")) {
            String[] kv = pair.split("=");
            try {
                String k = URLDecoder.decode(kv[0], StandardCharsets.UTF_8.name());
                String v = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8.name()) : "";
                map.put(k, v);
            } catch (Exception ignored) {}
        }
        return map;
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}
