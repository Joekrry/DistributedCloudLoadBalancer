package com.cloudbalancer.database;

import java.sql.*;

public class LocalDatabase {
    private static final String DB_URL = "jdbc:sqlite:cloudbalancer_local.db";
    private Connection connection;

    public LocalDatabase() {
        connect();
        initialiseSchema();
    }

    private void connect() {
        try {
            connection = DriverManager.getConnection(DB_URL);
            System.out.println("SQLite connection established.");
        } catch (SQLException e) {
            System.err.println("SQLite connection failed: " + e.getMessage());
        }
    }

    private void initialiseSchema() {
        String[] tables = {
            """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT UNIQUE NOT NULL,
                password_hash TEXT NOT NULL,
                salt TEXT NOT NULL,
                role TEXT DEFAULT 'standard' CHECK(role IN ('standard', 'admin')),
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                sync_status TEXT DEFAULT 'synced' CHECK(sync_status IN ('synced', 'pending', 'conflict'))
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS files (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                filename TEXT NOT NULL,
                owner_id INTEGER NOT NULL,
                file_size INTEGER,
                total_chunks INTEGER,
                encryption_key_hash TEXT,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                sync_status TEXT DEFAULT 'synced',
                FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS file_permissions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                file_id INTEGER NOT NULL,
                user_id INTEGER NOT NULL,
                permission TEXT NOT NULL CHECK(permission IN ('read', 'write')),
                granted_by INTEGER NOT NULL,
                granted_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                sync_status TEXT DEFAULT 'synced',
                FOREIGN KEY (file_id) REFERENCES files(id) ON DELETE CASCADE,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                UNIQUE(file_id, user_id, permission)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS event_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER,
                action TEXT NOT NULL,
                details TEXT,
                timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                sync_status TEXT DEFAULT 'synced'
            )
            """
        };

        try (Statement stmt = connection.createStatement()) {
            for (String sql : tables) {
                stmt.execute(sql);
            }
            System.out.println("SQLite schema initialised.");
        } catch (SQLException e) {
            System.err.println("Schema init failed: " + e.getMessage());
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void close() {
        try {
            if (connection != null) connection.close();
        } catch (SQLException e) {
            System.err.println("Error closing SQLite: " + e.getMessage());
        }
    }
}
