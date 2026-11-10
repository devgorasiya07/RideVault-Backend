package com.ridevault;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    
    // 1. Database Credentials (KEEP YOURS EXACTLY AS THEY WERE!)
    private static final String URL = "jdbc:postgresql://localhost:5432/postgres";
    private static final String USER = "postgres"; 
    private static final String PASSWORD = "12345678"; 

    // 2. THE FIX: The Factory Method
    // Notice how we removed the 'if(connection == null)' check. 
    // Now, it hands out a brand new connection every single time it is called!
    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            System.out.println("[!] Database Connection Failed!");
            e.printStackTrace();
            return null;
        }
    }
}