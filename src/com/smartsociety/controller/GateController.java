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
        if (a.getStatusEnum() != Approval.Status.APPROVED) {
            if (a.getStatusEnum() == Approval.Status.ENTERED) return "STATUS_ENTERED:" + approvalId;
            return "STATUS_" + a.getStatusString();
        }
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
        log.setPersonId(approvalId);
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
        EntryLog log = entryLogDAO.getActiveEntryByPersonId(approvalId, "VISITOR");
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
        User resident = resolveResidentByQR(qrData);
        if (resident == null || resident.getRole() != User.Role.RESIDENT) return null;
        if (resident.getStatus() == User.Status.INSIDE ||
                entryLogDAO.getActiveEntryByPersonId(resident.getUserId(), "RESIDENT") != null) {
            return null;
        }

        EntryLog log = new EntryLog();
        log.setPersonType(EntryLog.PersonType.RESIDENT);
        log.setPersonId(resident.getUserId());
        log.setEntryTimestamp(LocalDateTime.now());
        log.setCategory("RESIDENT");
        log.setGuardId(guardId);

        int logId = entryLogDAO.createEntryLog(log);
        if (logId > 0) {
            userDAO.updateUserStatus(resident.getUserId(), User.Status.INSIDE);
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
        User resident = resolveResidentByQR(qrData);
        if (resident == null || resident.getRole() != User.Role.RESIDENT) return null;

        EntryLog log = entryLogDAO.getActiveEntryByPersonId(resident.getUserId(), "RESIDENT");
        if (log == null) return null;

        LocalDateTime exitTime = LocalDateTime.now();
        log.setExitTimestamp(exitTime);
        entryLogDAO.setExitTimestamp(log.getLogId(), exitTime);
        userDAO.updateUserStatus(resident.getUserId(), User.Status.OUTSIDE);

        log.setPersonName(resident.getFullName());
        log.setResidentUnit(resident.getUnitNumber());
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

    public boolean reportQRSharingViolation(int approvalId, int guardId) {
        Approval a = approvalDAO.getApprovalById(approvalId);
        if (a == null) return false;

        EntryLog log = null;
        List<EntryLog> actives = entryLogDAO.getActiveEntries();
        for (EntryLog el : actives) {
            if (el.getPersonType() == EntryLog.PersonType.VISITOR && el.getApprovalId() != null && el.getApprovalId() == approvalId) {
                log = el; break;
            }
        }
        
        if (log == null) {
            return false;
        }

        ViolationDAO vDao = new ViolationDAO();
        Violation v1 = new Violation();
        v1.setLogId(log.getLogId());
        v1.setViolationType(Violation.ViolationType.UNAUTHORIZED);
        v1.setDescription("Visitor attempted to share QR code for multiple entries. Original approval ID: " + approvalId);
        vDao.createViolation(v1);

        return true;
    }

    public boolean reportResidentQRSharingViolation(String qrData, int guardId) {
        User resident = resolveResidentByQR(qrData);
        if (resident == null || resident.getRole() != User.Role.RESIDENT) return false;

        EntryLog log = entryLogDAO.getActiveEntryByPersonId(resident.getUserId(), "RESIDENT");
        if (log == null) return false;

        ViolationDAO vDao = new ViolationDAO();
        Violation violation = new Violation();
        violation.setLogId(log.getLogId());
        violation.setViolationType(Violation.ViolationType.UNAUTHORIZED);
        violation.setDescription("Resident QR attempted for another entry while resident is already inside. Resident ID: "
                + resident.getUserId());
        return vDao.createViolation(violation) > 0;
    }

    public List<EntryLog> getActiveEntries() { return entryLogDAO.getActiveEntries(); }
    public int getOccupancyCount() { return entryLogDAO.getActiveOccupancyCount(); }
    public Approval getApprovalByQR(String qr) { return approvalDAO.getApprovalByQRCode(qr); }

    public User getResidentByQR(String qrData) { return resolveResidentByQR(qrData); }

    private User resolveResidentByQR(String qrData) {
        if (qrData == null || qrData.trim().isEmpty()) return null;

        User resident = userDAO.getUserByQRCode(qrData.trim());
        if (resident != null) return resident;

        int residentId = qrService.decodeResidentQR(qrData);
        if (residentId > 0) return userDAO.getUserById(residentId);

        return null;
    }
}
