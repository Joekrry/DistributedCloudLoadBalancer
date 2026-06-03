package com.cloudbalancer.model;

public class User {
    private int id;
    private String username;
    private String passwordHash;
    private String salt;
    private String role;
    private String createdAt;

    public User(int id, String username, String passwordHash, String salt, String role, String createdAt) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.role = role;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getSalt() { return salt; }
    public String getRole() { return role; }
    public boolean isAdmin() { return "admin".equals(role); }
    public String getCreatedAt() { return createdAt; }
    public void setRole(String role) { this.role = role; }
}
