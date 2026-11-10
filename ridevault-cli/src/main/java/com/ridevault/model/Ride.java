package com.ridevault.model;
import java.sql.Timestamp;

public class Ride {
    private int rideId;
    private int riderId;       
    private Integer driverId;  
    private Integer vehicleId; // <--- UPDATED: Follows your DDL perfectly
    private Integer fareId;    // <--- UPDATED: Follows your DDL perfectly
    private String pickupAddress;
    private double pickupLat;  
    private double pickupLng;  
    private String dropoffAddress;
    private double dropoffLat; 
    private double dropoffLng; 
    private String status;     
    private double finalFare; 
    private Timestamp requestedAt;

    public Ride() {}

    // Constructor for Reading from Database (This matches the RideDAO exactly!)
    public Ride(int rideId, int riderId, Integer driverId, Integer vehicleId, Integer fareId,
                String pickupAddress, double pickupLat, double pickupLng, 
                String dropoffAddress, double dropoffLat, double dropoffLng, 
                String status, double finalFare, Timestamp requestedAt) {
        this.rideId = rideId;
        this.riderId = riderId;
        this.driverId = driverId;
        this.vehicleId = vehicleId;
        this.fareId = fareId;
        this.pickupAddress = pickupAddress;
        this.pickupLat = pickupLat;
        this.pickupLng = pickupLng;
        this.dropoffAddress = dropoffAddress;
        this.dropoffLat = dropoffLat;
        this.dropoffLng = dropoffLng;
        this.status = status;
        this.finalFare = finalFare; 
        this.requestedAt = requestedAt;
    }

    // Constructor for CREATING a new Pending Ride
    public Ride(int riderId, Integer fareId, String pickupAddress, double pickupLat, double pickupLng, 
                String dropoffAddress, double dropoffLat, double dropoffLng, double finalFare) {
        this.riderId = riderId;
        this.driverId = null; 
        this.vehicleId = null; // Stays null until a driver accepts!
        this.fareId = fareId;  // This tells us what category they paid for
        this.pickupAddress = pickupAddress;
        this.pickupLat = pickupLat;   
        this.pickupLng = pickupLng;   
        this.dropoffAddress = dropoffAddress;
        this.dropoffLat = dropoffLat; 
        this.dropoffLng = dropoffLng; 
        this.status = "PENDING";
        this.finalFare = finalFare; 
    }

    // --- GETTERS ---
    public int getRideId() { return rideId; }
    public int getRiderId() { return riderId; }
    public Integer getDriverId() { return driverId; }
    public Integer getVehicleId() { return vehicleId; }
    public Integer getFareId() { return fareId; }
    public String getPickupAddress() { return pickupAddress; }
    public double getPickupLat() { return pickupLat; }
    public double getPickupLng() { return pickupLng; }
    public String getDropoffAddress() { return dropoffAddress; }
    public double getDropoffLat() { return dropoffLat; }
    public double getDropoffLng() { return dropoffLng; }
    public String getStatus() { return status; }
    public double getFinalFare() { return finalFare; }
    public Timestamp getRequestedAt() { return requestedAt; }
}