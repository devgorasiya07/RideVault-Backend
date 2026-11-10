package com.ridevault.dao;

import com.ridevault.DatabaseConnection;
import com.ridevault.model.Vehicle;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class VehicleDAO {

    public boolean registerVehicle(Vehicle vehicle) {
        // UPDATED: Added insurance columns to the INSERT statement
        String sql = "INSERT INTO vehicle (driver_id, model, year, plate_no, category, is_active, is_verified, insurance_policy_no, insurance_expiry_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
                     
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
             
            pstmt.setInt(1, vehicle.getDriverId());
            pstmt.setString(2, vehicle.getModel());
            pstmt.setInt(3, vehicle.getYear());
            pstmt.setString(4, vehicle.getPlateNo());
            pstmt.setString(5, vehicle.getCategory());
            pstmt.setBoolean(6, vehicle.isActive());
            pstmt.setBoolean(7, vehicle.isVerified());
            pstmt.setString(8, vehicle.getInsurancePolicyNo());
            pstmt.setDate(9, vehicle.getInsuranceExpiryDate());
            
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.out.println("\u001B[31m[!] Database error registering vehicle: " + e.getMessage() + "\u001B[0m");
            return false;
        }
    }

    public Vehicle getVehicleByDriverId(int driverId) {
        String sql = "SELECT * FROM vehicle WHERE driver_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
             
            pstmt.setInt(1, driverId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return new Vehicle(
                    rs.getInt("vehicle_id"), rs.getInt("driver_id"), rs.getString("model"),
                    rs.getInt("year"), rs.getString("plate_no"), rs.getString("category"),
                    rs.getBoolean("is_active"), rs.getBoolean("is_verified"),
                    rs.getString("insurance_policy_no"), rs.getDate("insurance_expiry_date")
                );
            }
        } catch (SQLException e) {
            System.out.println("\u001B[31m[!] Error fetching Vehicle: " + e.getMessage() + "\u001B[0m");
        }
        return null;
    }
}