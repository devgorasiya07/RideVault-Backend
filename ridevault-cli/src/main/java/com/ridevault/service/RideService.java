package com.ridevault.service;

import com.ridevault.DatabaseConnection;
import com.ridevault.dao.PricingDAO;
import com.ridevault.dao.RideDAO;
import com.ridevault.dao.WalletDAO;
import com.ridevault.model.FareRule;
import com.ridevault.model.Location;
import com.ridevault.model.Ride;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.List;

public class RideService {

    private final WalletDAO walletDAO = new WalletDAO();
    private final PricingDAO pricingDAO = new PricingDAO(); 
    private final RideDAO rideDAO = new RideDAO(); 
    private static final DecimalFormat df = new DecimalFormat("0.00");

    // ==========================================
    // METHOD OVERLOAD: Safe default fallback
    // ==========================================
    public double calculateFare(Location pickup, Location dropoff) {
        return calculateFare(pickup, dropoff, "Mini");
    }

    // ==========================================
    // 1. REAL-WORLD DYNAMIC PRICING ENGINE
    // ==========================================
    public double calculateFare(Location pickup, Location dropoff, String vehicleCategory) {
        
        FareRule rule = pricingDAO.getFareRuleByCategory(vehicleCategory);
        
        double baseFare = (rule != null) ? rule.getBaseFare() : 50.00;
        double ratePerKm = (rule != null) ? rule.getPerKmRate() : 10.00;
        double ratePerMin = (rule != null) ? rule.getPerMinRate() : 1.50;
        double minimumFare = (rule != null) ? rule.getMinFare() : 70.00;
        
        double platformBookingFee = 0.0; 

        double distanceKm = calculateRealWorldDistance(pickup.getLat(), pickup.getLng(), dropoff.getLat(), dropoff.getLng()); 
        double trafficModifier = getRealTimeTrafficModifier(); 
        double estimatedTimeMin = (distanceKm / 0.5) * trafficModifier; 
        double surgeMultiplier = calculateSupplyDemandSurge(pickup);

        double routeSubtotal = baseFare + (distanceKm * ratePerKm) + (estimatedTimeMin * ratePerMin);
        double surgedFare = routeSubtotal * surgeMultiplier;
        double fareBeforeFees = Math.max(surgedFare, minimumFare);
        double finalCalculatedFare = fareBeforeFees + platformBookingFee;

        return Math.round(finalCalculatedFare);
    }

    // ==========================================
    // GPS Spherical Distance Engine (Haversine)
    // ==========================================
    private double calculateRealWorldDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; 
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                 + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                 * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distanceKm = R * c; 
        
        distanceKm = Math.max(distanceKm, 1.5);
        return Math.round(distanceKm * 10.0) / 10.0;
    }

    private double getRealTimeTrafficModifier() {
        double traffic = 1.0 + (Math.random() * 0.5);
        return Math.round(traffic * 10.0) / 10.0;
    }

    private double calculateSupplyDemandSurge(Location pickupZone) {
        int activeRiders = 0;
        int activeDrivers = 0;
        
        String demandSql = "SELECT COUNT(*) FROM Ride WHERE Status = 'PENDING' AND Pickup_Address = ?";
        String supplySql = "SELECT COUNT(*) FROM Driver WHERE Driver_Id NOT IN (SELECT Driver_Id FROM Ride WHERE Status = 'ACCEPTED')";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmtDemand = conn.prepareStatement(demandSql);
             PreparedStatement pstmtSupply = conn.prepareStatement(supplySql)) {
             
            pstmtDemand.setString(1, pickupZone.getDisplayName());
            ResultSet rsDemand = pstmtDemand.executeQuery();
            if (rsDemand.next()) activeRiders = rsDemand.getInt(1);
            
            ResultSet rsSupply = pstmtSupply.executeQuery();
            if (rsSupply.next()) activeDrivers = rsSupply.getInt(1);
            
        } catch (SQLException e) {
            System.out.println("\u001B[31m[!] Error calculating live surge metrics: " + e.getMessage() + "\u001B[0m");
        }

        if (activeDrivers == 0) activeDrivers = 1;
        double ratio = (double) activeRiders / activeDrivers;
        double surgeMultiplier = 1.0; 
        
        if (ratio > 1.2) {
            double surgeSensitivity = 0.35; 
            surgeMultiplier = 1.0 + ((ratio - 1.2) * surgeSensitivity);
        }
        
        return Math.min(surgeMultiplier, 3.5);
    }

    // ==========================================
    // 2. REQUEST A RIDE (Rider Portal)
    // ==========================================
    public void requestRide(int riderId, String vehicleCategory, Location pickup, Location dropoff, double estimatedFare) {
        // Step 1: Look up the Fare_Id for the requested category
        FareRule rule = pricingDAO.getFareRuleByCategory(vehicleCategory);
        if (rule == null) {
            System.out.println("\u001B[31m[!] System Error: Could not find pricing rule for " + vehicleCategory + "\u001B[0m");
            return;
        }
        
        // Step 2: Create the ride using the Fare_Id
        Ride newRide = new Ride(riderId, rule.getFareId(), pickup.getDisplayName(), pickup.getLat(), pickup.getLng(), 
                                dropoff.getDisplayName(), dropoff.getLat(), dropoff.getLng(), estimatedFare);
                                
        if (rideDAO.createRide(newRide)) {
            System.out.println("\u001B[32m[SUCCESS] Ride requested successfully! Waiting for a driver to accept.\u001B[0m");
        }
    }

    // ==========================================
    // 3. SHOW PENDING RIDES (Driver Portal)
    // ==========================================
    public void showPendingRides(String driverVehicleCategory) {
        List<Ride> pending = rideDAO.getPendingRides(driverVehicleCategory);
        if (pending.isEmpty()) {
            System.out.println("\u001B[33mNo pending rides available in your area right now for category: " + driverVehicleCategory + "\u001B[0m");
        } else {
            for (Ride r : pending) {
                System.out.println("Ride ID: [" + r.getRideId() + "] | Type: " + driverVehicleCategory + " | Route: " + 
                                   r.getPickupAddress() + " -> " + r.getDropoffAddress() + 
                                   " | Fare: Rs. " + r.getFinalFare());
            }
        }
    }

    // ==========================================
    // 4. ACCEPT A RIDE (Driver Portal)
    // ==========================================
    public void acceptRide(int rideId, int driverId, int vehicleId, String driverVehicleCategory) {
        // We pass the exact Vehicle_Id down to the database now!
        if (rideDAO.acceptRideInDB(rideId, driverId, vehicleId, driverVehicleCategory)) {
            System.out.println("\u001B[32m[SUCCESS] You have accepted Ride ID " + rideId + "!\u001B[0m");
        } else {
            System.out.println("\u001B[31m[!] Could not accept. Ride may not exist, is already taken, or does not match your vehicle category (" + driverVehicleCategory + ").\u001B[0m");
        }
    }

    // ==========================================
    // 5. FETCH EXPECTED FARE (Fraud Prevention)
    // ==========================================
    public double getExpectedFareForRide(int rideId) {
        return rideDAO.getExpectedFareForRide(rideId);
    }

    // ==========================================
    // 6. COMPLETE A RIDE (State Management & Payout)
    // ==========================================
    public void completeRide(int rideId, int driverId, double collectedFare) {
        double driverEarning = Double.parseDouble(df.format(collectedFare * 0.80));

        boolean isRideUpdated = rideDAO.completeRideInDB(rideId, driverId, collectedFare, driverEarning);
        
        if (isRideUpdated) {
            boolean walletSuccess = walletDAO.creditDriverWallet(driverId, rideId, driverEarning);
            
            if (walletSuccess) {
                System.out.println("\u001B[32m[SUCCESS] Ride Completed! Rs. " + driverEarning + " (80% cut) has been credited to your Wallet.\u001B[0m");
            } else {
                System.out.println("\u001B[31m[CRITICAL] Ride completed, but wallet credit failed. ACID Rollback triggered.\u001B[0m");
            }
        } else {
            System.out.println("\u001B[31m[!] Could not complete ride. Please verify the Ride ID.\u001B[0m");
        }
    }
}