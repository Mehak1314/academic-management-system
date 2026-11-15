package com.studentmanagement.util;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Utility class to manage database connections.
 * Automatically initializes database schema and adds courses, cgpa, and fee_status columns.
 */
public class DBUtil {

    private static String jdbcURL = "jdbc:h2:./data/student_db;MODE=MySQL;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1";
    private static String jdbcUsername = "sa";
    private static String jdbcPassword = "";
    private static boolean initialized = false;

    static {
        try {
            InputStream input = DBUtil.class.getClassLoader().getResourceAsStream("db.properties");
            if (input != null) {
                Properties prop = new Properties();
                prop.load(input);
                jdbcURL = prop.getProperty("db.url", jdbcURL);
                jdbcUsername = prop.getProperty("db.username", jdbcUsername);
                jdbcPassword = prop.getProperty("db.password", jdbcPassword);
            }

            if (jdbcURL.contains(":h2:")) {
                Class.forName("org.h2.Driver");
            } else {
                Class.forName("com.mysql.cj.jdbc.Driver");
            }
        } catch (Exception e) {
            System.err.println("Database driver loading error: " + e.getMessage());
        }
    }

    public static synchronized Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(jdbcURL, jdbcUsername, jdbcPassword);
        if (!initialized) {
            initDatabase(conn);
            initialized = true;
        }
        return conn;
    }

    private static void initDatabase(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            // 1. Admin table
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS admin (" +
                "  id INT AUTO_INCREMENT PRIMARY KEY," +
                "  username VARCHAR(50) NOT NULL UNIQUE," +
                "  password VARCHAR(100) NOT NULL" +
                ")"
            );

            stmt.executeUpdate(
                "MERGE INTO admin (id, username, password) KEY(username) VALUES (1, 'admin', 'admin123')"
            );

            // 2. Students table with course, cgpa, fee_status
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS students (" +
                "  id INT AUTO_INCREMENT PRIMARY KEY," +
                "  roll_number VARCHAR(30) NOT NULL UNIQUE," +
                "  name VARCHAR(100) NOT NULL," +
                "  email VARCHAR(100) NOT NULL," +
                "  phone VARCHAR(20) NOT NULL," +
                "  department VARCHAR(50) NOT NULL," +
                "  semester VARCHAR(20) NOT NULL," +
                "  course VARCHAR(50) NOT NULL DEFAULT 'B.Tech'," +
                "  cgpa DECIMAL(3, 2) NOT NULL DEFAULT 8.00," +
                "  fee_status VARCHAR(20) NOT NULL DEFAULT 'Paid'" +
                ")"
            );

            // In case table existed before without the new columns, add them safely
            try { stmt.executeUpdate("ALTER TABLE students ADD COLUMN IF NOT EXISTS course VARCHAR(50) NOT NULL DEFAULT 'B.Tech'"); } catch (Exception ignored) {}
            try { stmt.executeUpdate("ALTER TABLE students ADD COLUMN IF NOT EXISTS cgpa DECIMAL(3, 2) NOT NULL DEFAULT 8.00"); } catch (Exception ignored) {}
            try { stmt.executeUpdate("ALTER TABLE students ADD COLUMN IF NOT EXISTS fee_status VARCHAR(20) NOT NULL DEFAULT 'Paid'"); } catch (Exception ignored) {}

            // Insert initial sample records if table is completely empty
            var rs = stmt.executeQuery("SELECT COUNT(*) AS total FROM students");
            if (rs.next() && rs.getInt("total") == 0) {
                stmt.executeUpdate(
                    "INSERT INTO students (roll_number, name, email, phone, department, semester, course, cgpa, fee_status) VALUES " +
                    "('CS202401', 'Aarav Sharma', 'aarav.sharma@college.edu', '9876543210', 'Computer Science', 'Semester 5', 'B.Tech - Computer Science', 8.75, 'Paid')," +
                    "('CS202402', 'Priya Patel', 'priya.patel@college.edu', '9876543211', 'Computer Science', 'Semester 5', 'B.Tech - Computer Science', 9.20, 'Paid')," +
                    "('IT202401', 'Rohan Mehta', 'rohan.mehta@college.edu', '9876543212', 'Information Technology', 'Semester 4', 'B.Tech - Information Tech', 7.85, 'Pending')," +
                    "('EC202401', 'Ananya Verma', 'ananya.verma@college.edu', '9876543213', 'Electronics & Comm.', 'Semester 6', 'B.Tech - Electronics', 8.90, 'Paid')," +
                    "('ME202401', 'Vikram Singh', 'vikram.singh@college.edu', '9876543214', 'Mechanical Engg.', 'Semester 3', 'B.Tech - Mechanical', 8.10, 'Partial')"
                );
            }
        } catch (SQLException e) {
            System.err.println("Auto-init info: " + e.getMessage());
        }
    }

    public static void closeResource(AutoCloseable autoCloseable) {
        if (autoCloseable != null) {
            try {
                autoCloseable.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
