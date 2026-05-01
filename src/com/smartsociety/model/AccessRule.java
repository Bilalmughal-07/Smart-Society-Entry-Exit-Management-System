package com.smartsociety.model;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Domain model representing a Visitor Access Rule defined by the admin.
 * Maps to the AccessRules table. Used in UC-12: Define Visitor Access Rules.
 */
public class AccessRule {

    private int ruleId;
    private String ruleName;
    private String category;
    private LocalTime allowedStartTime;
    private LocalTime allowedEndTime;
    private int maxDurationMinutes;
    private int maxVisitorsPerDay;
    private boolean isActive;
    private int createdBy;
    private LocalDateTime createdAt;

    // Default constructor
    public AccessRule() {
        this.maxDurationMinutes = 120;
        this.maxVisitorsPerDay = 10;
        this.isActive = true;
    }

    // Getters and Setters
    public int getRuleId() { return ruleId; }
    public void setRuleId(int ruleId) { this.ruleId = ruleId; }

    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public LocalTime getAllowedStartTime() { return allowedStartTime; }
    public void setAllowedStartTime(LocalTime allowedStartTime) { this.allowedStartTime = allowedStartTime; }

    public LocalTime getAllowedEndTime() { return allowedEndTime; }
    public void setAllowedEndTime(LocalTime allowedEndTime) { this.allowedEndTime = allowedEndTime; }

    public int getMaxDurationMinutes() { return maxDurationMinutes; }
    public void setMaxDurationMinutes(int maxDurationMinutes) { this.maxDurationMinutes = maxDurationMinutes; }

    public int getMaxVisitorsPerDay() { return maxVisitorsPerDay; }
    public void setMaxVisitorsPerDay(int maxVisitorsPerDay) { this.maxVisitorsPerDay = maxVisitorsPerDay; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public int getCreatedBy() { return createdBy; }
    public void setCreatedBy(int createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getTimeWindow() {
        return allowedStartTime + " - " + allowedEndTime;
    }

    public String getActiveStatus() {
        return isActive ? "Active" : "Inactive";
    }
}
