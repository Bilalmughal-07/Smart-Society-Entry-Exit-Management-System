package com.smartsociety.dao;

import com.smartsociety.config.DatabaseConnection;
import com.smartsociety.model.EntryLog;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for EntryLog operations. GRASP: Information Expert for entry/exit records.
 */
public class EntryLogDAO {

    public int createEntryLog(EntryLog log) {
        String sql = "INSERT INTO EntryLogs (person_type, person_id, approval_id, entry_timestamp, category, guard_id, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE')";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, log.getPersonType().name());
            stmt.setInt(2, log.getPersonId());
            if (log.getApprovalId() != null) stmt.setInt(3, log.getApprovalId()); else stmt.setNull(3, Types.INTEGER);
            stmt.setTimestamp(4, Timestamp.valueOf(log.getEntryTimestamp()));
            stmt.setString(5, log.getCategory());
            if (log.getGuardId() != null) stmt.setInt(6, log.getGuardId()); else stmt.setNull(6, Types.INTEGER);
            stmt.executeUpdate();
            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) { int id = keys.getInt(1); log.setLogId(id); return id; }
        } catch (SQLException e) { System.err.println("[EntryLogDAO] Create error: " + e.getMessage()); }
        return -1;
    }

    public EntryLog getActiveEntryByPersonId(int personId, String personType) {
        String sql = "SELECT * FROM EntryLogs WHERE person_id = ? AND person_type = ? AND status = 'ACTIVE' " +
                     "ORDER BY entry_timestamp DESC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, personId); stmt.setString(2, personType);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { System.err.println("[EntryLogDAO] " + e.getMessage()); }
        return null;
    }

    public boolean setExitTimestamp(int logId, LocalDateTime exitTime) {
        String sql = "UPDATE EntryLogs SET exit_timestamp = ?, status = 'EXITED' WHERE log_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(exitTime)); stmt.setInt(2, logId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    public List<EntryLog> getActiveEntries() {
        List<EntryLog> list = new ArrayList<>();
        String sql = "SELECT el.*, CASE WHEN el.person_type = 'RESIDENT' THEN u.full_name " +
                     "ELSE a.visitor_name END as person_name, " +
                     "CASE WHEN el.person_type = 'RESIDENT' THEN u.unit_number ELSE ur.unit_number END as resident_unit, " +
                     "g.full_name as guard_name " +
                     "FROM EntryLogs el " +
                     "LEFT JOIN Users u ON el.person_type = 'RESIDENT' AND el.person_id = u.user_id " +
                     "LEFT JOIN Approvals a ON el.person_type = 'VISITOR' AND el.approval_id = a.approval_id " +
                     "LEFT JOIN Users ur ON a.resident_id = ur.user_id " +
                     "LEFT JOIN Users g ON el.guard_id = g.user_id " +
                     "WHERE el.status = 'ACTIVE' ORDER BY el.entry_timestamp DESC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) { EntryLog log = mapRow(rs);
                try { log.setPersonName(rs.getString("person_name")); } catch (SQLException ignored) {}
                try { log.setResidentUnit(rs.getString("resident_unit")); } catch (SQLException ignored) {}
                try { log.setGuardName(rs.getString("guard_name")); } catch (SQLException ignored) {}
                list.add(log);
            }
        } catch (SQLException e) { System.err.println("[EntryLogDAO] " + e.getMessage()); }
        return list;
    }

    public List<EntryLog> detectOverstays(int maxDurationMinutes) {
        List<EntryLog> list = new ArrayList<>();
        String sql = "SELECT el.*, CASE WHEN el.person_type = 'RESIDENT' THEN u.full_name " +
                     "ELSE a.visitor_name END as person_name " +
                     "FROM EntryLogs el " +
                     "LEFT JOIN Users u ON el.person_type = 'RESIDENT' AND el.person_id = u.user_id " +
                     "LEFT JOIN Approvals a ON el.person_type = 'VISITOR' AND el.approval_id = a.approval_id " +
                     "WHERE el.status = 'ACTIVE' AND DATEDIFF(MINUTE, el.entry_timestamp, GETDATE()) > ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, maxDurationMinutes);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) { EntryLog log = mapRow(rs);
                try { log.setPersonName(rs.getString("person_name")); } catch (SQLException ignored) {}
                list.add(log);
            }
        } catch (SQLException e) { System.err.println("[EntryLogDAO] " + e.getMessage()); }
        return list;
    }

    public int getActiveOccupancyCount() {
        String sql = "SELECT COUNT(*) FROM EntryLogs WHERE status = 'ACTIVE'";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { /* ignore */ }
        return 0;
    }

    private EntryLog mapRow(ResultSet rs) throws SQLException {
        EntryLog log = new EntryLog();
        log.setLogId(rs.getInt("log_id"));
        log.setPersonType(EntryLog.PersonType.valueOf(rs.getString("person_type")));
        log.setPersonId(rs.getInt("person_id"));
        int aid = rs.getInt("approval_id"); if (!rs.wasNull()) log.setApprovalId(aid);
        log.setEntryTimestamp(rs.getTimestamp("entry_timestamp").toLocalDateTime());
        Timestamp exit = rs.getTimestamp("exit_timestamp");
        if (exit != null) log.setExitTimestamp(exit.toLocalDateTime());
        log.setCategory(rs.getString("category"));
        int gid = rs.getInt("guard_id"); if (!rs.wasNull()) log.setGuardId(gid);
        log.setStatus(EntryLog.LogStatus.valueOf(rs.getString("status")));
        return log;
    }
}
