package com.studentmanagement.dao;

import com.studentmanagement.model.Student;
import com.studentmanagement.util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class StudentAuthDAO {

    private static final String AUTH_STUDENT_SQL = 
            "SELECT id, roll_number, name, email, phone, department, semester, course, cgpa, fee_status " +
            "FROM students WHERE LOWER(roll_number) = LOWER(?) AND LOWER(email) = LOWER(?)";

    public Student authenticateStudent(String rollNumber, String email) throws SQLException {
        Student student = null;
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            ps = conn.prepareStatement(AUTH_STUDENT_SQL);
            ps.setString(1, rollNumber != null ? rollNumber.trim() : "");
            ps.setString(2, email != null ? email.trim() : "");

            rs = ps.executeQuery();
            if (rs.next()) {
                student = new Student();
                student.setId(rs.getInt("id"));
                student.setRollNumber(rs.getString("roll_number"));
                student.setName(rs.getString("name"));
                student.setEmail(rs.getString("email"));
                student.setPhone(rs.getString("phone"));
                student.setDepartment(rs.getString("department"));
                student.setSemester(rs.getString("semester"));
                try {
                    student.setCourse(rs.getString("course"));
                    student.setCgpa(rs.getDouble("cgpa"));
                    student.setFeeStatus(rs.getString("fee_status"));
                } catch (Exception ignored) {}
            }
        } finally {
            DBUtil.closeResource(rs);
            DBUtil.closeResource(ps);
            DBUtil.closeResource(conn);
        }

        return student;
    }
}
