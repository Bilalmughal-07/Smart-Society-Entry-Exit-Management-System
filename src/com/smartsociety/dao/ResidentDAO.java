package com.smartsociety.dao;

import com.smartsociety.config.DatabaseConnection;
import java.sql.*;

/**
 * DAO for ArrivalRequest operations. Used in UC-04 approve visitor on arrival.
 */
public class ResidentDAO {

    public static class ArrivalRequest {
        private final int requestId;
        private final int guardId;
        private final int residentId;
        private final String visitorName;
        private final String visitorContact;
        private final String purpose;
        private final String status;

        public ArrivalRequest(int requestId, int guardId, int residentId, String visitorName, String visitorContact,
                              String purpose, String status) {
            this.requestId = requestId;
            this.guardId = guardId;
            this.residentId = residentId;
            this.visitorName = visitorName;
            this.visitorContact = visitorContact;
            this.purpose = purpose;
            this.status = status;
        }

        public int getRequestId() { return requestId; }
        public int getGuardId() { return guardId; }
        public int getResidentId() { return residentId; }
        public String getVisitorName() { return visitorName; }
        public String getVisitorContact() { return visitorContact; }
        public String getPurpose() { return purpose; }
        public String getStatus() { return status; }
    }

    public int storeArrivalRequest(int guardId, int residentId, String visitorName, String visitorContact, String purpose) {
        ensureVisitorContactColumn();
        String sql = "INSERT INTO ArrivalRequests (guard_id, resident_id, visitor_name, visitor_contact, visitor_purpose, status) " +
                     "VALUES (?, ?, ?, ?, ?, 'PENDING')";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, guardId); stmt.setInt(2, residentId);
            stmt.setString(3, visitorName); stmt.setString(4, visitorContact); stmt.setString(5, purpose);
            stmt.executeUpdate();
            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);
        } catch (SQLException e) { System.err.println("[ResidentDAO] " + e.getMessage()); }
        return -1;
    }

    public ArrivalRequest getArrivalRequestById(int requestId) {
        ensureVisitorContactColumn();
        String sql = "SELECT request_id, guard_id, resident_id, visitor_name, visitor_contact, visitor_purpose, status " +
                     "FROM ArrivalRequests WHERE request_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, requestId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new ArrivalRequest(
                    rs.getInt("request_id"),
                    rs.getInt("guard_id"),
                    rs.getInt("resident_id"),
                    rs.getString("visitor_name"),
                    rs.getString("visitor_contact"),
                    rs.getString("visitor_purpose"),
                    rs.getString("status")
                );
            }
        } catch (SQLException e) { System.err.println("[ResidentDAO] " + e.getMessage()); }
        return null;
    }

    public boolean updateArrivalRequestStatus(int requestId, String status) {
        String sql = "UPDATE ArrivalRequests SET status = ?, responded_at = GETDATE() WHERE request_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status); stmt.setInt(2, requestId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    public String[][] getPendingRequests(int residentId) {
        ensureVisitorContactColumn();
        String sql = "SELECT ar.*, g.full_name as guard_name FROM ArrivalRequests ar " +
                     "JOIN Users g ON ar.guard_id = g.user_id " +
                     "WHERE ar.resident_id = ? AND ar.status = 'PENDING' ORDER BY ar.created_at DESC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, residentId);
            ResultSet rs = stmt.executeQuery();
            java.util.List<String[]> list = new java.util.ArrayList<>();
            while (rs.next()) {
                list.add(new String[]{
                    String.valueOf(rs.getInt("request_id")),
                    rs.getString("visitor_name"),
                    rs.getString("visitor_contact"),
                    rs.getString("visitor_purpose"),
                    rs.getString("guard_name"),
                    rs.getTimestamp("created_at").toString()
                });
            }
            return list.toArray(new String[0][]);
        } catch (SQLException e) { System.err.println("[ResidentDAO] " + e.getMessage()); }
        return new String[0][];
    }

    private void ensureVisitorContactColumn() {
        String sql = "IF COL_LENGTH('ArrivalRequests', 'visitor_contact') IS NULL " +
                     "ALTER TABLE ArrivalRequests ADD visitor_contact VARCHAR(50) NULL";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("[ResidentDAO] Could not verify visitor_contact column: " + e.getMessage());
        }
    }
}
