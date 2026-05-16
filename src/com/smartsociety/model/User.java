package com.smartsociety.model;

import java.time.LocalDateTime;

/**
 * Domain model representing a system user (Admin, Guard, or Resident).
 * Maps to the Users table in the database.
 */
public class User {

    public enum Role { ADMIN, GUARD, RESIDENT }
    public enum Status { INSIDE, OUTSIDE }

    private int userId;
    private String username;
    private String passwordHash;
    private Role role;
    private String fullName;
    private String unitNumber;
    private String contact;
    private String qrCode;
    private Status status;
    private LocalDateTime createdAt;

    // Default constructor
    public User() {
        this.status = Status.OUTSIDE;
    }

    // Parameterized constructor
    public User(int userId, String username, String passwordHash, Role role,
                String fullName, String unitNumber, String contact, String qrCode, Status status) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.fullName = fullName;
        this.unitNumber = unitNumber;
        this.contact = contact;
        this.qrCode = qrCode;
        this.status = status;
    }

    // Getters and Setters
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getUnitNumber() { return unitNumber; }
    public void setUnitNumber(String unitNumber) { this.unitNumber = unitNumber; }

    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }

    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return fullName + " (" + role + " - " + unitNumber + ")";
    }
}
