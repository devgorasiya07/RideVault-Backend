package com.ridevault.dao;

import com.ridevault.DatabaseConnection;
import com.ridevault.model.Driver;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DriverDAO {

    public boolean registerDriver(Driver newDriver) {
        String sql = "INSERT INTO Driver (firstname, middlename, lastname, email, phone_no, password_hash, license_no) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newDriver.getFirstName());
            pstmt.setString(2, newDriver.getMiddleName());
            pstmt.setString(3, newDriver.getLastName());
            pstmt.setString(4, newDriver.getEmail());
            pstmt.setString(5, newDriver.getPhoneNo());
            pstmt.setString(6, newDriver.getPasswordHash());
            pstmt.setString(7, newDriver.getLicenseNo());

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("[!] Database error during Driver Registration: " + e.getMessage());
            return false;
        }
    }

    public Driver loginDriver(String email, String password) {
        String sql = "SELECT * FROM Driver WHERE email = ? AND password_hash = ?";
        Driver loggedInDriver = null;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);
            pstmt.setString(2, password);

            ResultSet resultSet = pstmt.executeQuery();

            if (resultSet.next()) {
                loggedInDriver = new Driver(
                    resultSet.getInt("driver_id"),
                    resultSet.getString("firstname"),
                    resultSet.getString("middlename"),
                    resultSet.getString("lastname"),
                    resultSet.getString("email"),
                    resultSet.getString("phone_no"),
                    resultSet.getString("password_hash"),
                    resultSet.getString("license_no"),
                    
                    // FIXED: Now mapping exactly to your DDL columns!
                    resultSet.getDouble("avg_rating"),
                    resultSet.getInt("total_rides"),
                    resultSet.getBoolean("is_verified"),
                    
                    resultSet.getTimestamp("created_at")
                );
            }

        } catch (SQLException e) {
            System.out.println("[!] Database error during Driver Login: " + e.getMessage());
        }

        return loggedInDriver;
    }
}