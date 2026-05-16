package com.smartsociety.model;

import java.time.LocalDateTime;

/**
 * Domain model representing a Violation record.
 * GRASP: Information Expert – knows its own type and manages its own action state transitions.
 * As per SD-14: applyAction(action)
 * Maps to the Violations table. Used in UC-14.
 */
public class Violation {

    public enum ViolationType { OVERSTAY, UNAUTHORIZED, BLACKLISTED }
    public enum Action { WARNING, FINE, BLACKLIST }
    public enum ViolationStatus { PENDING, WARNING, RESOLVED }

    private int violationId;
    private int logId;
    private ViolationType violationType;
    private String description;
    private Action actionTaken;
    private Integer actionBy;
    private ViolationStatus status;
    private LocalDateTime detectedAt;
    private LocalDateTime resolvedAt;

    // Display fields (from JOINs)
    private String personName;
    private String adminName;
    private String category;
    private long overstayMinutes;
    private Integer residentId;
    private String residentName;

    // Default constructor
    public Violation() {
        this.status = ViolationStatus.PENDING;
        this.detectedAt = LocalDateTime.now();
    }

    /**
     * GRASP: Information Expert
     * Violation manages its own action state transitions.
     * As per SD-14: applyAction(action) → return actionApplied = true
     */
    public boolean applyAction(Action action) {
        this.actionTaken = action;
        if (action == Action.WARNING) {
            this.status = ViolationStatus.WARNING;
        } else {
            this.status = ViolationStatus.RESOLVED;
        }
        this.resolvedAt = LocalDateTime.now();
        return true;
    }

    // Getters and Setters
    public int getViolationId() { return violationId; }
    public void setViolationId(int violationId) { this.violationId = violationId; }

    public int getLogId() { return logId; }
    public void setLogId(int logId) { this.logId = logId; }

    public ViolationType getViolationType() { return violationType; }
    public void setViolationType(ViolationType violationType) { this.violationType = violationType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Action getActionTaken() { return actionTaken; }
    public void setActionTaken(Action actionTaken) { this.actionTaken = actionTaken; }

    public Integer getActionBy() { return actionBy; }
    public void setActionBy(Integer actionBy) { this.actionBy = actionBy; }

    public ViolationStatus getStatus() { return status; }
    public void setStatus(ViolationStatus status) { this.status = status; }

    public LocalDateTime getDetectedAt() { return detectedAt; }
    public void setDetectedAt(LocalDateTime detectedAt) { this.detectedAt = detectedAt; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    public String getPersonName() { return personName; }
    public void setPersonName(String personName) { this.personName = personName; }

    public String getAdminName() { return adminName; }
    public void setAdminName(String adminName) { this.adminName = adminName; }

    public String getCategoryDisplay() { return category; }
    public void setCategory(String category) { this.category = category; }

    public long getOverstayMinutes() { return overstayMinutes; }
    public void setOverstayMinutes(long overstayMinutes) { this.overstayMinutes = overstayMinutes; }

    public String getViolationTypeString() { return violationType != null ? violationType.name() : ""; }
    public String getStatusString() { return status != null ? status.name() : "PENDING"; }
    public String getActionString() { return actionTaken != null ? actionTaken.name() : "None"; }
    
    public Integer getResidentId() { return residentId; }
    public void setResidentId(Integer residentId) { this.residentId = residentId; }
    public String getResidentName() { return residentName; }
    public void setResidentName(String residentName) { this.residentName = residentName; }
}
