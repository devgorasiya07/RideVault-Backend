package com.ridevault.model;

import java.sql.Timestamp;

public abstract class User {
    
    // Protected means ONLY child classes (Rider/Driver) can access these directly
    protected int id; 
    protected String firstName;
    protected String middleName;
    protected String lastName;
    protected String email;
    protected String phoneNo;
    protected String passwordHash;
    protected Timestamp createdAt;

    // 1. Empty Constructor
    public User() {}

    // 2. Full Constructor (Reading from Database)
    public User(int id, String firstName, String middleName, String lastName, 
                String email, String phoneNo, String passwordHash, Timestamp createdAt) {
        this.id = id;
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.email = email;
        this.phoneNo = phoneNo;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
    }

    // 3. Registration Constructor (Writing to Database)
    public User(String firstName, String middleName, String lastName, 
                String email, String phoneNo, String passwordHash) {
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.email = email;
        this.phoneNo = phoneNo;
        this.passwordHash = passwordHash;
    }

    // --- GETTERS & SETTERS ---
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getMiddleName() { return middleName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNo() { return phoneNo; }
    public void setPhoneNo(String phoneNo) { this.phoneNo = phoneNo; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}