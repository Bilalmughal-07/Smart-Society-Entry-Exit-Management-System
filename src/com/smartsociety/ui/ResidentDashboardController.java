package com.smartsociety.ui;

import com.smartsociety.controller.ApprovalController;
import com.smartsociety.controller.LoginController;
import com.smartsociety.model.Approval;
import com.smartsociety.model.AccessRule;
import com.smartsociety.model.UserSession;
import com.smartsociety.service.NotificationService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * UI Controller for Resident Dashboard.
 */
public class ResidentDashboardController {

    @FXML private Label welcomeLabel, approvalStatusLabel, qrCodeLabel;
    @FXML private TextField visitorNameField, visitorContactField, purposeField, durationField, timeHourField, timeMinField, timeEndField;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private DatePicker visitDatePicker;
    @FXML private VBox qrDisplayBox;
    @FXML private ImageView qrImageView;
    @FXML private Button notifBadge;
    @FXML private TabPane tabPane;

    // Approvals table
    @FXML private TableView<Approval> approvalsTable;
    @FXML private TableColumn<Approval, String> colVisitor, colCategory, colDate, colTime, colQR, colStatus;

    // Arrival requests table
    @FXML private TableView<String[]> arrivalRequestsTable;
    @FXML private TableColumn<String[], String> colArrVisitor, colArrPurpose, colArrGuard, colArrTime;

    // Access Rules table
    @FXML private TableView<AccessRule> rulesTable;
    @FXML private TableColumn<AccessRule, String> colRuleName, colRuleCat, colRuleTime, colRuleDuration, colRuleVisitors;

    @FXML private ListView<String> notificationsList;

    private final ApprovalController approvalCtrl = new ApprovalController();
    private final NotificationService notifService = NotificationService.getInstance();
    private UserSession session;

    @FXML
    public void initialize() {
        session = LoginController.getCurrentSession();
        if (session != null) {
            welcomeLabel.setText("Welcome, " + session.getFullName() + " | " + session.getUnitNumber());
        }

        categoryCombo.setItems(FXCollections.observableArrayList("Guest", "Delivery", "Service", "Contractor", "Other"));
        visitDatePicker.setValue(LocalDate.now());

        // Setup approvals table columns
        colVisitor.setCellValueFactory(new PropertyValueFactory<>("visitorName"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colDate.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getVisitDate().toString()));
        colTime.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getTimeWindow()));
        colQR.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getQrCode() != null ? cd.getValue().getQrCode() : "—"));
        colStatus.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getStatusString()));

        // Setup arrival requests table
        colArrVisitor.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().length > 1 ? cd.getValue()[1] : ""));
        colArrPurpose.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().length > 2 ? cd.getValue()[2] : ""));
        colArrGuard.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().length > 3 ? cd.getValue()[3] : ""));
        colArrTime.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().length > 4 ? cd.getValue()[4] : ""));

        // Setup rules table
        colRuleName.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getRuleName()));
        colRuleCat.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getCategory()));
        colRuleTime.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getTimeWindow()));
        colRuleDuration.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getMaxDurationMinutes() + " min"));
        colRuleVisitors.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cd.getValue().getMaxVisitorsPerDay())));

        loadApprovals();
        loadArrivalRequests();
        loadNotifications();
        loadRules();

        // Add listeners for auto-calculating end time
        javafx.beans.value.ChangeListener<String> timeCalcListener = (obs, oldVal, newVal) -> updateEndTime();
        timeHourField.textProperty().addListener(timeCalcListener);
        timeMinField.textProperty().addListener(timeCalcListener);
        durationField.textProperty().addListener(timeCalcListener);

        // Auto-pad hours and minutes on focus loss
        timeHourField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused && !timeHourField.getText().isEmpty()) {
                try {
                    int h = Integer.parseInt(timeHourField.getText().trim());
                    timeHourField.setText(String.format("%02d", h));
                } catch (NumberFormatException ignored) {}
            }
        });
        timeMinField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused && !timeMinField.getText().isEmpty()) {
                try {
                    int m = Integer.parseInt(timeMinField.getText().trim());
                    timeMinField.setText(String.format("%02d", m));
                } catch (NumberFormatException ignored) {}
            }
        });
    }

    private void updateEndTime() {
        try {
            if (timeHourField.getText().trim().isEmpty() || timeMinField.getText().trim().isEmpty() || durationField.getText().trim().isEmpty()) {
                timeEndField.setText("--:--");
                return;
            }
            int h = Integer.parseInt(timeHourField.getText().trim());
            int m = Integer.parseInt(timeMinField.getText().trim());
            int dur = Integer.parseInt(durationField.getText().trim());
            LocalTime start = LocalTime.of(h, m);
            LocalTime end = start.plusMinutes(dur);
            timeEndField.setText(end.toString());
        } catch (Exception e) {
            timeEndField.setText("--:--");
        }
    }

    @FXML
    private void handleCreateApproval() {
        try {
            String name = visitorNameField.getText().trim();
            String contact = visitorContactField.getText().trim();
            String category = categoryCombo.getValue();
            String purpose = purposeField.getText().trim();
            LocalDate date = visitDatePicker.getValue();
            
            String hourStr = timeHourField.getText().trim();
            String minStr = timeMinField.getText().trim();
            if (hourStr.isEmpty() || minStr.isEmpty()) {
                showStatus("Please enter arrival time", "error-label"); return;
            }
            
            int h = Integer.parseInt(hourStr);
            int m = Integer.parseInt(minStr);
            LocalTime start = LocalTime.of(h, m);
            int duration = Integer.parseInt(durationField.getText().trim());
            LocalTime end = start.plusMinutes(duration);

            if (name.isEmpty() || category == null) {
                showStatus("Please fill all required fields (*)", "error-label");
                return;
            }

            Approval approval = approvalCtrl.createApproval(session.getUserId(), name, contact, category, purpose, date, start, end, duration);
            if (approval != null) {
                showStatus("✅ Approval created successfully!", "success-label");
                qrCodeLabel.setText(approval.getQrCode());
                javafx.scene.image.Image qrImg = com.smartsociety.service.QRCodeService.getInstance().generateQRImage(approval.getQrCode());
                if (qrImg != null) qrImageView.setImage(qrImg);
                qrDisplayBox.setVisible(true);
                qrDisplayBox.setManaged(true);
                loadApprovals();
                clearInputs(); // Only clear inputs, leave QR visible
            } else {
                showStatus("❌ Failed to create approval.", "error-label");
            }
        } catch (NumberFormatException e) {
            showStatus("Invalid duration. Enter a number.", "error-label");
        } catch (IllegalArgumentException e) {
            showStatus("❌ " + e.getMessage(), "error-label");
        } catch (DateTimeParseException e) {
            showStatus("Invalid time format. Use HH:MM (e.g. 09:00)", "error-label");
        }
    }

    @FXML
    private void handleCancelApproval() {
        Approval selected = approvalsTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showStatus("Select an approval to cancel.", "warning-label"); return; }
        boolean cancelled = approvalCtrl.cancelApproval(selected.getApprovalId());
        showStatus(cancelled ? "Approval cancelled." : "Cannot cancel this approval.", cancelled ? "success-label" : "error-label");
        loadApprovals();
    }

    @FXML private void handleClear() {
        clearInputs();
        qrDisplayBox.setVisible(false); qrDisplayBox.setManaged(false); qrImageView.setImage(null);
    }

    private void clearInputs() {
        visitorNameField.clear(); visitorContactField.clear(); purposeField.clear();
        durationField.setText("60"); timeHourField.clear(); timeMinField.clear(); timeEndField.clear();
        categoryCombo.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleApproveArrival() {
        String[] selected = arrivalRequestsTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        approvalCtrl.respondToArrivalRequest(Integer.parseInt(selected[0]), true);
        loadArrivalRequests();
    }

    @FXML
    private void handleRejectArrival() {
        String[] selected = arrivalRequestsTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        approvalCtrl.respondToArrivalRequest(Integer.parseInt(selected[0]), false);
        loadArrivalRequests();
    }

    @FXML
    public void loadApprovals() {
        List<Approval> list = approvalCtrl.getApprovalsByResident(session.getUserId());
        approvalsTable.setItems(FXCollections.observableArrayList(list));
    }

    @FXML
    public void loadArrivalRequests() {
        String[][] requests = approvalCtrl.getPendingArrivalRequests(session.getUserId());
        ObservableList<String[]> data = FXCollections.observableArrayList(requests);
        arrivalRequestsTable.setItems(data);
    }

    @FXML
    public void loadNotifications() {
        List<String[]> notifs = notifService.getUnreadNotifications(session.getUserId());
        ObservableList<String> items = FXCollections.observableArrayList();
        for (String[] n : notifs) items.add("[" + n[2] + "] " + n[1] + "  (" + n[3] + ")");
        notificationsList.setItems(items);
        notifBadge.setText("🔔 " + notifs.size());
    }

    @FXML private void showNotifications() { tabPane.getSelectionModel().select(3); }
    @FXML private void markAllRead() { notifService.markAllAsRead(session.getUserId()); loadNotifications(); }

    @FXML
    public void loadRules() {
        List<AccessRule> rules = approvalCtrl.getActiveRules();
        rulesTable.setItems(FXCollections.observableArrayList(rules));
    }

    @FXML
    private void handleLogout() {
        try {
            new LoginController().logout();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Scene scene = new Scene(loader.load(), 500, 600);
            scene.getStylesheets().add(getClass().getResource("/css/glassmorphism.css").toExternalForm());
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setTitle("Smart Society - Login");
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showStatus(String msg, String styleClass) {
        approvalStatusLabel.setText(msg);
        approvalStatusLabel.getStyleClass().setAll(styleClass);
    }
}
