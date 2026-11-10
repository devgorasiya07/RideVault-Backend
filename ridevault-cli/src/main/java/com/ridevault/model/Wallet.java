package com.ridevault.model;

public class Wallet {
    private int walletId;
    private int driverId;
    private double balance;

    public Wallet(int walletId, int driverId, double balance) {
        this.walletId = walletId;
        this.driverId = driverId;
        this.balance = balance;
    }

    public int getWalletId() { return walletId; }
    public int getDriverId() { return driverId; }
    
    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
}