package com.smartsociety.ui;

import com.smartsociety.controller.AdminController;
import com.smartsociety.controller.LoginController;
import com.smartsociety.model.*;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AdminDashboardController {

    @FXML private Label welcomeLabel, avatarLabel, occupancyCount, overstayCount, ruleStatusLabel;
    @FXML private TextField ruleNameField, ruleStartField, ruleEndField, ruleMaxDuration, ruleMaxVisitors;
    @FXML private ComboBox<String> ruleCategoryCombo;

    @FXML private TableView<AccessRule> rulesTable;
    @FXML private TableColumn<AccessRule, String> colRuleName, colRuleCat, colRuleTime, colRuleDuration, colRuleVisitors, colRuleActive;

    @FXML private TableView<EntryLog> occupancyTable;
    @FXML private TableColumn<EntryLog, String> colOccName, colOccType, colOccCategory, colOccEntry, colOccDuration, colOccUnit;

    @FXML private TableView<Violation> violationsTable;
    @FXML private TableColumn<Violation, String> colViolId, colViolType, colViolPerson, colViolResidentId, colViolResidentName, colViolDesc, colViolStatus, colViolAction, colViolDate;

    // Sidebar nav
    @FXML private VBox section0, section1, section2;
    @FXML private Button navBtn0, navBtn1, navBtn2;
    @FXML private Pane ambientLayer;
    private int currentSection = 0;

    private final AdminController adminCtrl = new AdminController();
    private UserSession session;

    @FXML
    public void initialize() {
        session = LoginController.getCurrentSession();
        if (session != null) {
            welcomeLabel.setText(session.getFullName());
            avatarLabel.setText(String.valueOf(session.getFullName().charAt(0)).toUpperCase());
        }

        ruleCategoryCombo.setItems(FXCollections.observableArrayList("Guest", "Delivery", "Service", "Contractor", "Other"));

        colRuleName.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getRuleName()));
        colRuleCat.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getCategory()));
        colRuleTime.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getTimeWindow()));
        colRuleDuration.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getMaxDurationMinutes() + " min"));
        colRuleVisitors.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cd.getValue().getMaxVisitorsPerDay())));
        colRuleActive.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getActiveStatus()));

        colOccName.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(
            cd.getValue().getPersonName() != null ? cd.getValue().getPersonName() : "ID:" + cd.getValue().getPersonId()));
        colOccType.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getPersonType().name()));
        colOccCategory.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getCategory()));
        colOccEntry.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(
            cd.getValue().getEntryTimestamp() != null ? cd.getValue().getEntryTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : ""));
        colOccDuration.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getDurationFormatted()));
        colOccUnit.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(
            cd.getValue().getResidentUnit() != null ? cd.getValue().getResidentUnit() : "—"));

        colViolId.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cd.getValue().getViolationId())));
        colViolType.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getViolationTypeString()));
        colViolPerson.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(
            cd.getValue().getPersonName() != null ? cd.getValue().getPersonName() : "Unknown"));
        colViolResidentId.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(
            cd.getValue().getResidentId() != null ? String.valueOf(cd.getValue().getResidentId()) : "—"));
        colViolResidentName.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(
            cd.getValue().getResidentName() != null ? cd.getValue().getResidentName() : "—"));
        colViolDesc.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getDescription()));
        colViolStatus.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getStatusString()));
        colViolAction.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getActionString()));
        colViolDate.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(
            cd.getValue().getDetectedAt() != null ? cd.getValue().getDetectedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : ""));

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

        DashboardUiUtils.useConstrainedTableColumns(rulesTable, occupancyTable, violationsTable);
        DashboardUiUtils.initializeSidebar(new Button[] { navBtn0, navBtn1, navBtn2 }, currentSection);

        loadRules();
        loadOccupancy();
        loadViolations();

        AnimationUtils.installAmbientMotion(ambientLayer);
        Button[] animatedButtons = { navBtn0, navBtn1, navBtn2 };
        for (Button button : animatedButtons) AnimationUtils.addHoverLift(button);
        AnimationUtils.introAnimation(section0);
    }

    // === Sidebar Navigation ===

    @FXML private void showSection0() { showSection(0); }
    @FXML private void showSection1() { showSection(1); }
    @FXML private void showSection2() { showSection(2); }

    private void showSection(int index) {
        if (index == currentSection) return;
        VBox[] sections = { section0, section1, section2 };
        AnimationUtils.switchSection(sections[currentSection], sections[index],
                index > currentSection ? 1 : -1);
        activateNav(index);
        currentSection = index;
    }

    private void activateNav(int index) {
        DashboardUiUtils.activateSidebarButton(new Button[] { navBtn0, navBtn1, navBtn2 }, index);
    }

    // === Access Rules ===

    @FXML
    private void handleSaveRule() {
        try {
            String name = ruleNameField.getText().trim();
            String cat = ruleCategoryCombo.getValue();
            String startStr = ruleStartField.getText().trim();
            String endStr = ruleEndField.getText().trim();
            String durStr = ruleMaxDuration.getText().trim();
            String visStr = ruleMaxVisitors.getText().trim();

            if (name.isEmpty() || cat == null || startStr.isEmpty() || endStr.isEmpty() || durStr.isEmpty() || visStr.isEmpty()) {
                ruleStatusLabel.setText("All fields are required.");
                ruleStatusLabel.getStyleClass().setAll("error-label");
                return;
            }

            AccessRule rule = new AccessRule();
            rule.setRuleName(name);
            rule.setCategory(cat);
            rule.setAllowedStartTime(LocalTime.parse(startStr));
            rule.setAllowedEndTime(LocalTime.parse(endStr));
            rule.setMaxDurationMinutes(Integer.parseInt(durStr));
            rule.setMaxVisitorsPerDay(Integer.parseInt(visStr));
            rule.setCreatedBy(session.getUserId());

            AccessRule selected = rulesTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                rule.setRuleId(selected.getRuleId());
                adminCtrl.updateRule(rule);
                ruleStatusLabel.setText("Rule updated."); ruleStatusLabel.getStyleClass().setAll("success-label");
            } else {
                int id = adminCtrl.defineNewRule(rule);
                ruleStatusLabel.setText(id > 0 ? "Rule created." : "Failed to create rule.");
                ruleStatusLabel.getStyleClass().setAll(id > 0 ? "success-label" : "error-label");
            }
            loadRules();
        } catch (java.time.format.DateTimeParseException e) {
            ruleStatusLabel.setText("Invalid time format. Use HH:MM");
            ruleStatusLabel.getStyleClass().setAll("error-label");
        } catch (NumberFormatException e) {
            ruleStatusLabel.setText("Duration and visitors must be numbers.");
            ruleStatusLabel.getStyleClass().setAll("error-label");
        } catch (IllegalArgumentException e) {
            ruleStatusLabel.setText(e.getMessage());
            ruleStatusLabel.getStyleClass().setAll("error-label");
        } catch (Exception e) {
            ruleStatusLabel.setText("System error occurred.");
            ruleStatusLabel.getStyleClass().setAll("error-label");
        }
    }

    @FXML
    private void handleDeleteRule() {
        AccessRule selected = rulesTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            adminCtrl.deleteRule(selected.getRuleId());
            ruleStatusLabel.setText("Rule deleted.");
            loadRules();
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
        Stage stage = (Stage) welcomeLabel.getScene().getWindow();
        AnimationUtils.sceneTransition(welcomeLabel.getScene().getRoot(), () -> {
            try {
                new LoginController().logout();
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
                Scene scene = new Scene(loader.load(), 560, 660);
                scene.setCamera(new PerspectiveCamera());
                scene.getStylesheets().add(getClass().getResource("/css/glassmorphism.css").toExternalForm());
                stage.setTitle("Smart Society - Login");
                stage.setScene(scene);
                stage.centerOnScreen();
            } catch (Exception e) { e.printStackTrace(); }
        });
    }
}
