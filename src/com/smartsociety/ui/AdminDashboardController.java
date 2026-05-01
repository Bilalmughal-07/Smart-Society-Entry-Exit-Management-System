package com.smartsociety.ui;

import com.smartsociety.controller.AdminController;
import com.smartsociety.controller.LoginController;
import com.smartsociety.model.*;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * UI Controller for Admin Dashboard.
 */
public class AdminDashboardController {

    @FXML private Label welcomeLabel, occupancyCount, overstayCount, ruleStatusLabel;
    @FXML private TextField ruleNameField, ruleStartField, ruleEndField, ruleMaxDuration, ruleMaxVisitors;
    @FXML private ComboBox<String> ruleCategoryCombo;

    // Rules table
    @FXML private TableView<AccessRule> rulesTable;
    @FXML private TableColumn<AccessRule, String> colRuleName, colRuleCat, colRuleTime, colRuleDuration, colRuleVisitors, colRuleActive;

    // Occupancy table
    @FXML private TableView<EntryLog> occupancyTable;
    @FXML private TableColumn<EntryLog, String> colOccName, colOccType, colOccCategory, colOccEntry, colOccDuration, colOccUnit;

    // Violations table
    @FXML private TableView<Violation> violationsTable;
    @FXML private TableColumn<Violation, String> colViolId, colViolType, colViolPerson, colViolDesc, colViolStatus, colViolAction, colViolDate;

    private final AdminController adminCtrl = new AdminController();
    private UserSession session;

    @FXML
    public void initialize() {
        session = LoginController.getCurrentSession();
        if (session != null) welcomeLabel.setText("Admin: " + session.getFullName());

        ruleCategoryCombo.setItems(FXCollections.observableArrayList("Guest", "Delivery", "Service", "Contractor", "Other"));

        // Rules table columns
        colRuleName.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getRuleName()));
        colRuleCat.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getCategory()));
        colRuleTime.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getTimeWindow()));
        colRuleDuration.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getMaxDurationMinutes() + " min"));
        colRuleVisitors.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cd.getValue().getMaxVisitorsPerDay())));
        colRuleActive.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getActiveStatus()));

        // Occupancy table columns
        colOccName.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(
            cd.getValue().getPersonName() != null ? cd.getValue().getPersonName() : "ID:" + cd.getValue().getPersonId()));
        colOccType.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getPersonType().name()));
        colOccCategory.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getCategory()));
        colOccEntry.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(
            cd.getValue().getEntryTimestamp() != null ? cd.getValue().getEntryTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : ""));
        colOccDuration.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getDurationFormatted()));
        colOccUnit.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(
            cd.getValue().getResidentUnit() != null ? cd.getValue().getResidentUnit() : "—"));

        // Violations table columns
        colViolId.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cd.getValue().getViolationId())));
        colViolType.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getViolationTypeString()));
        colViolPerson.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(
            cd.getValue().getPersonName() != null ? cd.getValue().getPersonName() : "Unknown"));
        colViolDesc.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getDescription()));
        colViolStatus.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getStatusString()));
        colViolAction.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getActionString()));
        colViolDate.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(
            cd.getValue().getDetectedAt() != null ? cd.getValue().getDetectedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : ""));

        // Selection listener for rules table to populate edit fields
        rulesTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                ruleNameField.setText(newVal.getRuleName());
                ruleCategoryCombo.setValue(newVal.getCategory());
                ruleStartField.setText(newVal.getAllowedStartTime().toString());
                ruleEndField.setText(newVal.getAllowedEndTime().toString());
                ruleMaxDuration.setText(String.valueOf(newVal.getMaxDurationMinutes()));
                ruleMaxVisitors.setText(String.valueOf(newVal.getMaxVisitorsPerDay()));
            }
        });

        loadRules();
        loadOccupancy();
        loadViolations();
    }

    // === Access Rules ===
    @FXML
    private void handleSaveRule() {
        try {
            AccessRule rule = new AccessRule();
            rule.setRuleName(ruleNameField.getText().trim());
            rule.setCategory(ruleCategoryCombo.getValue());
            rule.setAllowedStartTime(LocalTime.parse(ruleStartField.getText().trim()));
            rule.setAllowedEndTime(LocalTime.parse(ruleEndField.getText().trim()));
            rule.setMaxDurationMinutes(Integer.parseInt(ruleMaxDuration.getText().trim()));
            rule.setMaxVisitorsPerDay(Integer.parseInt(ruleMaxVisitors.getText().trim()));
            rule.setCreatedBy(session.getUserId());

            AccessRule selected = rulesTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                rule.setRuleId(selected.getRuleId());
                adminCtrl.updateRule(rule);
                ruleStatusLabel.setText("✅ Rule updated."); ruleStatusLabel.getStyleClass().setAll("success-label");
            } else {
                int id = adminCtrl.defineNewRule(rule);
                ruleStatusLabel.setText(id > 0 ? "✅ Rule created." : "❌ Failed.");
                ruleStatusLabel.getStyleClass().setAll(id > 0 ? "success-label" : "error-label");
            }
            loadRules();
        } catch (Exception e) {
            ruleStatusLabel.setText("❌ Invalid input: " + e.getMessage());
            ruleStatusLabel.getStyleClass().setAll("error-label");
        }
    }

    @FXML
    private void handleDeleteRule() {
        AccessRule selected = rulesTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            adminCtrl.deleteRule(selected.getRuleId());
            ruleStatusLabel.setText("Rule deleted."); loadRules();
        }
    }

    @FXML public void loadRules() {
        rulesTable.setItems(FXCollections.observableArrayList(adminCtrl.getExistingRules()));
    }

    // === Occupancy ===
    @FXML public void loadOccupancy() {
        List<EntryLog> entries = adminCtrl.getActiveEntries();
        occupancyTable.setItems(FXCollections.observableArrayList(entries));
        occupancyCount.setText(String.valueOf(entries.size()));
        List<EntryLog> overstays = adminCtrl.detectOverstays();
        overstayCount.setText(String.valueOf(overstays.size()));
    }

    @FXML
    private void handleAutoDetect() {
        adminCtrl.autoCreateViolations();
        loadViolations();
        loadOccupancy();
    }

    // === Violations ===
    @FXML public void loadViolations() {
        violationsTable.setItems(FXCollections.observableArrayList(adminCtrl.getAllViolations()));
    }

    @FXML private void handleWarning() { takeViolationAction("WARNING"); }
    @FXML private void handleBlacklist() { takeViolationAction("BLACKLIST"); }
    @FXML private void handleResolve() { takeViolationAction("FINE"); }

    private void takeViolationAction(String action) {
        Violation selected = violationsTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        adminCtrl.takeAction(selected.getViolationId(), action, session.getUserId());
        loadViolations();
    }

    @FXML
    private void handleLogout() {
        try {
            new LoginController().logout();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Scene scene = new Scene(loader.load(), 500, 600);
            scene.getStylesheets().add(getClass().getResource("/css/glassmorphism.css").toExternalForm());
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setTitle("Smart Society - Login"); stage.setScene(scene); stage.centerOnScreen();
        } catch (Exception e) { e.printStackTrace(); }
    }
}
