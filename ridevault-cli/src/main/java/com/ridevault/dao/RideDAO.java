package com.ridevault.dao;

import com.ridevault.DatabaseConnection;
import com.ridevault.model.Ride;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RideDAO {

    public boolean createRide(Ride newRide) {
        // We now insert the fare_id into the ride table!
        String sql = "INSERT INTO Ride (rider_id, status, fare_id, pickup_address, pickup_lat, pickup_lng, " +
                     "dropoff_address, dropoff_lat, dropoff_lng, final_fare) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, newRide.getRiderId());
            pstmt.setString(2, newRide.getStatus());
            pstmt.setInt(3, newRide.getFareId()); 
            pstmt.setString(4, newRide.getPickupAddress());
            pstmt.setDouble(5, newRide.getPickupLat()); 
            pstmt.setDouble(6, newRide.getPickupLng()); 
            pstmt.setString(7, newRide.getDropoffAddress());
            pstmt.setDouble(8, newRide.getDropoffLat()); 
            pstmt.setDouble(9, newRide.getDropoffLng()); 
            pstmt.setDouble(10, newRide.getFinalFare()); 
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("\u001B[31m[!] Database error requesting ride: " + e.getMessage() + "\u001B[0m");
            return false;
        }
    }

    // STRICT: Uses a JOIN to match the Ride's Fare_Id to the Driver's Vehicle Category!
    public List<Ride> getPendingRides(String driverVehicleCategory) {
        String sql = "SELECT r.* FROM Ride r " +
                     "JOIN FARE_RULES f ON r.fare_id = f.fare_id " +
                     "WHERE r.status = 'PENDING' AND f.vehicle_category = ?";
                     
        List<Ride> pendingRides = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
             
            pstmt.setString(1, driverVehicleCategory);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Integer driverId = (Integer) rs.getObject("driver_id");
                Integer vehicleId = (Integer) rs.getObject("vehicle_id");
                
                Ride ride = new Ride(
                    rs.getInt("ride_id"), rs.getInt("rider_id"), driverId, vehicleId, rs.getInt("fare_id"),
                    rs.getString("pickup_address"), rs.getDouble("pickup_lat"), rs.getDouble("pickup_lng"), 
                    rs.getString("dropoff_address"), rs.getDouble("dropoff_lat"), rs.getDouble("dropoff_lng"), 
                    rs.getString("status"), rs.getDouble("final_fare"), rs.getTimestamp("requested_at")
                );
                pendingRides.add(ride); 
            }
        } catch (SQLException e) {
            System.out.println("\u001B[31m[!] Database error fetching rides: " + e.getMessage() + "\u001B[0m");
        }
        return pendingRides; 
    }

    // STRICT: Updates Driver_Id AND Vehicle_Id, and ensures the category matches!
    public boolean acceptRideInDB(int rideId, int driverId, int vehicleId, String driverVehicleCategory) {
        String sql = "UPDATE Ride SET driver_id = ?, vehicle_id = ?, status = 'ACCEPTED' " +
                     "WHERE ride_id = ? AND status = 'PENDING' AND fare_id IN " +
                     "(SELECT fare_id FROM FARE_RULES WHERE vehicle_category = ?)";
                     
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, driverId); 
            pstmt.setInt(2, vehicleId);
            pstmt.setInt(3, rideId);
            pstmt.setString(4, driverVehicleCategory);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("\u001B[31m[!] Database error accepting ride: " + e.getMessage() + "\u001B[0m");
            return false;
        }
    }

    public double getExpectedFareForRide(int rideId) {
        String sql = "SELECT final_fare FROM Ride WHERE ride_id = ? AND status = 'ACCEPTED'"; 
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, rideId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getDouble("final_fare");
        } catch (SQLException e) { }
        return -1.0; 
    }

    public boolean completeRideInDB(int rideId, int driverId, double collectedFare, double driverEarning) {
        String sql = "UPDATE Ride SET status = 'COMPLETED', final_fare = ?, driver_earning = ?, completed_at = CURRENT_TIMESTAMP " +
                     "WHERE ride_id = ? AND driver_id = ? AND status = 'ACCEPTED'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, collectedFare);
            pstmt.setDouble(2, driverEarning);
            pstmt.setInt(3, rideId);
            pstmt.setInt(4, driverId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }
}