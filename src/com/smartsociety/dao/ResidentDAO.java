package com.smartsociety.dao;

import com.smartsociety.config.DatabaseConnection;
import java.sql.*;

/**
 * DAO for ArrivalRequest operations. Used in UC-04 approve visitor on arrival.
 */
public class ResidentDAO {

    public int storeArrivalRequest(int guardId, int residentId, String visitorName, String purpose) {
        String sql = "INSERT INTO ArrivalRequests (guard_id, resident_id, visitor_name, visitor_purpose, status) " +
                     "VALUES (?, ?, ?, ?, 'PENDING')";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, guardId); stmt.setInt(2, residentId);
            stmt.setString(3, visitorName); stmt.setString(4, purpose);
            stmt.executeUpdate();
            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);
        } catch (SQLException e) { System.err.println("[ResidentDAO] " + e.getMessage()); }
        return -1;
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
                    rs.getString("visitor_purpose"),
                    rs.getString("guard_name"),
                    rs.getTimestamp("created_at").toString()
                });
            }
            return list.toArray(new String[0][]);
        } catch (SQLException e) { System.err.println("[ResidentDAO] " + e.getMessage()); }
        return new String[0][];
    }
}
