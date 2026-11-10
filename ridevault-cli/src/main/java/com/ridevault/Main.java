package com.ridevault;

import com.ridevault.dao.WalletDAO;
import com.ridevault.dao.VehicleDAO;
import com.ridevault.model.Driver;
import com.ridevault.model.Location;
import com.ridevault.model.Rider;
import com.ridevault.model.Vehicle;
import com.ridevault.model.Wallet;
import com.ridevault.service.DriverService;
import com.ridevault.service.RideService;
import com.ridevault.service.RiderService;

import java.util.Scanner;

public class Main {

    public static final String RESET = "\u001B[0m";
    public static final String CYAN = "\u001B[36m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String RED = "\u001B[31m";
    public static final String BOLD = "\u001B[1m";

    static Scanner scanner = new Scanner(System.in);
    
    // Core Services & DAOs initialized once
    static RiderService riderService = new RiderService();
    static DriverService driverService = new DriverService();
    static RideService rideService = new RideService();
    static WalletDAO walletDAO = new WalletDAO();
    static VehicleDAO vehicleDAO = new VehicleDAO();

    public static void main(String[] args) {
        boolean isRunning = true;
        printLogo();

        while (isRunning) {
            System.out.println("\n" + CYAN + BOLD + "================================================" + RESET);
            System.out.println(CYAN + BOLD + "             WELCOME TO RIDEVAULT               " + RESET);
            System.out.println(CYAN + BOLD + "================================================" + RESET);
            System.out.println("  [1] Enter as RIDER");
            System.out.println("  [2] Enter as DRIVER");
            System.out.println("  [3] Shut Down System");
            System.out.println(CYAN + "------------------------------------------------" + RESET);
            System.out.print(YELLOW + ">>> Choose your portal: " + RESET);

            String choice = scanner.nextLine();

            switch (choice) {
                case "1": riderPortal(); break;
                case "2": driverPortal(); break;
                case "3":
                    System.out.println("\n" + GREEN + "[SYSTEM] Shutting down RideVault databases... Goodbye!" + RESET + "\n");
                    isRunning = false;
                    break;
                default:
                    System.out.println(RED + "[!] Invalid option. Please enter 1, 2, or 3." + RESET);
            }
        }
        scanner.close();
    }

    // ==========================================
    //              RIDER PORTAL
    // ==========================================
    private static void riderPortal() {
        boolean inRiderMenu = true;
        while (inRiderMenu) {
            System.out.println("\n" + GREEN + BOLD + "+++ RIDER PORTAL +++" + RESET);
            System.out.println("  [1] Register New Account");
            System.out.println("  [2] Login");
            System.out.println("  [3] Go Back to Main Menu");
            System.out.print(YELLOW + ">>> Option: " + RESET);
            
            String choice = scanner.nextLine();
            
            if (choice.equals("1")) {
                System.out.println("\n" + GREEN + "--- RIDER REGISTRATION ---" + RESET);
                System.out.print("First Name   : "); String fName = scanner.nextLine();
                System.out.print("Middle Name  : "); String mName = scanner.nextLine(); if(mName.isEmpty()) mName = null;
                System.out.print("Last Name    : "); String lName = scanner.nextLine();
                System.out.print("Email        : "); String email = scanner.nextLine();
                System.out.print("Phone        : "); String phone = scanner.nextLine();
                System.out.print("Password     : "); String pass = scanner.nextLine();
                
                System.out.println(YELLOW + "\n[SYSTEM] Processing registration..." + RESET);
                riderService.registerNewRider(fName, mName, lName, email, phone, pass);
                
            } else if (choice.equals("2")) {
                System.out.println("\n" + GREEN + "--- RIDER LOGIN ---" + RESET);
                System.out.print("Email        : "); String email = scanner.nextLine();
                System.out.print("Password     : "); String pass = scanner.nextLine();
                
                System.out.println(YELLOW + "\n[SYSTEM] Authenticating..." + RESET);
                Rider currentRider = riderService.login(email, pass);
                if (currentRider != null) {
                    System.out.println(GREEN + "[SUCCESS] Login accepted." + RESET);
                    riderDashboard(currentRider);
                } else {
                    System.out.println(RED + "[!] Login Failed. Incorrect credentials." + RESET);
                }
            } else if (choice.equals("3")) {
                inRiderMenu = false;
            } else {
                System.out.println(RED + "[!] Invalid option." + RESET);
            }
        }
    }

    private static void riderDashboard(Rider currentRider) {
        boolean loggedIn = true;
        while (loggedIn) {
            System.out.println("\n" + YELLOW + BOLD + "================================================" + RESET);
            System.out.println(YELLOW + BOLD + "                RIDER DASHBOARD                 " + RESET);
            System.out.println(YELLOW + BOLD + "================================================" + RESET);
            System.out.println("  [1] Request a Ride");
            System.out.println("  [2] View Profile");
            System.out.println("  [3] Logout");
            System.out.println(YELLOW + "------------------------------------------------" + RESET);
            System.out.print(">>> Option: ");
            
            String choice = scanner.nextLine();
            
            if (choice.equals("1")) {
                System.out.println("\n" + CYAN + "--- REQUEST NEW RIDE ---" + RESET);
                
                System.out.println(YELLOW + "Select Pickup Location:" + RESET);
                Location pickup = chooseLocation();
                
                System.out.println(YELLOW + "\nSelect Drop-off Location:" + RESET);
                Location dropoff = chooseLocation();
                
                if (pickup == dropoff) {
                    System.out.println(RED + "[!] Invalid: Pickup and Drop-off cannot be the same." + RESET);
                    continue; 
                }

                System.out.println(YELLOW + "\n[SYSTEM] Calculating optimal routes and fetching live fares..." + RESET);
                
                String[] vehicleTypes = {"Mini", "Sedan", "SUV", "Premium", "Auto", "Bike"};
                double[] calculatedFares = new double[vehicleTypes.length];
                
                System.out.println(CYAN + BOLD + "\n>>> Available Rides & Fares:" + RESET);
                for (int i = 0; i < vehicleTypes.length; i++) {
                    calculatedFares[i] = rideService.calculateFare(pickup, dropoff, vehicleTypes[i]);
                    System.out.println("  [" + (i + 1) + "] " + vehicleTypes[i] + " \t- Rs. " + calculatedFares[i]);
                }
                
                int selectedVehicleIndex = -1;
                while (true) {
                    System.out.print(YELLOW + "\n>>> Choose your ride type (1-" + vehicleTypes.length + "): " + RESET);
                    try {
                        selectedVehicleIndex = Integer.parseInt(scanner.nextLine()) - 1;
                        if (selectedVehicleIndex >= 0 && selectedVehicleIndex < vehicleTypes.length) {
                            break; 
                        }
                        System.out.println(RED + "[!] Please select a valid number from the list." + RESET);
                    } catch (NumberFormatException e) {
                        System.out.println(RED + "[!] Invalid input. You must enter a number." + RESET);
                    }
                }
                
                String chosenVehicle = vehicleTypes[selectedVehicleIndex];
                double finalEstimatedFare = calculatedFares[selectedVehicleIndex];
                
                System.out.println(CYAN + BOLD + "\n>>> Route: " + pickup.getDisplayName() + " to " + dropoff.getDisplayName() + RESET);
                System.out.println(CYAN + BOLD + ">>> Selected: " + chosenVehicle + " | Final Fare: Rs. " + finalEstimatedFare + RESET);
                System.out.print(YELLOW + "Confirm this ride booking? (Y/N): " + RESET);
                String confirm = scanner.nextLine();
                
                if (confirm.equalsIgnoreCase("Y")) {
                    rideService.requestRide(currentRider.getId(), chosenVehicle, pickup, dropoff, finalEstimatedFare);
                } else {
                    System.out.println(RED + "[!] Ride request cancelled by user." + RESET);
                }
                
            } else if (choice.equals("2")) {
                System.out.println("\n" + CYAN + "--- USER PROFILE ---" + RESET);
                System.out.println("Name   : " + currentRider.getFirstName() + " " + currentRider.getLastName());
                System.out.println("Email  : " + currentRider.getEmail());
                System.out.println("Phone  : " + currentRider.getPhoneNo());
            } else if (choice.equals("3")) {
                System.out.println(GREEN + "\n[SYSTEM] Logging out..." + RESET);
                loggedIn = false;
            } else {
                System.out.println(RED + "[!] Invalid option." + RESET);
            }
        }
    }

    // ==========================================
    //              DRIVER PORTAL
    // ==========================================
    private static void driverPortal() {
        boolean inDriverMenu = true;
        while (inDriverMenu) {
            System.out.println("\n" + CYAN + BOLD + "+++ DRIVER PORTAL +++" + RESET);
            System.out.println("  [1] Register as Driver");
            System.out.println("  [2] Login");
            System.out.println("  [3] Go Back to Main Menu");
            System.out.print(YELLOW + ">>> Option: " + RESET);
            
            String choice = scanner.nextLine();
            if (choice.equals("1")) {
                System.out.println("\n" + CYAN + "--- DRIVER REGISTRATION ---" + RESET);
                System.out.print("First Name   : "); String fName = scanner.nextLine();
                System.out.print("Middle Name  : "); String mName = scanner.nextLine(); if(mName.isEmpty()) mName = null;
                System.out.print("Last Name    : "); String lName = scanner.nextLine();
                System.out.print("Email        : "); String email = scanner.nextLine();
                System.out.print("Phone        : "); String phone = scanner.nextLine();
                System.out.print("Password     : "); String pass = scanner.nextLine();
                System.out.print("License No.  : "); String license = scanner.nextLine();
                
                System.out.println("\n" + CYAN + "--- VEHICLE DETAILS ---" + RESET);
                System.out.print("Vehicle Type (Mini/Sedan/SUV/Premium/Auto/Bike): "); String vType = scanner.nextLine();
                System.out.print("Vehicle Model (e.g., Maruti Swift): "); String vModel = scanner.nextLine();
                System.out.print("Vehicle Year (e.g., 2021)       : "); int vYear = Integer.parseInt(scanner.nextLine());
                System.out.print("Number Plate (e.g., MH-12-AB-1234): "); String vPlate = scanner.nextLine();
                System.out.print("Insurance Policy Number         : "); String vInsurance = scanner.nextLine();
                System.out.print("Insurance Expiry (YYYY-MM-DD)   : "); String vExpiryStr = scanner.nextLine();
                
                // Convert String to SQL Date
                java.sql.Date vExpiryDate = java.sql.Date.valueOf(vExpiryStr);
                
                System.out.println(YELLOW + "\n[SYSTEM] Processing registration..." + RESET);
                driverService.registerNewDriver(fName, mName, lName, email, phone, pass, license, 
                                                vType, vModel, vYear, vPlate, vInsurance, vExpiryDate);
            }
            
            else if (choice.equals("2")) {
                System.out.println("\n" + CYAN + "--- DRIVER LOGIN ---" + RESET);
                System.out.print("Email        : "); String email = scanner.nextLine();
                System.out.print("Password     : "); String pass = scanner.nextLine();
                
                System.out.println(YELLOW + "\n[SYSTEM] Authenticating..." + RESET);
                Driver currentDriver = driverService.login(email, pass);
                
                if (currentDriver != null) {
                    Vehicle driverVehicle = vehicleDAO.getVehicleByDriverId(currentDriver.getId());
                    if (driverVehicle != null) {
                        System.out.println(GREEN + "[SUCCESS] Login accepted. Welcome, driving a " + driverVehicle.getCategory() + "!" + RESET);
                        driverDashboard(currentDriver, driverVehicle);
                    } else {
                        System.out.println(RED + "[!] Login Failed. No active vehicle assigned to this driver account." + RESET);
                    }
                } else {
                    System.out.println(RED + "[!] Login Failed. Incorrect credentials." + RESET);
                }
            } else if (choice.equals("3")) {
                inDriverMenu = false;
            } else {
                System.out.println(RED + "[!] Invalid option." + RESET);
            }
        }
    }

    private static void driverDashboard(Driver currentDriver, Vehicle driverVehicle) {
        boolean loggedIn = true;
        while (loggedIn) {
            System.out.println("\n" + YELLOW + BOLD + "================================================" + RESET);
            System.out.println(YELLOW + BOLD + "               DRIVER DASHBOARD                 " + RESET);
            System.out.println(YELLOW + BOLD + "================================================" + RESET);
            System.out.println("  [1] View Available Rides (" + driverVehicle.getCategory() + ")");
            System.out.println("  [2] Accept a Ride");
            System.out.println("  [3] Complete a Ride");
            System.out.println("  [4] Check Wallet Balance");
            System.out.println("  [5] Withdraw Funds"); 
            System.out.println("  [6] Logout");
            System.out.println(YELLOW + "------------------------------------------------" + RESET);
            System.out.print(">>> Option: ");
            
            String choice = scanner.nextLine();
            
            if (choice.equals("1")) {
                System.out.println("\n" + CYAN + "--- SCANNING FOR " + driverVehicle.getCategory().toUpperCase() + " RIDES ---" + RESET);
                rideService.showPendingRides(driverVehicle.getCategory());
                
            } else if (choice.equals("2")) {
                System.out.print("\n" + CYAN + "Enter the Ride ID you want to accept: " + RESET);
                try {
                    int rideId = Integer.parseInt(scanner.nextLine());
                    rideService.acceptRide(rideId, currentDriver.getId(), driverVehicle.getVehicleId(), driverVehicle.getCategory());
                } catch (NumberFormatException e) {
                    System.out.println(RED + "[!] Please enter a valid numerical ID." + RESET);
                }
                
            } else if (choice.equals("3")) {
                try {
                    System.out.print("\n" + CYAN + "Enter the Ride ID you just completed: " + RESET);
                    int rideId = Integer.parseInt(scanner.nextLine());
                    
                    double expectedFare = rideService.getExpectedFareForRide(rideId);
                    
                    if (expectedFare <= 0) {
                        System.out.println(RED + "[!] Invalid Ride ID or ride is already completed." + RESET);
                        continue; 
                    }
                    
                    System.out.println(YELLOW + "Expected System Fare for this ride: Rs. " + expectedFare + RESET);
                    double collectedFare = -1; 
                    
                    while (collectedFare < expectedFare) {
                        System.out.print(CYAN + "Enter the Final Fare collected from Rider (Rs.): " + RESET);
                        collectedFare = Double.parseDouble(scanner.nextLine());
                        
                        if (collectedFare < expectedFare) {
                            System.out.println(RED + "[!] System Error: Collected amount cannot be less than the calculated Final Fare (Rs. " + expectedFare + ")." + RESET);
                        }
                    }
                    
                    rideService.completeRide(rideId, currentDriver.getId(), collectedFare);
                    
                } catch (NumberFormatException e) {
                    System.out.println(RED + "[!] Please enter a valid numerical value." + RESET);
                }
                
            } else if (choice.equals("4")) {
                System.out.println("\n" + CYAN + "--- WALLET BALANCE ---" + RESET);
                Wallet myWallet = walletDAO.getWalletByDriverId(currentDriver.getId());
                if (myWallet != null) {
                    System.out.println(GREEN + "Current Available Balance: Rs. " + myWallet.getBalance() + RESET);
                } else {
                    System.out.println(RED + "Could not retrieve wallet information." + RESET);
                }

            } else if (choice.equals("5")) {
                System.out.println("\n" + CYAN + "--- WITHDRAW FUNDS ---" + RESET);
                Wallet myWallet = walletDAO.getWalletByDriverId(currentDriver.getId());
                
                if (myWallet != null) {
                    System.out.println("Available Balance: Rs. " + myWallet.getBalance());
                    if (myWallet.getBalance() > 0) {
                        System.out.print(YELLOW + "Enter amount to withdraw (Rs.): " + RESET);
                        try {
                            double amount = Double.parseDouble(scanner.nextLine());
                            boolean success = walletDAO.withdrawFromWallet(currentDriver.getId(), amount);
                            
                            if (success) {
                                System.out.println(GREEN + "[SUCCESS] Withdraw Successfully!! Rs. " + amount + " has been deducted from your wallet." + RESET);
                            }
                        } catch (NumberFormatException e) {
                            System.out.println(RED + "[!] Please enter a valid numerical value." + RESET);
                        }
                    } else {
                        System.out.println(RED + "[!] You have no available funds to withdraw." + RESET);
                    }
                } else {
                    System.out.println(RED + "Could not retrieve wallet information." + RESET);
                }

            } else if (choice.equals("6")) {
                System.out.println(GREEN + "\n[SYSTEM] Logging out..." + RESET);
                loggedIn = false;
            } else {
                System.out.println(RED + "[!] Invalid option." + RESET);
            }
        }
    }

    // ==========================================
    //              HELPER METHODS
    // ==========================================
    private static Location chooseLocation() {
        Location[] locations = Location.values();
        for (int i = 0; i < locations.length; i++) {
            System.out.println("  [" + (i + 1) + "] " + locations[i].getDisplayName());
        }
        
        while (true) {
            System.out.print(">>> Choose a location number: ");
            try {
                int index = Integer.parseInt(scanner.nextLine()) - 1;
                if (index >= 0 && index < locations.length) {
                    return locations[index];
                }
                System.out.println(RED + "[!] Please select a valid number from the list." + RESET);
            } catch (NumberFormatException e) {
                System.out.println(RED + "[!] Invalid input. You must enter a number." + RESET);
            }
        }
    }

    private static void printLogo() {
        System.out.println(CYAN + BOLD);
        System.out.println("  _____  _     _     __      __         _  _   ");
        System.out.println(" |  __ \\(_)   | |    \\ \\    / /        | || |  ");
        System.out.println(" | |__) |_  __| | ___ \\ \\  / /_ _ _   _| || |_ ");
        System.out.println(" |  _  /| |/ _` |/ _ \\ \\ \\/ / _` | | | | || __|");
        System.out.println(" | | \\ \\| | (_| |  __/  \\  / (_| | |_| | || |_ ");
        System.out.println(" |_|  \\_\\_|\\__,_|\\___|   \\/ \\__,_|\\__,_|_| \\__|");
        System.out.println(RESET);
    }
}