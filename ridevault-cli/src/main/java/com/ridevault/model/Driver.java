package com.ridevault.model;

import java.sql.Timestamp;

public class Driver extends User {
    
    // Driver-specific attributes matching your DDL perfectly!
    private String licenseNo;
    private double avgRating;
    private int totalRides;
    private boolean isVerified;

    public Driver() {
        super();
    }

    // Database Read Constructor
    public Driver(int id, String firstName, String middleName, String lastName, 
                  String email, String phoneNo, String passwordHash, 
                  String licenseNo, double avgRating, int totalRides, boolean isVerified, Timestamp createdAt) {
        
        super(id, firstName, middleName, lastName, email, phoneNo, passwordHash, createdAt);
        this.licenseNo = licenseNo;
        this.avgRating = avgRating;
        this.totalRides = totalRides;
        this.isVerified = isVerified;
    }

    // Registration Constructor
    public Driver(String firstName, String middleName, String lastName, 
                  String email, String phoneNo, String passwordHash, String licenseNo) {
        
        super(firstName, middleName, lastName, email, phoneNo, passwordHash);
        this.licenseNo = licenseNo;
        this.avgRating = 0.0;     // Default starting rating
        this.totalRides = 0;      // Default starting rides
        this.isVerified = false;  // Default verification
    }

    // --- GETTERS & SETTERS ---
    public String getLicenseNo() { return licenseNo; }
    public void setLicenseNo(String licenseNo) { this.licenseNo = licenseNo; }

    public double getAvgRating() { return avgRating; }
    public void setAvgRating(double avgRating) { this.avgRating = avgRating; }

    public int getTotalRides() { return totalRides; }
    public void setTotalRides(int totalRides) { this.totalRides = totalRides; }

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean isVerified) { this.isVerified = isVerified; }
}