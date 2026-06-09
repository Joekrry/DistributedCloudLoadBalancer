package com.cloudbalancer.service;

import com.cloudbalancer.database.LocalDatabase;
import com.cloudbalancer.database.RemoteDatabase;
import javafx.application.Platform;
import javafx.scene.control.TextArea;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class EventLogger {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final LocalDatabase localDb;
    private final RemoteDatabase remoteDb;
    private TextArea logPane;

    public EventLogger(LocalDatabase localDb, RemoteDatabase remoteDb) {
        this.localDb = localDb;
        this.remoteDb = remoteDb;
    }

    public void setLogPane(TextArea logPane) {
        this.logPane = logPane;
    }

    public void log(int userId, String action, String details) {
        String timestamp = LocalDateTime.now().format(FMT);
        String message = "[" + timestamp + "] User " + userId + ": " + action + " — " + details;

        String sql = "INSERT INTO event_logs (user_id, action, details) VALUES (?, ?, ?)";
        writeToDb(localDb.getConnection(), sql, userId, action, details);
        if (remoteDb.isConnected()) writeToDb(remoteDb.getConnection(), sql, userId, action, details);

        if (logPane != null) {
            Platform.runLater(() -> logPane.appendText(message + "\n"));
        }
        System.out.println(message);
    }

    public void logError(String action, Exception e) {
        log(0, "ERROR:" + action, e.getMessage());
    }

    private void writeToDb(Connection conn, String sql, int userId, String action, String details) {
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, action);
            pstmt.setString(3, details);
            pstmt.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Logging failed: " + ex.getMessage());
        }
    }
}
