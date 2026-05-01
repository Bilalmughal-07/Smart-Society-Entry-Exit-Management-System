package com.smartsociety.controller;

import com.smartsociety.dao.*;
import com.smartsociety.model.*;
import com.smartsociety.service.QRCodeService;
import java.time.LocalDateTime;
import java.util.List;

/**
 * GRASP: Facade Controller for gate operations.
 * UC-07 to UC-11 following exact sequence diagram flows.
 */
public class GateController {

    private final ApprovalDAO approvalDAO;
    private final EntryLogDAO entryLogDAO;
    private final UserDAO userDAO;
    private final QRCodeService qrService;

    public GateController() {
        this.approvalDAO = new ApprovalDAO();
        this.entryLogDAO = new EntryLogDAO();
        this.userDAO = new UserDAO();
        this.qrService = QRCodeService.getInstance();
    }

    /**
     * UC-07: Verify Visitor Approval (SD-07)
     * decodeQR → getApproval → validateTimeWindow → createEntryLog
     */
    public String verifyVisitorApproval(String qrData) {
        int approvalId = qrService.decodeApprovalQR(qrData);
        if (approvalId < 0) return "INVALID_QR";
        Approval a = approvalDAO.getApprovalById(approvalId);
        if (a == null) return "NOT_FOUND";
        if (a.getStatusEnum() != Approval.Status.APPROVED) return "STATUS_" + a.getStatusString();
        if (!approvalDAO.validateTimeWindow(a)) return "OUTSIDE_TIME_WINDOW";
        return "VALID:" + approvalId;
    }

    /**
     * UC-08: Register Visitor Entry (SD-08)
     * getApproval → confirmDetails → createEntryLog → updateStatus → addToOccupancy
     */
    public EntryLog registerVisitorEntry(int approvalId, int guardId) {
        Approval a = approvalDAO.getApprovalById(approvalId);
        if (a == null) return null;

        EntryLog log = new EntryLog();
        log.setPersonType(EntryLog.PersonType.VISITOR);
        log.setPersonId(a.getResidentId());
        log.setApprovalId(approvalId);
        log.setEntryTimestamp(LocalDateTime.now());
        log.setCategory(a.getCategory());
        log.setGuardId(guardId);

        int logId = entryLogDAO.createEntryLog(log);
        if (logId > 0) {
            approvalDAO.updateApprovalStatus(approvalId, "ENTERED");
            log.setPersonName(a.getVisitorName());
            System.out.println("[GateController] Visitor entry registered: " + a.getVisitorName());
            return log;
        }
        return null;
    }

    /**
     * UC-09: Register Visitor Exit (SD-09)
     * getActiveEntry → confirmExit → setExitTimestamp → persist → removeFromOccupancy
     */
    public EntryLog registerVisitorExit(int approvalId) {
        EntryLog log = entryLogDAO.getActiveEntryByPersonId(0, "VISITOR");
        // Try to find by approval
        if (log == null) {
            List<EntryLog> actives = entryLogDAO.getActiveEntries();
            for (EntryLog el : actives) {
                if (el.getPersonType() == EntryLog.PersonType.VISITOR &&
                    el.getApprovalId() != null && el.getApprovalId() == approvalId) {
                    log = el;
                    break;
                }
            }
        }
        if (log == null) return null;

        LocalDateTime exitTime = LocalDateTime.now();
        log.setExitTimestamp(exitTime);
        entryLogDAO.setExitTimestamp(log.getLogId(), exitTime);
        approvalDAO.updateApprovalStatus(approvalId, "COMPLETED");
        System.out.println("[GateController] Visitor exit registered. Duration: " + log.getDurationFormatted());
        return log;
    }

    /**
     * UC-10: Register Resident Entry (SD-10)
     * scanQR → verifyQR → getProfile → recordEntry → updateStatus → addToOccupancy
     */
    public EntryLog registerResidentEntry(String qrData, int guardId) {
        int residentId = qrService.decodeResidentQR(qrData);
        if (residentId < 0) {
            // Try lookup by QR code
            User u = userDAO.getUserByQRCode(qrData);
            if (u != null) residentId = u.getUserId();
            else return null;
        }
        User resident = userDAO.getUserById(residentId);
        if (resident == null || resident.getRole() != User.Role.RESIDENT) return null;

        EntryLog log = new EntryLog();
        log.setPersonType(EntryLog.PersonType.RESIDENT);
        log.setPersonId(residentId);
        log.setEntryTimestamp(LocalDateTime.now());
        log.setCategory("RESIDENT");
        log.setGuardId(guardId);

        int logId = entryLogDAO.createEntryLog(log);
        if (logId > 0) {
            userDAO.updateUserStatus(residentId, User.Status.INSIDE);
            log.setPersonName(resident.getFullName());
            log.setResidentUnit(resident.getUnitNumber());
            System.out.println("[GateController] Resident entry: " + resident.getFullName());
            return log;
        }
        return null;
    }

    /**
     * UC-11: Register Resident Exit (SD-11)
     * scanQR → verifyQR → getActiveEntry → recordExit → updateStatus → removeFromOccupancy
     */
    public EntryLog registerResidentExit(String qrData) {
        int residentId = qrService.decodeResidentQR(qrData);
        if (residentId < 0) {
            User u = userDAO.getUserByQRCode(qrData);
            if (u != null) residentId = u.getUserId();
            else return null;
        }

        EntryLog log = entryLogDAO.getActiveEntryByPersonId(residentId, "RESIDENT");
        if (log == null) return null;

        LocalDateTime exitTime = LocalDateTime.now();
        log.setExitTimestamp(exitTime);
        entryLogDAO.setExitTimestamp(log.getLogId(), exitTime);
        userDAO.updateUserStatus(residentId, User.Status.OUTSIDE);

        User resident = userDAO.getUserById(residentId);
        if (resident != null) {
            log.setPersonName(resident.getFullName());
            log.setResidentUnit(resident.getUnitNumber());
        }
        System.out.println("[GateController] Resident exit. Duration: " + log.getDurationFormatted());
        return log;
    }

    // Register exit by searching for active entry log by log ID
    public EntryLog registerExitByLogId(int logId) {
        List<EntryLog> actives = entryLogDAO.getActiveEntries();
        for (EntryLog el : actives) {
            if (el.getLogId() == logId) {
                LocalDateTime exitTime = LocalDateTime.now();
                el.setExitTimestamp(exitTime);
                entryLogDAO.setExitTimestamp(logId, exitTime);
                if (el.getPersonType() == EntryLog.PersonType.RESIDENT) {
                    userDAO.updateUserStatus(el.getPersonId(), User.Status.OUTSIDE);
                }
                if (el.getApprovalId() != null) {
                    approvalDAO.updateApprovalStatus(el.getApprovalId(), "COMPLETED");
                }
                return el;
            }
        }
        return null;
    }

    public List<EntryLog> getActiveEntries() { return entryLogDAO.getActiveEntries(); }
    public int getOccupancyCount() { return entryLogDAO.getActiveOccupancyCount(); }
    public Approval getApprovalByQR(String qr) { return approvalDAO.getApprovalByQRCode(qr); }
}
