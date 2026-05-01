package com.smartsociety.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Domain model representing a Visitor Approval.
 * GRASP: Information Expert – knows its own state and manages status transitions.
 * Maps to the Approvals table in the database.
 */
public class Approval {

    public enum Status { PENDING, APPROVED, ENTERED, COMPLETED, CANCELLED, REJECTED }

    private int approvalId;
    private int residentId;
    private String visitorName;
    private String visitorContact;
    private String category;
    private String purpose;
    private LocalDate visitDate;
    private LocalTime timeWindowStart;
    private LocalTime timeWindowEnd;
    private int durationMinutes;
    private String qrCode;
    private Status status;
    private LocalDateTime createdAt;

    // Additional fields for display (joined from Users)
    private String residentName;
    private String residentUnit;

    // Default constructor
    public Approval() {
        this.status = Status.PENDING;
        this.durationMinutes = 60;
    }

    /**
     * GRASP: Information Expert
     * Approval manages its own status transitions.
     * As per SD-03: setStatus(CANCELLED)
     */
    public boolean setStatus(Status newStatus) {
        this.status = newStatus;
        return true;
    }

    /**
     * GRASP: Information Expert
     * Approval enforces its own update rules.
     * As per SD-02: updateApprovalData(updatedDetails)
     */
    public boolean updateApprovalData(String visitorName, String category, String purpose,
                                       LocalDate visitDate, LocalTime start, LocalTime end, int duration) {
        this.visitorName = visitorName;
        this.category = category;
        this.purpose = purpose;
        this.visitDate = visitDate;
        this.timeWindowStart = start;
        this.timeWindowEnd = end;
        this.durationMinutes = duration;
        return true;
    }

    // Getters and Setters
    public int getApprovalId() { return approvalId; }
    public void setApprovalId(int approvalId) { this.approvalId = approvalId; }

    public int getResidentId() { return residentId; }
    public void setResidentId(int residentId) { this.residentId = residentId; }

    public String getVisitorName() { return visitorName; }
    public void setVisitorName(String visitorName) { this.visitorName = visitorName; }

    public String getVisitorContact() { return visitorContact; }
    public void setVisitorContact(String visitorContact) { this.visitorContact = visitorContact; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public LocalDate getVisitDate() { return visitDate; }
    public void setVisitDate(LocalDate visitDate) { this.visitDate = visitDate; }

    public LocalTime getTimeWindowStart() { return timeWindowStart; }
    public void setTimeWindowStart(LocalTime timeWindowStart) { this.timeWindowStart = timeWindowStart; }

    public LocalTime getTimeWindowEnd() { return timeWindowEnd; }
    public void setTimeWindowEnd(LocalTime timeWindowEnd) { this.timeWindowEnd = timeWindowEnd; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }

    public Status getStatusEnum() { return status; }
    public String getStatusString() { return status != null ? status.name() : "PENDING"; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getResidentName() { return residentName; }
    public void setResidentName(String residentName) { this.residentName = residentName; }

    public String getResidentUnit() { return residentUnit; }
    public void setResidentUnit(String residentUnit) { this.residentUnit = residentUnit; }

    public String getTimeWindow() {
        return timeWindowStart + " - " + timeWindowEnd;
    }
}
