package com.ridevault.service;

import com.ridevault.dao.DriverDAO;
import com.ridevault.dao.VehicleDAO;
import com.ridevault.model.Driver;
import com.ridevault.model.Vehicle;

public class DriverService {

    private DriverDAO driverDAO;
    private VehicleDAO vehicleDAO;

    public DriverService() {
        this.driverDAO = new DriverDAO();
        this.vehicleDAO = new VehicleDAO();
    }

    // UPDATED: Now accepts model, year, plate, and insurance details!
    public boolean registerNewDriver(String firstName, String middleName, String lastName, 
                                     String email, String phoneNo, String password, String licenseNo, 
                                     String vehicleCategory, String model, int year, String plateNo, 
                                     String insurancePolicyNo, java.sql.Date insuranceExpiryDate) {
        
        if (password == null || password.length() < 6) {
            System.out.println("[!] Registration Failed: Password must be at least 6 characters.");
            return false;
        }

        if (email == null || !email.contains("@") || !email.contains(".")) {
            System.out.println("[!] Registration Failed: Invalid email format.");
            return false;
        }

        if (licenseNo == null || licenseNo.trim().isEmpty()) {
            System.out.println("[!] Registration Failed: A valid License Number is required.");
            return false;
        }

        Driver newDriver = new Driver(firstName, middleName, lastName, email, phoneNo, password, licenseNo);
        boolean isSaved = driverDAO.registerDriver(newDriver);
        
        if (isSaved) {
            Driver registeredDriver = driverDAO.loginDriver(email, password);
            
            if (registeredDriver != null) {
                // UPDATED: Passing the real user inputs into the Vehicle object
                Vehicle newVehicle = new Vehicle(registeredDriver.getId(), model, year, plateNo, vehicleCategory, insurancePolicyNo, insuranceExpiryDate);
                vehicleDAO.registerVehicle(newVehicle);
            }
            
            System.out.println("\u001B[32mDriver Registration Successful! Welcome aboard, " + firstName + ".\u001B[0m");
        } else {
            System.out.println("\u001B[31m[!] Registration Failed: Email, Phone, or License already exists.\u001B[0m");
        }

        return isSaved;
    }

    public Driver login(String email, String password) {
        return driverDAO.loginDriver(email, password);
    }
}