package com.smartsociety.controller;

import com.smartsociety.dao.*;
import com.smartsociety.model.*;
import com.smartsociety.service.NotificationService;
import java.util.List;

/**
 * GRASP: Session Controller for admin operations.
 * UC-12: Access Rules, UC-13: Occupancy Dashboard, UC-14: Violations
 */
public class AdminController {

    private final AccessRuleDAO ruleDAO;
    private final EntryLogDAO entryLogDAO;
    private final ViolationDAO violationDAO;
    private final NotificationService notifService;

    public AdminController() {
        this.ruleDAO = new AccessRuleDAO();
        this.entryLogDAO = new EntryLogDAO();
        this.violationDAO = new ViolationDAO();
        this.notifService = NotificationService.getInstance();
    }

    // === UC-12: Define Visitor Access Rules (SD-12) ===

    public List<AccessRule> getExistingRules() { return ruleDAO.getAllRules(); }
    public List<AccessRule> getActiveRules() { return ruleDAO.getActiveRules(); }

    public int defineNewRule(AccessRule rule) {
        if (!ruleDAO.validateRules(rule)) {
            System.err.println("[AdminController] Rule validation failed."); return -1;
        }
        int id = ruleDAO.saveRule(rule);
        if (id > 0) ruleDAO.activateRule(id);
        return id;
    }

    public boolean updateRule(AccessRule rule) { return ruleDAO.updateRule(rule); }
    public boolean deleteRule(int ruleId) { return ruleDAO.deleteRule(ruleId); }

    // === UC-13: Monitor Real-Time Occupancy (SD-13) ===

    public List<EntryLog> getActiveEntries() { return entryLogDAO.getActiveEntries(); }
    public int getOccupancyCount() { return entryLogDAO.getActiveOccupancyCount(); }

    public List<EntryLog> detectOverstays() {
        int maxDuration = ruleDAO.getMaxDurationForCategory("Guest");
        return entryLogDAO.detectOverstays(maxDuration);
    }

    public void autoCreateViolations() {
        List<EntryLog> overstays = detectOverstays();
        for (EntryLog el : overstays) {
            if (!violationDAO.existsForLog(el.getLogId())) {
                Violation v = new Violation();
                v.setLogId(el.getLogId());
                v.setViolationType(Violation.ViolationType.OVERSTAY);
                v.setDescription("Overstay: " + el.getDurationFormatted() + " by " +
                    (el.getPersonName() != null ? el.getPersonName() : "Unknown"));
                violationDAO.createViolation(v);
            }
        }
    }

    // === UC-14: Review Violations (SD-14) ===

    public List<Violation> getPendingViolations() { return violationDAO.getPendingViolations(); }
    public List<Violation> getAllViolations() { return violationDAO.getAllViolations(); }
    public Violation getViolationDetails(int violationId) { return violationDAO.getViolationById(violationId); }

    public boolean takeAction(int violationId, String action, int adminId) {
        Violation v = violationDAO.getViolationById(violationId);
        if (v == null) return false;

        v.applyAction(Violation.Action.valueOf(action));
        boolean persisted = violationDAO.persistViolationAction(violationId, action, adminId);
        if (persisted) {
            String info = v.getViolationTypeString() + " by " + v.getPersonName();
            notifService.notifyAboutViolation(0, info, action);
        }
        return persisted;
    }
}
