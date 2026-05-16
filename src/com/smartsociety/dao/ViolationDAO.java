package com.smartsociety.dao;

import com.smartsociety.config.DatabaseConnection;
import com.smartsociety.model.Violation;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for Violation operations. GRASP: Information Expert for violations.
 */
public class ViolationDAO {

    public int createViolation(Violation v) {
        String sql = "INSERT INTO Violations (log_id, violation_type, description, status) VALUES (?,?,?,?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, v.getLogId()); stmt.setString(2, v.getViolationType().name());
            stmt.setString(3, v.getDescription()); stmt.setString(4, "PENDING");
            stmt.executeUpdate();
            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) { int id = keys.getInt(1); v.setViolationId(id); return id; }
        } catch (SQLException e) { System.err.println("[ViolationDAO] " + e.getMessage()); }
        return -1;
    }

    public List<Violation> getPendingViolations() {
        List<Violation> list = new ArrayList<>();
        String sql = "SELECT v.*, CASE WHEN el.person_type = 'VISITOR' THEN a.visitor_name ELSE u.full_name END as person_name, " +
                     "el.category, adm.full_name as admin_name, a.resident_id, res.full_name as resident_name FROM Violations v " +
                     "JOIN EntryLogs el ON v.log_id = el.log_id " +
                     "LEFT JOIN Approvals a ON el.approval_id = a.approval_id " +
                     "LEFT JOIN Users u ON el.person_type = 'RESIDENT' AND el.person_id = u.user_id " +
                     "LEFT JOIN Users adm ON v.action_by = adm.user_id " +
                     "LEFT JOIN Users res ON a.resident_id = res.user_id " +
                     "WHERE v.status = 'PENDING' ORDER BY v.detected_at DESC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapRowFull(rs));
        } catch (SQLException e) { System.err.println("[ViolationDAO] " + e.getMessage()); }
        return list;
    }

    public List<Violation> getAllViolations() {
        List<Violation> list = new ArrayList<>();
        String sql = "SELECT v.*, CASE WHEN el.person_type = 'VISITOR' THEN a.visitor_name ELSE u.full_name END as person_name, " +
                     "el.category, adm.full_name as admin_name, a.resident_id, res.full_name as resident_name FROM Violations v " +
                     "JOIN EntryLogs el ON v.log_id = el.log_id " +
                     "LEFT JOIN Approvals a ON el.approval_id = a.approval_id " +
                     "LEFT JOIN Users u ON el.person_type = 'RESIDENT' AND el.person_id = u.user_id " +
                     "LEFT JOIN Users adm ON v.action_by = adm.user_id " +
                     "LEFT JOIN Users res ON a.resident_id = res.user_id ORDER BY v.detected_at DESC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapRowFull(rs));
        } catch (SQLException e) { System.err.println("[ViolationDAO] " + e.getMessage()); }
        return list;
    }

    public Violation getViolationById(int violationId) {
        String sql = "SELECT v.*, CASE WHEN el.person_type = 'VISITOR' THEN a.visitor_name ELSE u.full_name END as person_name, " +
                     "el.category, adm.full_name as admin_name, a.resident_id, res.full_name as resident_name FROM Violations v " +
                     "JOIN EntryLogs el ON v.log_id = el.log_id " +
                     "LEFT JOIN Approvals a ON el.approval_id = a.approval_id " +
                     "LEFT JOIN Users u ON el.person_type = 'RESIDENT' AND el.person_id = u.user_id " +
                     "LEFT JOIN Users adm ON v.action_by = adm.user_id " +
                     "LEFT JOIN Users res ON a.resident_id = res.user_id WHERE v.violation_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, violationId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return mapRowFull(rs);
        } catch (SQLException e) { System.err.println("[ViolationDAO] " + e.getMessage()); }
        return null;
    }

    public boolean persistViolationAction(int violationId, String action, int adminId) {
        String sql = "UPDATE Violations SET action_taken=?, action_by=?, status=?, resolved_at=GETDATE() WHERE violation_id=?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, action); stmt.setInt(2, adminId);
            stmt.setString(3, action.equals("WARNING") ? "WARNING" : "RESOLVED");
            stmt.setInt(4, violationId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    public boolean existsForLog(int logId) {
        String sql = "SELECT COUNT(*) FROM Violations WHERE log_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, logId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) { /* ignore */ }
        return false;
    }

    private Violation mapRowFull(ResultSet rs) throws SQLException {
        Violation v = new Violation();
        v.setViolationId(rs.getInt("violation_id")); v.setLogId(rs.getInt("log_id"));
        v.setViolationType(Violation.ViolationType.valueOf(rs.getString("violation_type")));
        v.setDescription(rs.getString("description"));
        String act = rs.getString("action_taken");
        if (act != null) v.setActionTaken(Violation.Action.valueOf(act));
        int ab = rs.getInt("action_by"); if (!rs.wasNull()) v.setActionBy(ab);
        v.setStatus(Violation.ViolationStatus.valueOf(rs.getString("status")));
        v.setDetectedAt(rs.getTimestamp("detected_at").toLocalDateTime());
        Timestamp ra = rs.getTimestamp("resolved_at"); if (ra != null) v.setResolvedAt(ra.toLocalDateTime());
        try { v.setPersonName(rs.getString("person_name")); } catch (SQLException ignored) {}
        try { v.setCategory(rs.getString("category")); } catch (SQLException ignored) {}
        try { v.setAdminName(rs.getString("admin_name")); } catch (SQLException ignored) {}
        try { 
            int rId = rs.getInt("resident_id"); 
            if (!rs.wasNull()) v.setResidentId(rId); 
        } catch (SQLException ignored) {}
        try { v.setResidentName(rs.getString("resident_name")); } catch (SQLException ignored) {}
        return v;
    }
}
