package com.ridevault.dao;

import com.ridevault.DatabaseConnection;
import com.ridevault.model.FareRule;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PricingDAO {

    public FareRule getFareRuleByCategory(String category) {
        // FIXED: The table is explicitly named 'fare_rules' to match your database
        String sql = "SELECT fare_id, vehicle_category, base_fare, per_km_rate, per_min_rate, min_fare FROM fare_rules WHERE vehicle_category = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
             
            pstmt.setString(1, category);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return new FareRule(
                    rs.getInt("fare_id"),
                    rs.getString("vehicle_category"),
                    rs.getDouble("base_fare"),
                    rs.getDouble("per_km_rate"),
                    rs.getDouble("per_min_rate"),
                    rs.getDouble("min_fare")
                );
            }
        } catch (SQLException e) {
            System.out.println("\u001B[31m[!] Error fetching Fare Rules from DB: " + e.getMessage() + "\u001B[0m");
        }
        return null;
    }
}