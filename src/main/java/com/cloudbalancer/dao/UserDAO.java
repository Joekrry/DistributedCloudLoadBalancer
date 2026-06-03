package com.cloudbalancer.dao;

import com.cloudbalancer.database.LocalDatabase;
import com.cloudbalancer.database.RemoteDatabase;
import com.cloudbalancer.model.User;
import com.cloudbalancer.security.PasswordHasher;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {
    private final LocalDatabase localDb;
    private final RemoteDatabase remoteDb;

    public UserDAO(LocalDatabase localDb, RemoteDatabase remoteDb) {
        this.localDb = localDb;
        this.remoteDb = remoteDb;
        ensureDefaultAdmin();
    }

    private void ensureDefaultAdmin() {
        if (findByUsername("admin") == null) {
            String salt = PasswordHasher.generateSalt();
            String hash = PasswordHasher.hashPassword("admin", salt);
            executeOnBoth("INSERT INTO users (username, password_hash, salt, role) VALUES (?, ?, ?, 'admin')",
                    "admin", hash, salt);
        }
    }

    public User createUser(String username, String password) {
        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hashPassword(password, salt);
        executeOnBoth("INSERT INTO users (username, password_hash, salt, role) VALUES (?, ?, ?, 'standard')",
                username, hash, salt);
        return findByUsername(username);
    }

    public User authenticate(String username, String password) {
        User user = findByUsername(username);
        if (user != null && PasswordHasher.verifyPassword(password, user.getPasswordHash(), user.getSalt())) {
            return user;
        }
        return null;
    }

    public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (PreparedStatement pstmt = localDb.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.err.println("Error finding user: " + e.getMessage());
        }
        return null;
    }

    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";
        try (Statement stmt = localDb.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) users.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("Error listing users: " + e.getMessage());
        }
        return users;
    }

    public void updatePassword(int userId, String newPassword) {
        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hashPassword(newPassword, salt);
        executeOnBoth("UPDATE users SET password_hash = ?, salt = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                hash, salt, userId);
    }

    public void deleteUser(int userId) {
        executeOnBoth("DELETE FROM users WHERE id = ?", userId);
    }

    public void promoteToAdmin(int userId) {
        executeOnBoth("UPDATE users SET role = 'admin', updated_at = CURRENT_TIMESTAMP WHERE id = ?", userId);
    }

    public void demoteToStandard(int userId) {
        executeOnBoth("UPDATE users SET role = 'standard', updated_at = CURRENT_TIMESTAMP WHERE id = ?", userId);
    }

    private void executeOnBoth(String sql, Object... params) {
        executeWithParams(localDb.getConnection(), sql, params);
        if (remoteDb.isConnected()) {
            executeWithParams(remoteDb.getConnection(), sql, params);
        }
    }

    private void executeWithParams(Connection conn, String sql, Object... params) {
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) pstmt.setObject(i + 1, params[i]);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("SQL error: " + e.getMessage());
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        return new User(
            rs.getInt("id"),
            rs.getString("username"),
            rs.getString("password_hash"),
            rs.getString("salt"),
            rs.getString("role"),
            rs.getString("created_at")
        );
    }
}
