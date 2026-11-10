package com.ridevault.dao;

import com.ridevault.DatabaseConnection;
import com.ridevault.model.Rider;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class RiderDAO {

    // --- 1. INSERT A NEW RIDER ---
    public boolean registerRider(Rider newRider) {
        String sql = "INSERT INTO Rider (firstname, middlename, lastname, email, phone_no, password_hash) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newRider.getFirstName());
            pstmt.setString(2, newRider.getMiddleName());
            pstmt.setString(3, newRider.getLastName());
            pstmt.setString(4, newRider.getEmail());
            pstmt.setString(5, newRider.getPhoneNo());
            pstmt.setString(6, newRider.getPasswordHash());

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("❌ Database error during Rider Registration: " + e.getMessage());
            return false;
        }
    }

    // --- 2. FIND A RIDER FOR LOGIN ---
    public Rider loginRider(String email, String password) {
        String sql = "SELECT * FROM Rider WHERE email = ? AND password_hash = ?";
        Rider loggedInRider = null;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);
            pstmt.setString(2, password);

            ResultSet resultSet = pstmt.executeQuery();

            if (resultSet.next()) {
                loggedInRider = new Rider(
                    resultSet.getInt("rider_id"),
                    resultSet.getString("firstname"),
                    resultSet.getString("middlename"),
                    resultSet.getString("lastname"),
                    resultSet.getString("email"),
                    resultSet.getString("phone_no"),
                    resultSet.getString("password_hash"),
                    resultSet.getBoolean("is_verified"),
                    resultSet.getTimestamp("created_at")
                );
            }

        } catch (SQLException e) {
            System.out.println("❌ Database error during Rider Login: " + e.getMessage());
        }

        return loggedInRider;
    }
}