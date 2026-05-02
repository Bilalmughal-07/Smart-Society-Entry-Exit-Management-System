package com.smartsociety.controller;

import com.smartsociety.dao.ApprovalDAO;
import com.smartsociety.dao.ResidentDAO;
import com.smartsociety.model.Approval;
import com.smartsociety.service.NotificationService;
import com.smartsociety.service.QRCodeService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import com.smartsociety.model.AccessRule;
import com.smartsociety.dao.AccessRuleDAO;

/**
 * GRASP: Facade Controller for approval use cases.
 * UC-01: Create Pre-Approval, UC-02: Modify, UC-03: Cancel, UC-04: Approve on
 * Arrival
 */
public class ApprovalController {

    private final ApprovalDAO approvalDAO;
    private final ResidentDAO residentDAO;
    private final QRCodeService qrService;
    private final NotificationService notifService;

    public ApprovalController() {
        this.approvalDAO = new ApprovalDAO();
        this.residentDAO = new ResidentDAO();
        this.qrService = QRCodeService.getInstance();
        this.notifService = NotificationService.getInstance();
    }

    /**
     * UC-01: Create Pre-Approval (SD-01 flow)
     * validate → checkRules → create → save → generateQR → linkQR → return
     */
    public Approval createApproval(int residentId, String visitorName, String visitorContact,
            String category, String purpose, LocalDate visitDate,
            LocalTime startTime, LocalTime endTime, int durationMinutes) {
        // Step 1: validateVisitorData
        if (!approvalDAO.validateVisitorData(visitorName, category, visitDate)) {
            System.err.println("[ApprovalController] Validation failed.");
            return null;
        }
        // Step 1.5: check if visitor is blacklisted
        if (approvalDAO.isVisitorBlacklisted(visitorName, visitorContact)) {
            throw new IllegalArgumentException("Visitor is blacklisted and cannot be approved.");
        }
        // Step 2: checkVisitorAccessRules
        String ruleError = approvalDAO.checkVisitorAccessRules(category, visitDate, startTime, endTime);
        if (ruleError != null) {
            System.err.println("[ApprovalController] Access rules violation: " + ruleError);
            throw new IllegalArgumentException(ruleError);
        }
        // Step 3: create Approval object
        Approval approval = new Approval();
        approval.setResidentId(residentId);
        approval.setVisitorName(visitorName);
        approval.setVisitorContact(visitorContact);
        approval.setCategory(category);
        approval.setPurpose(purpose);
        approval.setVisitDate(visitDate);
        approval.setTimeWindowStart(startTime);
        approval.setTimeWindowEnd(endTime);
        approval.setDurationMinutes(durationMinutes);
        // Step 4: saveApproval
        int approvalId = approvalDAO.saveApproval(approval);
        if (approvalId <= 0) {
            System.err.println("[ApprovalController] Failed to save approval.");
            return null;
        }
        // Step 5: generateQRCode
        String qrCode = qrService.generateQRCode(approvalId);
        // Step 6: linkQRToApproval
        approvalDAO.linkQRToApproval(approvalId, qrCode);
        approval.setQrCode(qrCode);
        approval.setStatus(Approval.Status.APPROVED);
        System.out.println("[ApprovalController] Approval created: ID=" + approvalId + ", QR=" + qrCode);
        return approval;
    }

    /**
     * UC-02: Modify Approval (SD-02 flow)
     */
    public boolean modifyApproval(int approvalId, String visitorName, String category, String purpose,
            LocalDate visitDate, LocalTime start, LocalTime end, int duration) {
        Approval existing = approvalDAO.getApprovalById(approvalId);
        if (existing == null)
            return false;
        String oldQR = existing.getQrCode();
        existing.updateApprovalData(visitorName, category, purpose, visitDate, start, end, duration);
        boolean updated = approvalDAO.updateApproval(existing);
        if (updated && oldQR != null) {
            qrService.invalidateQR(oldQR);
            String newQR = qrService.generateQRCode(approvalId);
            approvalDAO.linkQRToApproval(approvalId, newQR);
        }
        return updated;
    }

    /**
     * UC-03: Cancel Approval (SD-03 flow)
     */
    public boolean cancelApproval(int approvalId) {
        String status = approvalDAO.checkEntryStatus(approvalId);
        if ("ENTERED".equals(status) || "COMPLETED".equals(status)) {
            System.err.println("[ApprovalController] Cannot cancel: visitor already entered.");
            return false;
        }
        Approval approval = approvalDAO.getApprovalById(approvalId);
        if (approval == null)
            return false;
        approval.setStatus(Approval.Status.CANCELLED);
        approvalDAO.updateApprovalStatus(approvalId, "CANCELLED");
        approvalDAO.invalidateQRCode(approvalId);
        notifService.notifyVisitorCancellation(approval.getResidentId(), approval.getVisitorName());
        return true;
    }

    /**
     * UC-04: Approve Visitor on Arrival (SD-04 flow)
     */
    public int createArrivalRequest(int guardId, int residentId, String visitorName, String purpose) {
        int requestId = residentDAO.storeArrivalRequest(guardId, residentId, visitorName, purpose);
        if (requestId > 0) {
            notifService.sendArrivalNotification(residentId, visitorName, purpose);
        }
        return requestId;
    }

    public boolean respondToArrivalRequest(int requestId, boolean approved) {
        return residentDAO.updateArrivalRequestStatus(requestId, approved ? "APPROVED" : "REJECTED");
    }

    public List<Approval> getApprovalsByResident(int residentId) {
        return approvalDAO.getApprovalsByResident(residentId);
    }

    public List<Approval> getActiveApprovalsByResident(int residentId) {
        return approvalDAO.getActiveApprovalsByResident(residentId);
    }

    public Approval getApprovalById(int approvalId) {
        return approvalDAO.getApprovalById(approvalId);
    }

    public List<Approval> getTodayApprovals() {
        return approvalDAO.getTodayApprovedApprovals();
    }

    public String[][] getPendingArrivalRequests(int residentId) {
        return residentDAO.getPendingRequests(residentId);
    }

    public List<AccessRule> getActiveRules() {
        return new AccessRuleDAO().getActiveRules();
    }
}
