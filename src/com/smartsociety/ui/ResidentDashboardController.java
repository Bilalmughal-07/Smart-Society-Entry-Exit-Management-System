package com.smartsociety.ui;

import com.smartsociety.controller.ApprovalController;
import com.smartsociety.controller.LoginController;
import com.smartsociety.model.Approval;
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
    @FXML private TextField visitorNameField, visitorContactField, purposeField, durationField, timeStartField, timeEndField;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private DatePicker visitDatePicker;
    @FXML private VBox qrDisplayBox;
    @FXML private Button notifBadge;
    @FXML private TabPane tabPane;

    // Approvals table
    @FXML private TableView<Approval> approvalsTable;
    @FXML private TableColumn<Approval, String> colVisitor, colCategory, colDate, colTime, colQR, colStatus;

    // Arrival requests table
    @FXML private TableView<String[]> arrivalRequestsTable;
    @FXML private TableColumn<String[], String> colArrVisitor, colArrPurpose, colArrGuard, colArrTime;

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

        loadApprovals();
        loadArrivalRequests();
        loadNotifications();
    }

    @FXML
    private void handleCreateApproval() {
        try {
            String name = visitorNameField.getText().trim();
            String contact = visitorContactField.getText().trim();
            String category = categoryCombo.getValue();
            String purpose = purposeField.getText().trim();
            LocalDate date = visitDatePicker.getValue();
            LocalTime start = LocalTime.parse(timeStartField.getText().trim());
            LocalTime end = LocalTime.parse(timeEndField.getText().trim());
            int duration = Integer.parseInt(durationField.getText().trim());

            if (name.isEmpty() || category == null) {
                showStatus("Please fill all required fields (*)", "error-label");
                return;
            }

            Approval approval = approvalCtrl.createApproval(session.getUserId(), name, contact, category, purpose, date, start, end, duration);
            if (approval != null) {
                showStatus("✅ Approval created successfully!", "success-label");
                qrCodeLabel.setText(approval.getQrCode());
                qrDisplayBox.setVisible(true);
                qrDisplayBox.setManaged(true);
                loadApprovals();
                handleClear();
            } else {
                showStatus("❌ Failed to create approval. Check access rules.", "error-label");
            }
        } catch (DateTimeParseException e) {
            showStatus("Invalid time format. Use HH:MM (e.g. 09:00)", "error-label");
        } catch (NumberFormatException e) {
            showStatus("Invalid duration. Enter a number.", "error-label");
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
        visitorNameField.clear(); visitorContactField.clear(); purposeField.clear();
        durationField.setText("60"); timeStartField.clear(); timeEndField.clear();
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
