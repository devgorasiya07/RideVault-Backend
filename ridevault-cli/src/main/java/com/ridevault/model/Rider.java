package com.ridevault.model;

import java.sql.Timestamp;

public class Rider extends User {
    
    // Rider-specific attribute
    private boolean isVerified;

    public Rider() {
        super();
    }

    // Database Read Constructor
    public Rider(int id, String firstName, String middleName, String lastName, 
                 String email, String phoneNo, String passwordHash, 
                 boolean isVerified, Timestamp createdAt) {
        
        super(id, firstName, middleName, lastName, email, phoneNo, passwordHash, createdAt);
        this.isVerified = isVerified;
    }

    // Registration Constructor
    public Rider(String firstName, String middleName, String lastName, 
                 String email, String phoneNo, String passwordHash) {
        
        super(firstName, middleName, lastName, email, phoneNo, passwordHash);
        this.isVerified = false; // Default for new riders
    }

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean isVerified) { this.isVerified = isVerified; }
}