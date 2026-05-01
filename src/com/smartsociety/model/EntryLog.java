package com.smartsociety.model;

import java.time.LocalDateTime;
import java.time.Duration;

/**
 * Domain model representing an Entry/Exit log record.
 * GRASP: Information Expert – knows its own entry timestamp and can set its own exit time.
 * Maps to the EntryLogs table. Used in UC-08, UC-09, UC-10, UC-11.
 */
public class EntryLog {

    public enum PersonType { VISITOR, RESIDENT }
    public enum LogStatus { ACTIVE, EXITED }

    private int logId;
    private PersonType personType;
    private int personId;
    private Integer approvalId;
    private LocalDateTime entryTimestamp;
    private LocalDateTime exitTimestamp;
    private String category;
    private Integer guardId;
    private LogStatus status;

    // Display-only fields (from JOINs)
    private String personName;
    private String guardName;
    private String residentUnit;

    // Default constructor
    public EntryLog() {
        this.status = LogStatus.ACTIVE;
        this.entryTimestamp = LocalDateTime.now();
    }

    /**
     * GRASP: Information Expert
     * EntryLog knows its own entry timestamp and can set its own exit time.
     * As per SD-09: setExitTimestamp(exitTimestamp)
     */
    public boolean setExitTimestamp(LocalDateTime exitTimestamp) {
        this.exitTimestamp = exitTimestamp;
        this.status = LogStatus.EXITED;
        return true;
    }

    /**
     * Calculate duration of stay in minutes.
     */
    public long getDurationMinutes() {
        if (entryTimestamp == null) return 0;
        LocalDateTime end = (exitTimestamp != null) ? exitTimestamp : LocalDateTime.now();
        return Duration.between(entryTimestamp, end).toMinutes();
    }

    public String getDurationFormatted() {
        long mins = getDurationMinutes();
        long hours = mins / 60;
        long remainMins = mins % 60;
        if (hours > 0) {
            return hours + "h " + remainMins + "m";
        }
        return remainMins + "m";
    }

    // Getters and Setters
    public int getLogId() { return logId; }
    public void setLogId(int logId) { this.logId = logId; }

    public PersonType getPersonType() { return personType; }
    public void setPersonType(PersonType personType) { this.personType = personType; }

    public int getPersonId() { return personId; }
    public void setPersonId(int personId) { this.personId = personId; }

    public Integer getApprovalId() { return approvalId; }
    public void setApprovalId(Integer approvalId) { this.approvalId = approvalId; }

    public LocalDateTime getEntryTimestamp() { return entryTimestamp; }
    public void setEntryTimestamp(LocalDateTime entryTimestamp) { this.entryTimestamp = entryTimestamp; }

    public LocalDateTime getExitTimestamp() { return exitTimestamp; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Integer getGuardId() { return guardId; }
    public void setGuardId(Integer guardId) { this.guardId = guardId; }

    public LogStatus getStatus() { return status; }
    public void setStatus(LogStatus status) { this.status = status; }

    public String getPersonName() { return personName; }
    public void setPersonName(String personName) { this.personName = personName; }

    public String getGuardName() { return guardName; }
    public void setGuardName(String guardName) { this.guardName = guardName; }

    public String getResidentUnit() { return residentUnit; }
    public void setResidentUnit(String residentUnit) { this.residentUnit = residentUnit; }

    public String getStatusString() { return status != null ? status.name() : "ACTIVE"; }
}
