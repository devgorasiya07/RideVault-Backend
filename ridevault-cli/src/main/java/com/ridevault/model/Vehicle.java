package com.ridevault.model;

import java.sql.Date;

public class Vehicle {
    private int vehicleId;
    private int driverId;
    private String model;
    private int year;
    private String plateNo;
    private String category; 
    private boolean isActive;
    private boolean isVerified;
    private String insurancePolicyNo;
    private Date insuranceExpiryDate;

    public Vehicle() {}

    // Full constructor from Database
    public Vehicle(int vehicleId, int driverId, String model, int year, String plateNo, 
                   String category, boolean isActive, boolean isVerified, 
                   String insurancePolicyNo, Date insuranceExpiryDate) {
        this.vehicleId = vehicleId;
        this.driverId = driverId;
        this.model = model;
        this.year = year;
        this.plateNo = plateNo;
        this.category = category;
        this.isActive = isActive;
        this.isVerified = isVerified;
        this.insurancePolicyNo = insurancePolicyNo;
        this.insuranceExpiryDate = insuranceExpiryDate;
    }

    // UPDATED: Registration constructor now includes Insurance & Plate No!
    public Vehicle(int driverId, String model, int year, String plateNo, String category, 
                   String insurancePolicyNo, Date insuranceExpiryDate) {
        this.driverId = driverId;
        this.model = model;
        this.year = year;
        this.plateNo = plateNo;
        this.category = category;
        this.isActive = true;
        this.isVerified = false;
        this.insurancePolicyNo = insurancePolicyNo;
        this.insuranceExpiryDate = insuranceExpiryDate;
    }

    // --- GETTERS & SETTERS ---
    public int getVehicleId() { return vehicleId; }
    public int getDriverId() { return driverId; }
    public String getModel() { return model; }
    public int getYear() { return year; }
    public String getPlateNo() { return plateNo; }
    public String getCategory() { return category; }
    public boolean isActive() { return isActive; }
    public boolean isVerified() { return isVerified; }
    public String getInsurancePolicyNo() { return insurancePolicyNo; }
    public Date getInsuranceExpiryDate() { return insuranceExpiryDate; }
}