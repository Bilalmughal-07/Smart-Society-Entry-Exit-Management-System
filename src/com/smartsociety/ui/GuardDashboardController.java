package com.smartsociety.ui;

import com.smartsociety.controller.ApprovalController;
import com.smartsociety.controller.GateController;
import com.smartsociety.controller.LoginController;
import com.smartsociety.dao.UserDAO;
import com.smartsociety.model.*;
import com.smartsociety.service.QRCodeService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.util.List;

/**
 * UI Controller for Guard Dashboard.
 */
public class GuardDashboardController {

    @FXML private Label welcomeLabel, occupancyLabel, scanResultLabel;
    @FXML private Label verifyVisitorName, verifyDetails, walkInStatusLabel;
    @FXML private TextField qrInputField, walkInVisitorName, walkInPurpose;
    @FXML private VBox verifyResultBox;
    @FXML private Button registerEntryBtn;
    @FXML private ComboBox<String> residentCombo;

    // Active entries table
    @FXML private TableView<EntryLog> activeEntriesTable;
    @FXML private TableColumn<EntryLog, String> colName, colType, colCat, colEntry, colDuration, colLogStatus;

    // Today's approvals table
    @FXML private TableView<Approval> todayApprovalsTable;
    @FXML private TableColumn<Approval, String> colTVisitor, colTResident, colTCategory, colTTime, colTStatus;

    private final GateController gateCtrl = new GateController();
    private final ApprovalController approvalCtrl = new ApprovalController();
    private final UserDAO userDAO = new UserDAO();
    private UserSession session;
    private int verifiedApprovalId = -1;
    private String verifiedQrData = null;
    private String verifiedQrType = null;

    @FXML
    public void initialize() {
        session = LoginController.getCurrentSession();
        if (session != null) welcomeLabel.setText("Guard: " + session.getFullName());

        // Active entries table columns
        colName.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(
            cd.getValue().getPersonName() != null ? cd.getValue().getPersonName() : "ID:" + cd.getValue().getPersonId()));
        colType.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getPersonType().name()));
        colCat.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getCategory()));
        colEntry.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(
            cd.getValue().getEntryTimestamp() != null ? cd.getValue().getEntryTimestamp().toString().replace("T", " ") : ""));
        colDuration.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getDurationFormatted()));
        colLogStatus.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getStatusString()));

        // Today's approvals columns
        colTVisitor.setCellValueFactory(new PropertyValueFactory<>("visitorName"));
        colTResident.setCellValueFactory(new PropertyValueFactory<>("residentName"));
        colTCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colTTime.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getTimeWindow()));
        colTStatus.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getStatusString()));

        // Load resident list for walk-in combo
        List<User> residents = userDAO.getAllResidents();
        for (User r : residents) {
            residentCombo.getItems().add(r.getUserId() + " - " + r.getFullName() + " (" + r.getUnitNumber() + ")");
        }

        loadActiveEntries();
        loadTodayApprovals();
        updateOccupancy();
    }

    @FXML
    private void handleVerifyQR() {
        String qrData = qrInputField.getText().trim();
        if (qrData.isEmpty()) { scanResultLabel.setText("Enter QR code data."); return; }

        QRCodeService qrService = QRCodeService.getInstance();
        String qrType = qrService.getQRType(qrData);
        verifiedQrData = qrData;
        verifiedQrType = qrType;

        if ("APPROVAL".equals(qrType)) {
            String result = gateCtrl.verifyVisitorApproval(qrData);
            if (result.startsWith("VALID:")) {
                verifiedApprovalId = Integer.parseInt(result.split(":")[1]);
                Approval a = approvalCtrl.getApprovalById(verifiedApprovalId);
                verifyVisitorName.setText("Visitor: " + a.getVisitorName());
                verifyDetails.setText("Category: " + a.getCategory() + " | Time: " + a.getTimeWindow() +
                    "\nResident: " + a.getResidentName() + " | Unit: " + a.getResidentUnit());
                verifyResultBox.setVisible(true); verifyResultBox.setManaged(true);
                registerEntryBtn.setText("✅ Register Visitor Entry");
                scanResultLabel.setText("✅ Visitor approval verified!");
                scanResultLabel.getStyleClass().setAll("success-label");
            } else {
                verifyResultBox.setVisible(false); verifyResultBox.setManaged(false);
                scanResultLabel.setText("❌ Verification failed: " + result);
                scanResultLabel.getStyleClass().setAll("error-label");
            }
        } else if ("RESIDENT".equals(qrType)) {
            int resId = qrService.decodeResidentQR(qrData);
            if (resId > 0) {
                User resident = userDAO.getUserById(resId);
                if (resident != null) {
                    verifyVisitorName.setText("Resident: " + resident.getFullName());
                    verifyDetails.setText("Unit: " + resident.getUnitNumber() + " | Status: " + resident.getStatus());
                    verifyResultBox.setVisible(true); verifyResultBox.setManaged(true);
                    registerEntryBtn.setText(resident.getStatus() == User.Status.INSIDE ?
                        "📤 Register Resident Exit" : "✅ Register Resident Entry");
                    scanResultLabel.setText("✅ Resident QR verified!");
                    scanResultLabel.getStyleClass().setAll("success-label");
                } else {
                    scanResultLabel.setText("❌ Resident not found."); scanResultLabel.getStyleClass().setAll("error-label");
                }
            }
        } else {
            // Try matching by raw QR code
            User u = userDAO.getUserByQRCode(qrData);
            if (u != null) {
                verifiedQrType = "RESIDENT"; verifiedQrData = qrData;
                verifyVisitorName.setText("Resident: " + u.getFullName());
                verifyDetails.setText("Unit: " + u.getUnitNumber() + " | Status: " + u.getStatus());
                verifyResultBox.setVisible(true); verifyResultBox.setManaged(true);
                registerEntryBtn.setText(u.getStatus() == User.Status.INSIDE ? "📤 Register Exit" : "✅ Register Entry");
                scanResultLabel.setText("✅ Resident matched!"); scanResultLabel.getStyleClass().setAll("success-label");
            } else {
                Approval apr = gateCtrl.getApprovalByQR(qrData);
                if (apr != null) {
                    verifiedQrType = "APPROVAL"; verifiedApprovalId = apr.getApprovalId();
                    verifyVisitorName.setText("Visitor: " + apr.getVisitorName());
                    verifyDetails.setText("Category: " + apr.getCategory() + " | Status: " + apr.getStatusString());
                    verifyResultBox.setVisible(true); verifyResultBox.setManaged(true);
                    registerEntryBtn.setText("✅ Register Entry");
                    scanResultLabel.setText("✅ Approval found!"); scanResultLabel.getStyleClass().setAll("success-label");
                } else {
                    scanResultLabel.setText("❌ Unknown QR code format.");
                    scanResultLabel.getStyleClass().setAll("error-label");
                    verifyResultBox.setVisible(false); verifyResultBox.setManaged(false);
                }
            }
        }
    }

    @FXML
    private void handleRegisterEntry() {
        if ("APPROVAL".equals(verifiedQrType) && verifiedApprovalId > 0) {
            EntryLog log = gateCtrl.registerVisitorEntry(verifiedApprovalId, session.getUserId());
            scanResultLabel.setText(log != null ? "✅ Visitor entry registered!" : "❌ Entry registration failed.");
            scanResultLabel.getStyleClass().setAll(log != null ? "success-label" : "error-label");
        } else if ("RESIDENT".equals(verifiedQrType)) {
            User r = userDAO.getUserByQRCode(verifiedQrData);
            if (r == null) { int rId = QRCodeService.getInstance().decodeResidentQR(verifiedQrData); r = userDAO.getUserById(rId); }
            if (r != null && r.getStatus() == User.Status.INSIDE) {
                EntryLog log = gateCtrl.registerResidentExit(verifiedQrData);
                scanResultLabel.setText(log != null ? "📤 Resident exit registered! Duration: " + log.getDurationFormatted() : "❌ Exit failed.");
            } else {
                EntryLog log = gateCtrl.registerResidentEntry(verifiedQrData, session.getUserId());
                scanResultLabel.setText(log != null ? "✅ Resident entry registered!" : "❌ Entry failed.");
            }
            scanResultLabel.getStyleClass().setAll(scanResultLabel.getText().contains("✅") || scanResultLabel.getText().contains("📤") ? "success-label" : "error-label");
        }
        verifyResultBox.setVisible(false); verifyResultBox.setManaged(false);
        loadActiveEntries(); updateOccupancy(); loadTodayApprovals();
    }

    @FXML
    private void handleRegisterExit() {
        EntryLog selected = activeEntriesTable.getSelectionModel().getSelectedItem();
        if (selected == null) { scanResultLabel.setText("Select an entry to register exit."); return; }
        EntryLog result = gateCtrl.registerExitByLogId(selected.getLogId());
        scanResultLabel.setText(result != null ? "📤 Exit registered. Duration: " + result.getDurationFormatted() : "❌ Exit failed.");
        scanResultLabel.getStyleClass().setAll(result != null ? "success-label" : "error-label");
        loadActiveEntries(); updateOccupancy();
    }

    @FXML
    private void handleWebcamScan() {
        String qr = QRCodeService.getInstance().scanQRFromWebcam();
        if (qr != null) { qrInputField.setText(qr); handleVerifyQR(); }
        else { scanResultLabel.setText("Webcam scan unavailable. Please enter QR code manually."); scanResultLabel.getStyleClass().setAll("warning-label"); }
    }

    @FXML
    private void handleWalkInRequest() {
        String name = walkInVisitorName.getText().trim();
        String purpose = walkInPurpose.getText().trim();
        String residentSelection = residentCombo.getValue();
        if (name.isEmpty() || residentSelection == null) { walkInStatusLabel.setText("Fill name and select resident."); return; }
        int residentId = Integer.parseInt(residentSelection.split(" - ")[0]);
        int reqId = approvalCtrl.createArrivalRequest(session.getUserId(), residentId, name, purpose);
        walkInStatusLabel.setText(reqId > 0 ? "✅ Request sent to resident!" : "❌ Failed.");
        walkInStatusLabel.getStyleClass().setAll(reqId > 0 ? "success-label" : "error-label");
        walkInVisitorName.clear(); walkInPurpose.clear();
    }

    @FXML public void loadActiveEntries() {
        List<EntryLog> entries = gateCtrl.getActiveEntries();
        activeEntriesTable.setItems(FXCollections.observableArrayList(entries));
    }

    @FXML public void loadTodayApprovals() {
        List<Approval> approvals = approvalCtrl.getTodayApprovals();
        todayApprovalsTable.setItems(FXCollections.observableArrayList(approvals));
    }

    private void updateOccupancy() { occupancyLabel.setText("Occupancy: " + gateCtrl.getOccupancyCount()); }

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
