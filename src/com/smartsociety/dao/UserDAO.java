package com.smartsociety.dao;

import com.smartsociety.config.DatabaseConnection;
import com.smartsociety.model.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for User operations.
 * GRASP: Information Expert for user data persistence.
 */
public class UserDAO {

    /**
     * Authenticates a user by username and password.
     * Used in UC-00: System Login.
     */
    public User authenticate(String username, String password) {
        String sql = "SELECT * FROM Users WHERE username = ? AND password_hash = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Authentication error: " + e.getMessage());
        }
        return null;
    }

    /**
     * Retrieves a user by their ID.
     */
    public User getUserById(int userId) {
        String sql = "SELECT * FROM Users WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error getting user: " + e.getMessage());
        }
        return null;
    }

    /**
     * Retrieves all users with a specific role.
     */
    public List<User> getUsersByRole(User.Role role) {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM Users WHERE role = ? ORDER BY full_name";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, role.name());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error getting users by role: " + e.getMessage());
        }
        return users;
    }

    /**
     * Updates the status of a user (INSIDE/OUTSIDE).
     * Used in UC-10/UC-11 for resident entry/exit.
     */
    public boolean updateUserStatus(int userId, User.Status newStatus) {
        String sql = "UPDATE Users SET status = ? WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newStatus.name());
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error updating status: " + e.getMessage());
            return false;
        }
    }

    /**
     * Finds a user by their QR code string.
     * Used for resident QR scanning at gate.
     */
    public User getUserByQRCode(String qrCode) {
        String sql = "SELECT * FROM Users WHERE qr_code = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, qrCode);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error finding user by QR: " + e.getMessage());
        }
        return null;
    }

    /**
     * Gets all residents for display in dropdowns.
     */
    public List<User> getAllResidents() {
        return getUsersByRole(User.Role.RESIDENT);
    }

    /**
     * Updates the QR code for a user.
     */
    public boolean updateQRCode(int userId, String qrCode) {
        String sql = "UPDATE Users SET qr_code = ? WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, qrCode);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error updating QR code: " + e.getMessage());
            return false;
        }
    }

    /**
     * Maps a ResultSet row to a User object.
     */
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("user_id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setRole(User.Role.valueOf(rs.getString("role")));
        user.setFullName(rs.getString("full_name"));
        user.setUnitNumber(rs.getString("unit_number"));
        user.setContact(rs.getString("contact"));
        user.setQrCode(rs.getString("qr_code"));
        String statusStr = rs.getString("status");
        if (statusStr != null) {
            user.setStatus(User.Status.valueOf(statusStr));
        }
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            user.setCreatedAt(ts.toLocalDateTime());
        }
        return user;
    }
}
