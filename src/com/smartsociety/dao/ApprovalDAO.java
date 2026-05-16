package com.smartsociety.dao;

import com.smartsociety.config.DatabaseConnection;
import com.smartsociety.model.Approval;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for Approval operations. GRASP: Information Expert.
 */
public class ApprovalDAO {

    public int saveApproval(Approval approval) {
        String sql = "INSERT INTO Approvals (resident_id, visitor_name, visitor_contact, category, purpose, " +
                     "visit_date, time_window_start, time_window_end, duration_minutes, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING')";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, approval.getResidentId());
            stmt.setString(2, approval.getVisitorName());
            stmt.setString(3, approval.getVisitorContact());
            stmt.setString(4, approval.getCategory());
            stmt.setString(5, approval.getPurpose());
            stmt.setDate(6, Date.valueOf(approval.getVisitDate()));
            stmt.setTime(7, Time.valueOf(approval.getTimeWindowStart()));
            stmt.setTime(8, Time.valueOf(approval.getTimeWindowEnd()));
            stmt.setInt(9, approval.getDurationMinutes());
            stmt.executeUpdate();
            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) { int id = keys.getInt(1); approval.setApprovalId(id); return id; }
        } catch (SQLException e) { System.err.println("[ApprovalDAO] Save error: " + e.getMessage()); }
        return -1;
    }

    public boolean linkQRToApproval(int approvalId, String qrCode) {
        String sql = "UPDATE Approvals SET qr_code = ?, status = 'APPROVED' WHERE approval_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, qrCode); stmt.setInt(2, approvalId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    public boolean validateVisitorData(String name, String category, LocalDate date) {
        return name != null && !name.trim().isEmpty() && category != null && !category.trim().isEmpty()
               && date != null && !date.isBefore(LocalDate.now());
    }

    public String checkVisitorAccessRules(String category, LocalDate visitDate, LocalTime start, LocalTime end) {
        String sql = "SELECT * FROM AccessRules WHERE category = ? AND is_active = 1";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, category);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                LocalTime ruleStart = rs.getTime("allowed_start_time").toLocalTime();
                LocalTime ruleEnd = rs.getTime("allowed_end_time").toLocalTime();
                if (start.isBefore(ruleStart) || end.isAfter(ruleEnd)) {
                    return "Violates " + category + " rule: allowed between " + ruleStart + " and " + ruleEnd;
                }
                return null; // Valid
            }
            return null; // No rule exists for this category
        } catch (SQLException e) { return "Database error checking rules."; }
    }

    public boolean isVisitorBlacklisted(String name, String contact) {
        String sql = "SELECT COUNT(*) FROM Violations v " +
                     "JOIN EntryLogs el ON v.log_id = el.log_id " +
                     "LEFT JOIN Approvals a ON el.approval_id = a.approval_id " +
                     "WHERE a.visitor_name = ? AND a.visitor_contact = ? AND v.action_taken = 'BLACKLIST'";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, contact);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) { System.err.println("[ApprovalDAO] Blacklist check error: " + e.getMessage()); }
        return false;
    }

    public List<Approval> getApprovalsByResident(int residentId) {
        List<Approval> list = new ArrayList<>();
        String sql = "SELECT a.*, u.full_name as resident_name, u.unit_number FROM Approvals a " +
                     "JOIN Users u ON a.resident_id = u.user_id WHERE a.resident_id = ? ORDER BY a.created_at DESC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, residentId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { System.err.println("[ApprovalDAO] " + e.getMessage()); }
        return list;
    }

    public List<Approval> getActiveApprovalsByResident(int residentId) {
        List<Approval> list = new ArrayList<>();
        String sql = "SELECT a.*, u.full_name as resident_name, u.unit_number FROM Approvals a " +
                     "JOIN Users u ON a.resident_id = u.user_id WHERE a.resident_id = ? " +
                     "AND a.status NOT IN ('CANCELLED','COMPLETED') ORDER BY a.visit_date";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, residentId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { System.err.println("[ApprovalDAO] " + e.getMessage()); }
        return list;
    }

    public Approval getApprovalById(int approvalId) {
        String sql = "SELECT a.*, u.full_name as resident_name, u.unit_number FROM Approvals a " +
                     "JOIN Users u ON a.resident_id = u.user_id WHERE a.approval_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, approvalId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { System.err.println("[ApprovalDAO] " + e.getMessage()); }
        return null;
    }

    public Approval getApprovalByQRCode(String qrCode) {
        String sql = "SELECT a.*, u.full_name as resident_name, u.unit_number FROM Approvals a " +
                     "JOIN Users u ON a.resident_id = u.user_id WHERE a.qr_code = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, qrCode);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { System.err.println("[ApprovalDAO] " + e.getMessage()); }
        return null;
    }

    public boolean updateApprovalStatus(int approvalId, String status) {
        String sql = "UPDATE Approvals SET status = ? WHERE approval_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status); stmt.setInt(2, approvalId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    public boolean updateApproval(Approval a) {
        String sql = "UPDATE Approvals SET visitor_name=?, visitor_contact=?, category=?, purpose=?, " +
                     "visit_date=?, time_window_start=?, time_window_end=?, duration_minutes=? WHERE approval_id=?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, a.getVisitorName()); stmt.setString(2, a.getVisitorContact());
            stmt.setString(3, a.getCategory()); stmt.setString(4, a.getPurpose());
            stmt.setDate(5, Date.valueOf(a.getVisitDate()));
            stmt.setTime(6, Time.valueOf(a.getTimeWindowStart()));
            stmt.setTime(7, Time.valueOf(a.getTimeWindowEnd()));
            stmt.setInt(8, a.getDurationMinutes()); stmt.setInt(9, a.getApprovalId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    public boolean validateTimeWindow(Approval approval) {
        LocalTime now = LocalTime.now();
        return approval.getVisitDate().equals(LocalDate.now())
               && !now.isBefore(approval.getTimeWindowStart()) && !now.isAfter(approval.getTimeWindowEnd());
    }

    public boolean invalidateQRCode(int approvalId) {
        String sql = "UPDATE Approvals SET qr_code = NULL WHERE approval_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, approvalId); return stmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    /**
     * Checks the entry status of an approval.
     * As per SD-03: checkEntryStatus(approvalID) → return status
     */
    public String checkEntryStatus(int approvalId) {
        Approval approval = getApprovalById(approvalId);
        if (approval != null) {
            return approval.getStatusString();
        }
        return "NOT_FOUND";
    }

    public List<Approval> getTodayApprovedApprovals() {
        List<Approval> list = new ArrayList<>();
        String sql = "SELECT a.*, u.full_name as resident_name, u.unit_number FROM Approvals a " +
                     "JOIN Users u ON a.resident_id = u.user_id WHERE a.visit_date = CAST(GETDATE() AS DATE) " +
                     "AND a.status IN ('APPROVED','PENDING') ORDER BY a.time_window_start";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { System.err.println("[ApprovalDAO] " + e.getMessage()); }
        return list;
    }

    private Approval mapRow(ResultSet rs) throws SQLException {
        Approval a = new Approval();
        a.setApprovalId(rs.getInt("approval_id")); a.setResidentId(rs.getInt("resident_id"));
        a.setVisitorName(rs.getString("visitor_name")); a.setVisitorContact(rs.getString("visitor_contact"));
        a.setCategory(rs.getString("category")); a.setPurpose(rs.getString("purpose"));
        a.setVisitDate(rs.getDate("visit_date").toLocalDate());
        a.setTimeWindowStart(rs.getTime("time_window_start").toLocalTime());
        a.setTimeWindowEnd(rs.getTime("time_window_end").toLocalTime());
        a.setDurationMinutes(rs.getInt("duration_minutes")); a.setQrCode(rs.getString("qr_code"));
        a.setStatus(Approval.Status.valueOf(rs.getString("status")));
        Timestamp ts = rs.getTimestamp("created_at"); if (ts != null) a.setCreatedAt(ts.toLocalDateTime());
        try { a.setResidentName(rs.getString("resident_name")); a.setResidentUnit(rs.getString("unit_number")); }
        catch (SQLException ignored) {}
        return a;
    }
}
