package com.smartsociety.dao;

import com.smartsociety.config.DatabaseConnection;
import com.smartsociety.model.AccessRule;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for AccessRule operations. GRASP: Information Expert for rules.
 */
public class AccessRuleDAO {

    public List<AccessRule> getAllRules() {
        List<AccessRule> list = new ArrayList<>();
        String sql = "SELECT * FROM AccessRules ORDER BY category, rule_name";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { System.err.println("[AccessRuleDAO] " + e.getMessage()); }
        return list;
    }

    public List<AccessRule> getActiveRules() {
        List<AccessRule> list = new ArrayList<>();
        String sql = "SELECT * FROM AccessRules WHERE is_active = 1 ORDER BY category";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { System.err.println("[AccessRuleDAO] " + e.getMessage()); }
        return list;
    }

    public int saveRule(AccessRule rule) {
        String sql = "INSERT INTO AccessRules (rule_name, category, allowed_start_time, allowed_end_time, " +
                     "max_duration_minutes, max_visitors_per_day, is_active, created_by) VALUES (?,?,?,?,?,?,?,?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, rule.getRuleName()); stmt.setString(2, rule.getCategory());
            stmt.setTime(3, Time.valueOf(rule.getAllowedStartTime()));
            stmt.setTime(4, Time.valueOf(rule.getAllowedEndTime()));
            stmt.setInt(5, rule.getMaxDurationMinutes()); stmt.setInt(6, rule.getMaxVisitorsPerDay());
            stmt.setBoolean(7, rule.isActive()); stmt.setInt(8, rule.getCreatedBy());
            stmt.executeUpdate();
            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) { int id = keys.getInt(1); rule.setRuleId(id); return id; }
        } catch (SQLException e) { System.err.println("[AccessRuleDAO] Save error: " + e.getMessage()); }
        return -1;
    }

    public boolean updateRule(AccessRule rule) {
        String sql = "UPDATE AccessRules SET rule_name=?, category=?, allowed_start_time=?, allowed_end_time=?, " +
                     "max_duration_minutes=?, max_visitors_per_day=?, is_active=? WHERE rule_id=?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, rule.getRuleName()); stmt.setString(2, rule.getCategory());
            stmt.setTime(3, Time.valueOf(rule.getAllowedStartTime()));
            stmt.setTime(4, Time.valueOf(rule.getAllowedEndTime()));
            stmt.setInt(5, rule.getMaxDurationMinutes()); stmt.setInt(6, rule.getMaxVisitorsPerDay());
            stmt.setBoolean(7, rule.isActive()); stmt.setInt(8, rule.getRuleId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    public boolean deleteRule(int ruleId) {
        String sql = "DELETE FROM AccessRules WHERE rule_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, ruleId); return stmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    public boolean activateRule(int ruleId) {
        String sql = "UPDATE AccessRules SET is_active = 1 WHERE rule_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, ruleId); return stmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    public int getMaxDurationForCategory(String category) {
        String sql = "SELECT max_duration_minutes FROM AccessRules WHERE category = ? AND is_active = 1";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, category);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt("max_duration_minutes");
        } catch (SQLException e) { /* ignore */ }
        return 120; // default
    }

    public boolean validateRules(AccessRule rule) {
        return rule.getRuleName() != null && !rule.getRuleName().isEmpty()
               && rule.getCategory() != null && !rule.getCategory().isEmpty()
               && rule.getAllowedStartTime() != null && rule.getAllowedEndTime() != null
               && rule.getAllowedStartTime().isBefore(rule.getAllowedEndTime())
               && rule.getMaxDurationMinutes() > 0;
    }

    private AccessRule mapRow(ResultSet rs) throws SQLException {
        AccessRule r = new AccessRule();
        r.setRuleId(rs.getInt("rule_id")); r.setRuleName(rs.getString("rule_name"));
        r.setCategory(rs.getString("category"));
        r.setAllowedStartTime(rs.getTime("allowed_start_time").toLocalTime());
        r.setAllowedEndTime(rs.getTime("allowed_end_time").toLocalTime());
        r.setMaxDurationMinutes(rs.getInt("max_duration_minutes"));
        r.setMaxVisitorsPerDay(rs.getInt("max_visitors_per_day"));
        r.setActive(rs.getBoolean("is_active"));
        r.setCreatedBy(rs.getInt("created_by"));
        Timestamp ts = rs.getTimestamp("created_at"); if (ts != null) r.setCreatedAt(ts.toLocalDateTime());
        return r;
    }
}
