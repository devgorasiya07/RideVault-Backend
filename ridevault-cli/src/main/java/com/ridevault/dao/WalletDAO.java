package com.ridevault.dao;

import com.ridevault.DatabaseConnection;
import com.ridevault.model.Wallet;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class WalletDAO {

    public Wallet getWalletByDriverId(int driverId) {
        String selectSql = "SELECT * FROM WALLET WHERE Driver_Id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(selectSql)) {
             
            pstmt.setInt(1, driverId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return new Wallet(rs.getInt("wallet_id"), rs.getInt("driver_id"), rs.getDouble("balance"));
            } else {
                return createWalletForDriver(driverId);
            }
        } catch (SQLException e) {
            System.out.println("[!] Error fetching wallet: " + e.getMessage());
        }
        return null;
    }

    private Wallet createWalletForDriver(int driverId) {
        // ID is strictly omitted so PostgreSQL handles sequence generation
        String insertSql = "INSERT INTO WALLET (Driver_Id, Balance) VALUES (?, 0.00) RETURNING wallet_id";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
             
            pstmt.setInt(1, driverId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new Wallet(rs.getInt("wallet_id"), driverId, 0.00);
            }
        } catch (SQLException e) {
            System.out.println("[!] Error creating wallet: " + e.getMessage());
        }
        return null;
    }

    public boolean creditDriverWallet(int driverId, int rideId, double amount) {
        Wallet wallet = getWalletByDriverId(driverId);
        if (wallet == null) return false;

        String updateWalletSql = "UPDATE WALLET SET Balance = Balance + ? WHERE Wallet_Id = ?";
        // Txn_Id is omitted entirely to prevent sequence errors
        String insertTxnSql = "INSERT INTO WALLET_TRANSACTIONS (Wallet_Id, Ride_Id, Movement, Purpose, Amount, Balance_After) " +
                              "VALUES (?, ?, 'CREDIT', 'Ride Earning', ?, ?)";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // ACID Transaction Begins

            double newBalance = wallet.getBalance() + amount;

            try (PreparedStatement updateStmt = conn.prepareStatement(updateWalletSql)) {
                updateStmt.setDouble(1, amount);
                updateStmt.setInt(2, wallet.getWalletId());
                updateStmt.executeUpdate();
            }

            try (PreparedStatement insertStmt = conn.prepareStatement(insertTxnSql)) {
                insertStmt.setInt(1, wallet.getWalletId());
                insertStmt.setInt(2, rideId);
                insertStmt.setDouble(3, amount);
                insertStmt.setDouble(4, newBalance);
                insertStmt.executeUpdate();
            }

            conn.commit(); 
            return true;

        } catch (SQLException e) {
            System.out.println("[!] Financial transaction failed. Rolling back changes: " + e.getMessage());
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }

    public boolean withdrawFromWallet(int driverId, double amount) {
        if (amount <= 0) {
            System.out.println("[!] Withdrawal failed: Amount must be greater than zero.");
            return false;
        }

        Wallet wallet = getWalletByDriverId(driverId);
        if (wallet == null) return false;

        if (wallet.getBalance() < amount) {
            System.out.println("[!] Withdrawal failed: Insufficient funds. Available Balance: Rs. " + wallet.getBalance());
            return false;
        }

        String updateWalletSql = "UPDATE WALLET SET Balance = Balance - ? WHERE Wallet_Id = ?";
        // Txn_Id omitted again for safety
        String insertTxnSql = "INSERT INTO WALLET_TRANSACTIONS (Wallet_Id, Movement, Purpose, Amount, Balance_After) " +
                              "VALUES (?, 'DEBIT', 'Wallet Withdrawal', ?, ?)";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // ACID Transaction Begins

            double newBalance = wallet.getBalance() - amount;

            try (PreparedStatement updateStmt = conn.prepareStatement(updateWalletSql)) {
                updateStmt.setDouble(1, amount);
                updateStmt.setInt(2, wallet.getWalletId());
                updateStmt.executeUpdate();
            }

            try (PreparedStatement insertStmt = conn.prepareStatement(insertTxnSql)) {
                insertStmt.setInt(1, wallet.getWalletId());
                insertStmt.setDouble(2, amount);
                insertStmt.setDouble(3, newBalance);
                insertStmt.executeUpdate();
            }

            conn.commit(); 
            return true;

        } catch (SQLException e) {
            System.out.println("[!] Withdrawal transaction failed. Rolling back changes: " + e.getMessage());
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }
}