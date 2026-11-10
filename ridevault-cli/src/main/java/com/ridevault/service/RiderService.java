package com.ridevault.service;

import com.ridevault.dao.RiderDAO;
import com.ridevault.model.Rider;

public class RiderService {

    private RiderDAO riderDAO;

    public RiderService() {
        this.riderDAO = new RiderDAO();
    }

    // --- RIDER REGISTRATION LOGIC ---
    public boolean registerNewRider(String firstName, String middleName, String lastName, 
                                    String email, String phoneNo, String password) {
        
        // BUSINESS RULE 1: Password must be secure
        if (password == null || password.length() < 6) {
            System.out.println("[!] Registration Failed: Password must be at least 6 characters.");
            return false;
        }

        // BUSINESS RULE 2: Email must contain basic formatting
        if (email == null || !email.contains("@") || !email.contains(".")) {
            System.out.println("[!] Registration Failed: Invalid email format.");
            return false;
        }

        // If rules pass, pack the data into the Object
        Rider newRider = new Rider(firstName, middleName, lastName, email, phoneNo, password);

        // Send to DAO to save
        boolean isSaved = riderDAO.registerRider(newRider);
        
        if (isSaved) {
            System.out.println("Registration Successful! Welcome to RideVault, " + firstName + "!");
        } else {
            System.out.println("[!] Registration Failed: Email or Phone Number might already exist.");
        }

        return isSaved;
    }

    // --- RIDER LOGIN LOGIC ---
    public Rider login(String email, String password) {
        // We can add logic here later like "Account locked after 3 failed attempts"
        return riderDAO.loginRider(email, password);
    }
}