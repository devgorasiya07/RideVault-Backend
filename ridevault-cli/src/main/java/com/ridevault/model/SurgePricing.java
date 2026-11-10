package com.ridevault.model;

public class SurgePricing {
    private int surgeId;
    private String zoneName;
    private double multiplier;

    public SurgePricing(int surgeId, String zoneName, double multiplier) {
        this.surgeId = surgeId;
        this.zoneName = zoneName;
        this.multiplier = multiplier;
    }

    public int getSurgeId() { return surgeId; }
    public String getZoneName() { return zoneName; }
    public double getMultiplier() { return multiplier; }
}