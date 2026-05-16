package com.smartsociety.service;

import com.smartsociety.config.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * GoF: Observer Pattern
 * NotificationService acts as a publisher that pushes notifications to users.
 * Notifications are persisted in the database and can be polled by UI.
 * Used across UC-04, UC-14 and cancellation flows.
 */
public class NotificationService {

    private static volatile NotificationService instance;

    private NotificationService() {
        System.out.println("[NotificationService] Instance created.");
    }

    public static NotificationService getInstance() {
        if (instance == null) {
            synchronized (NotificationService.class) {
                if (instance == null) {
                    instance = new NotificationService();
                }
            }
        }
        return instance;
    }

    /**
     * Sends a notification to a specific user.
     * As per SD-04: sendArrivalNotification(residentID, visitorData)
     * As per SD-14: notifyGuardsAndResident(violationID, action)
     */
    public boolean sendNotification(int recipientId, String message, String type) {
        String sql = "INSERT INTO Notifications (recipient_id, message, type) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, recipientId);
            stmt.setString(2, message);
            stmt.setString(3, type);
            stmt.executeUpdate();
            System.out.println("[NotificationService] Notification sent to user " + recipientId + ": " + message);
            return true;
        } catch (SQLException e) {
            System.err.println("[NotificationService] Failed to send notification: " + e.getMessage());
            return false;
        }
    }

    /**
     * Sends arrival notification to a resident when a visitor arrives.
     * As per SD-04: sendArrivalNotification(residentID, visitorData)
     */
    public boolean sendArrivalNotification(int residentId, String visitorName, String purpose) {
        String message = "Visitor '" + visitorName + "' has arrived. Purpose: " + purpose + ". Please approve or reject.";
        return sendNotification(residentId, message, "ARRIVAL");
    }

    /**
     * Notifies the guard about resident's decision.
     * As per SD-04: notifyGuard(guardID, 'Visitor approved by resident')
     */
    public boolean notifyGuard(int guardId, String message) {
        return sendNotification(guardId, message, "GUARD_ALERT");
    }

    /**
     * Notifies visitor about cancellation (simulated via resident notification).
     * As per SD-03: notifyVisitor(visitorContact, 'Approval cancelled')
     */
    public boolean notifyVisitorCancellation(int residentId, String visitorName) {
        String message = "Approval for visitor '" + visitorName + "' has been cancelled.";
        return sendNotification(residentId, message, "CANCELLATION");
    }

    /**
     * Notifies guards and residents about violations.
     * As per SD-14: notifyGuardsAndResident(violationID, action)
     */
    public boolean notifyAboutViolation(Integer residentId, String violationInfo, String action) {
        String message = "Violation Action: " + action + " - " + violationInfo;
        if (residentId != null && residentId > 0) {
            sendNotification(residentId, message, "VIOLATION");
        }
        // Notify all guards
        notifyAllGuards("Violation reported: " + violationInfo + " | Action: " + action);
        return true;
    }

    /**
     * Sends a notification to all guards.
     */
    public boolean notifyAllGuards(String message) {
        String sql = "INSERT INTO Notifications (recipient_id, message, type) " +
                     "SELECT user_id, ?, 'GUARD_ALERT' FROM Users WHERE role = 'GUARD'";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, message);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("[NotificationService] Failed to notify guards: " + e.getMessage());
            return false;
        }
    }

    /**
     * Retrieves unread notifications for a user.
     */
    public List<String[]> getUnreadNotifications(int userId) {
        List<String[]> notifications = new ArrayList<>();
        String sql = "SELECT notification_id, message, type, created_at FROM Notifications " +
                     "WHERE recipient_id = ? AND is_read = 0 ORDER BY created_at DESC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                notifications.add(new String[]{
                    String.valueOf(rs.getInt("notification_id")),
                    rs.getString("message"),
                    rs.getString("type"),
                    rs.getTimestamp("created_at").toString()
                });
            }
        } catch (SQLException e) {
            System.err.println("[NotificationService] Failed to get notifications: " + e.getMessage());
        }
        return notifications;
    }

    /**
     * Marks a notification as read.
     */
    public boolean markAsRead(int notificationId) {
        String sql = "UPDATE Notifications SET is_read = 1 WHERE notification_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, notificationId);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Marks all notifications as read for a user.
     */
    public boolean markAllAsRead(int userId) {
        String sql = "UPDATE Notifications SET is_read = 1 WHERE recipient_id = ? AND is_read = 0";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Gets the count of unread notifications for a user.
     */
    public int getUnreadCount(int userId) {
        String sql = "SELECT COUNT(*) FROM Notifications WHERE recipient_id = ? AND is_read = 0";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            // ignore
        }
        return 0;
    }
}
