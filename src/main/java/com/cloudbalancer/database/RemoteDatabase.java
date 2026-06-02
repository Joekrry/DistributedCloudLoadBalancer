package com.cloudbalancer.database;

import java.sql.*;

public class RemoteDatabase {
    private static final String DB_URL = "jdbc:mysql://mysql-db:3306/cloudbalancer";
    private static final String DB_USER = "cbuser";
    private static final String DB_PASS = "cbpassword";
    private Connection connection;
    private boolean isConnected = false;

    public RemoteDatabase() {
        connect();
    }

    public void connect() {
        try {
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            isConnected = true;
            System.out.println("MySQL connection established.");
        } catch (SQLException e) {
            isConnected = false;
            System.err.println("MySQL unavailable — working in offline mode: " + e.getMessage());
        }
    }

    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed() && connection.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void close() {
        try {
            if (connection != null) connection.close();
        } catch (SQLException e) {
            System.err.println("Error closing MySQL: " + e.getMessage());
        }
    }
}
