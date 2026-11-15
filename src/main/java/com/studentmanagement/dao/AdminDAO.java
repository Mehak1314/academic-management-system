package com.studentmanagement.dao;

import com.studentmanagement.model.Admin;
import com.studentmanagement.util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Data Access Object (DAO) for Admin entity.
 * Handles database operations related to authentication using PreparedStatement.
 */
public class AdminDAO {

    private static final String SELECT_ADMIN_BY_CREDENTIALS = 
            "SELECT id, username, password FROM admin WHERE username = ? AND password = ?";

    /**
     * Validates admin login credentials against the MySQL database.
     * Demonstrates safe PreparedStatement execution avoiding SQL injection.
     *
     * @param username the entered username
     * @param password the entered password
     * @return Admin object if found, null otherwise
     * @throws SQLException on database error
     */
    public Admin validateAdmin(String username, String password) throws SQLException {
        Admin admin = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            connection = DBUtil.getConnection();
            preparedStatement = connection.prepareStatement(SELECT_ADMIN_BY_CREDENTIALS);
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, password);

            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                admin = new Admin();
                admin.setId(resultSet.getInt("id"));
                admin.setUsername(resultSet.getString("username"));
                admin.setPassword(resultSet.getString("password"));
            }
        } finally {
            DBUtil.closeResource(resultSet);
            DBUtil.closeResource(preparedStatement);
            DBUtil.closeResource(connection);
        }

        return admin;
    }
}

