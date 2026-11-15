package com.studentmanagement.dao;

import com.studentmanagement.model.Student;
import com.studentmanagement.util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) for Student entity.
 * Implements complete CRUD (Create, Read, Update, Delete) operations using JDBC and PreparedStatement.
 * Includes course, cgpa, and fee_status fields.
 */
public class StudentDAO {

    private static final String INSERT_STUDENT_SQL = 
            "INSERT INTO students (roll_number, name, email, phone, department, semester, course, cgpa, fee_status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    
    private static final String SELECT_STUDENT_BY_ID = 
            "SELECT id, roll_number, name, email, phone, department, semester, course, cgpa, fee_status FROM students WHERE id = ?";
    
    private static final String SELECT_STUDENT_BY_ROLL = 
            "SELECT id, roll_number, name, email, phone, department, semester, course, cgpa, fee_status FROM students WHERE roll_number = ?";
    
    private static final String SELECT_ALL_STUDENTS = 
            "SELECT id, roll_number, name, email, phone, department, semester, course, cgpa, fee_status FROM students ORDER BY id DESC";
    
    private static final String UPDATE_STUDENT_SQL = 
            "UPDATE students SET roll_number = ?, name = ?, email = ?, phone = ?, department = ?, semester = ?, course = ?, cgpa = ?, fee_status = ? WHERE id = ?";
    
    private static final String DELETE_STUDENT_SQL = 
            "DELETE FROM students WHERE id = ?";
    
    private static final String COUNT_ALL_STUDENTS = 
            "SELECT COUNT(*) AS total FROM students";
    
    private static final String SEARCH_STUDENTS_SQL = 
            "SELECT id, roll_number, name, email, phone, department, semester, course, cgpa, fee_status FROM students " +
            "WHERE roll_number LIKE ? OR name LIKE ? OR course LIKE ? ORDER BY id DESC";

    public boolean insertStudent(Student student) throws SQLException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            connection = DBUtil.getConnection();
            preparedStatement = connection.prepareStatement(INSERT_STUDENT_SQL);
            preparedStatement.setString(1, student.getRollNumber().trim());
            preparedStatement.setString(2, student.getName().trim());
            preparedStatement.setString(3, student.getEmail().trim());
            preparedStatement.setString(4, student.getPhone().trim());
            preparedStatement.setString(5, student.getDepartment().trim());
            preparedStatement.setString(6, student.getSemester().trim());
            preparedStatement.setString(7, student.getCourse() != null ? student.getCourse().trim() : "B.Tech");
            preparedStatement.setDouble(8, student.getCgpa());
            preparedStatement.setString(9, student.getFeeStatus() != null ? student.getFeeStatus().trim() : "Paid");

            return preparedStatement.executeUpdate() > 0;
        } finally {
            DBUtil.closeResource(preparedStatement);
            DBUtil.closeResource(connection);
        }
    }

    public Student selectStudent(int id) throws SQLException {
        Student student = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            connection = DBUtil.getConnection();
            preparedStatement = connection.prepareStatement(SELECT_STUDENT_BY_ID);
            preparedStatement.setInt(1, id);
            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                student = extractStudentFromResultSet(resultSet);
            }
        } finally {
            DBUtil.closeResource(resultSet);
            DBUtil.closeResource(preparedStatement);
            DBUtil.closeResource(connection);
        }
        return student;
    }

    public Student selectStudentByRollNumber(String rollNumber) throws SQLException {
        Student student = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            connection = DBUtil.getConnection();
            preparedStatement = connection.prepareStatement(SELECT_STUDENT_BY_ROLL);
            preparedStatement.setString(1, rollNumber != null ? rollNumber.trim() : "");
            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                student = extractStudentFromResultSet(resultSet);
            }
        } finally {
            DBUtil.closeResource(resultSet);
            DBUtil.closeResource(preparedStatement);
            DBUtil.closeResource(connection);
        }
        return student;
    }

    public List<Student> selectAllStudents() throws SQLException {
        List<Student> students = new ArrayList<>();
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            connection = DBUtil.getConnection();
            preparedStatement = connection.prepareStatement(SELECT_ALL_STUDENTS);
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                students.add(extractStudentFromResultSet(resultSet));
            }
        } finally {
            DBUtil.closeResource(resultSet);
            DBUtil.closeResource(preparedStatement);
            DBUtil.closeResource(connection);
        }
        return students;
    }

    public boolean updateStudent(Student student) throws SQLException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            connection = DBUtil.getConnection();
            preparedStatement = connection.prepareStatement(UPDATE_STUDENT_SQL);
            preparedStatement.setString(1, student.getRollNumber().trim());
            preparedStatement.setString(2, student.getName().trim());
            preparedStatement.setString(3, student.getEmail().trim());
            preparedStatement.setString(4, student.getPhone().trim());
            preparedStatement.setString(5, student.getDepartment().trim());
            preparedStatement.setString(6, student.getSemester().trim());
            preparedStatement.setString(7, student.getCourse() != null ? student.getCourse().trim() : "B.Tech");
            preparedStatement.setDouble(8, student.getCgpa());
            preparedStatement.setString(9, student.getFeeStatus() != null ? student.getFeeStatus().trim() : "Paid");
            preparedStatement.setInt(10, student.getId());

            return preparedStatement.executeUpdate() > 0;
        } finally {
            DBUtil.closeResource(preparedStatement);
            DBUtil.closeResource(connection);
        }
    }

    public boolean deleteStudent(int id) throws SQLException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            connection = DBUtil.getConnection();
            preparedStatement = connection.prepareStatement(DELETE_STUDENT_SQL);
            preparedStatement.setInt(1, id);

            return preparedStatement.executeUpdate() > 0;
        } finally {
            DBUtil.closeResource(preparedStatement);
            DBUtil.closeResource(connection);
        }
    }

    public int getStudentCount() throws SQLException {
        int count = 0;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            connection = DBUtil.getConnection();
            preparedStatement = connection.prepareStatement(COUNT_ALL_STUDENTS);
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                count = resultSet.getInt("total");
            }
        } finally {
            DBUtil.closeResource(resultSet);
            DBUtil.closeResource(preparedStatement);
            DBUtil.closeResource(connection);
        }
        return count;
    }

    public List<Student> searchStudents(String query) throws SQLException {
        List<Student> students = new ArrayList<>();
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            connection = DBUtil.getConnection();
            preparedStatement = connection.prepareStatement(SEARCH_STUDENTS_SQL);
            String wildcardQuery = "%" + (query != null ? query.trim() : "") + "%";
            preparedStatement.setString(1, wildcardQuery);
            preparedStatement.setString(2, wildcardQuery);
            preparedStatement.setString(3, wildcardQuery);

            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                students.add(extractStudentFromResultSet(resultSet));
            }
        } finally {
            DBUtil.closeResource(resultSet);
            DBUtil.closeResource(preparedStatement);
            DBUtil.closeResource(connection);
        }
        return students;
    }

    private Student extractStudentFromResultSet(ResultSet rs) throws SQLException {
        Student student = new Student();
        student.setId(rs.getInt("id"));
        student.setRollNumber(rs.getString("roll_number"));
        student.setName(rs.getString("name"));
        student.setEmail(rs.getString("email"));
        student.setPhone(rs.getString("phone"));
        student.setDepartment(rs.getString("department"));
        student.setSemester(rs.getString("semester"));
        
        // Handle optional/newer columns gracefully
        try {
            student.setCourse(rs.getString("course"));
        } catch (Exception e) {
            student.setCourse("B.Tech");
        }
        try {
            student.setCgpa(rs.getDouble("cgpa"));
        } catch (Exception e) {
            student.setCgpa(8.0);
        }
        try {
            student.setFeeStatus(rs.getString("fee_status"));
        } catch (Exception e) {
            student.setFeeStatus("Paid");
        }
        
        return student;
    }
}
