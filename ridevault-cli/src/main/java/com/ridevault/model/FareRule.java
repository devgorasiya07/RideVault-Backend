package com.ridevault.model;

public class FareRule {
    private int fareId;
    private String vehicleCategory;
    private double baseFare;
    private double perKmRate;
    private double perMinRate;
    private double minFare;

    public FareRule(int fareId, String vehicleCategory, double baseFare, double perKmRate, double perMinRate, double minFare) {
        this.fareId = fareId;
        this.vehicleCategory = vehicleCategory;
        this.baseFare = baseFare;
        this.perKmRate = perKmRate;
        this.perMinRate = perMinRate;
        this.minFare = minFare;
    }

    // Getters
    public int getFareId() { return fareId; }
    public String getVehicleCategory() { return vehicleCategory; }
    public double getBaseFare() { return baseFare; }
    public double getPerKmRate() { return perKmRate; }
    public double getPerMinRate() { return perMinRate; }
    public double getMinFare() { return minFare; }
}