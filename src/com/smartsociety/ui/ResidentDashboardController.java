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
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import javafx.application.Platform;

public class ResidentDashboardController {

    @FXML private Label welcomeLabel, avatarLabel, approvalStatusLabel, qrCodeLabel;
    @FXML private TextField visitorNameField, visitorContactField, purposeField, durationField,
            timeHourField, timeMinField, timeEndField;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private DatePicker visitDatePicker;
    @FXML private VBox qrDisplayBox;
    @FXML private ImageView qrImageView;
    @FXML private Button notifBadge;

    @FXML private TableView<Approval> approvalsTable;
    @FXML private TableColumn<Approval, String> colVisitor, colCategory, colDate, colTime, colQR, colStatus;

    @FXML private TableView<String[]> arrivalRequestsTable;
    @FXML private TableColumn<String[], String> colArrVisitor, colArrContact, colArrPurpose, colArrGuard, colArrTime;

    @FXML private TableView<AccessRule> rulesTable;
    @FXML private TableColumn<AccessRule, String> colRuleName, colRuleCat, colRuleTime, colRuleDuration, colRuleVisitors;

    @FXML private ListView<String> notificationsList;

    // Sidebar nav — section0 is a ScrollPane, rest are VBox
    @FXML private ScrollPane section0;
    @FXML private VBox section1, section2, section3, section4;
    @FXML private Button navBtn0, navBtn1, navBtn2, navBtn3, navBtn4;
    @FXML private Pane ambientLayer;
    private int currentSection = 0;

    private final ApprovalController approvalCtrl = new ApprovalController();
    private final NotificationService notifService = NotificationService.getInstance();
    private UserSession session;
    private int pendingArrivalRequestId = -1;


    @FXML
    public void initialize() {
        session = LoginController.getCurrentSession();
        if (session != null) {
            welcomeLabel.setText(session.getFullName() + "\n" + session.getUnitNumber());
            avatarLabel.setText(String.valueOf(session.getFullName().charAt(0)).toUpperCase());
        }

        categoryCombo.setItems(FXCollections.observableArrayList("Guest", "Delivery", "Service", "Contractor", "Other"));
        visitDatePicker.setValue(LocalDate.now());

        colVisitor.setCellValueFactory(new PropertyValueFactory<>("visitorName"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colDate.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getVisitDate().toString()));
        colTime.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getTimeWindow()));
        colQR.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getQrCode() != null ? cd.getValue().getQrCode() : "—"));
        colStatus.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getStatusString()));

        colArrVisitor.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().length > 1 ? cd.getValue()[1] : ""));
        colArrContact.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().length > 2 ? cd.getValue()[2] : ""));
        colArrPurpose.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().length > 3 ? cd.getValue()[3] : ""));
        colArrGuard.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().length > 4 ? cd.getValue()[4] : ""));
        colArrTime.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().length > 5 ? cd.getValue()[5] : ""));

        colRuleName.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getRuleName()));
        colRuleCat.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getCategory()));
        colRuleTime.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getTimeWindow()));
        colRuleDuration.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getMaxDurationMinutes() + " min"));
        colRuleVisitors.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cd.getValue().getMaxVisitorsPerDay())));

        loadApprovals();
        loadArrivalRequests();
        loadNotifications();
        loadRules();

        DashboardUiUtils.useConstrainedTableColumns(approvalsTable, arrivalRequestsTable, rulesTable);
        DashboardUiUtils.initializeSidebar(new Button[] { navBtn0, navBtn1, navBtn2, navBtn3, navBtn4 }, currentSection);

        javafx.beans.value.ChangeListener<String> timeCalcListener = (obs, oldVal, newVal) -> updateEndTime();
        timeHourField.textProperty().addListener(timeCalcListener);
        timeMinField.textProperty().addListener(timeCalcListener);
        durationField.textProperty().addListener(timeCalcListener);

        timeHourField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused && !timeHourField.getText().isEmpty()) {
                try { timeHourField.setText(String.format("%02d", Integer.parseInt(timeHourField.getText().trim()))); }
                catch (NumberFormatException ignored) {}
            }
        });
        timeMinField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused && !timeMinField.getText().isEmpty()) {
                try { timeMinField.setText(String.format("%02d", Integer.parseInt(timeMinField.getText().trim()))); }
                catch (NumberFormatException ignored) {}
            }
        });

        AnimationUtils.installAmbientMotion(ambientLayer);
        Button[] animatedButtons = { navBtn0, navBtn1, navBtn2, navBtn3, navBtn4, notifBadge };
        for (Button button : animatedButtons) AnimationUtils.addHoverLift(button);
        AnimationUtils.introAnimation(section0);
    }

    // === Sidebar Navigation ===

    @FXML private void showSection0() { showSection(0); }
    @FXML private void showSection1() { showSection(1); }
    @FXML private void showSection2() { showSection(2); }
    @FXML private void showSection3() { showSection(3); }
    @FXML private void showSection4() { showSection(4); }

    private void showSection(int index) {
        if (index == currentSection) return;
        Node[] sections = { section0, section1, section2, section3, section4 };
        AnimationUtils.switchSection(sections[currentSection], sections[index],
                index > currentSection ? 1 : -1);
        activateNav(index);
        currentSection = index;
    }

    private void activateNav(int index) {
        DashboardUiUtils.activateSidebarButton(new Button[] { navBtn0, navBtn1, navBtn2, navBtn3, navBtn4 }, index);
    }

    // === Time calculation ===

    private void updateEndTime() {
        try {
            if (timeHourField.getText().trim().isEmpty() || timeMinField.getText().trim().isEmpty()
                    || durationField.getText().trim().isEmpty()) {
                timeEndField.setText("--:--"); return;
            }
            int h = Integer.parseInt(timeHourField.getText().trim());
            int m = Integer.parseInt(timeMinField.getText().trim());
            int dur = Integer.parseInt(durationField.getText().trim());
            timeEndField.setText(LocalTime.of(h, m).plusMinutes(dur).toString());
        } catch (Exception e) {
            timeEndField.setText("--:--");
        }
    }

    // === Approval Creation ===

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
                showStatus("Please fill all required fields (*)", "error-label"); return;
            }

            Approval approval = approvalCtrl.createApproval(session.getUserId(), name, contact, category, purpose, date, start, end, duration);
            if (approval != null) {
                boolean arrivalRequestCompleted = true;
                if (pendingArrivalRequestId > 0) {
                    arrivalRequestCompleted = approvalCtrl.completeArrivalApproval(pendingArrivalRequestId, approval);
                    pendingArrivalRequestId = -1;
                    loadArrivalRequests();
                }
                showStatus(arrivalRequestCompleted
                    ? "Approval created successfully!"
                    : "Approval created, but the arrival request could not be updated.",
                    arrivalRequestCompleted ? "success-label" : "warning-label");
                qrCodeLabel.setText(approval.getQrCode());
                javafx.scene.image.Image qrImg = com.smartsociety.service.QRCodeService.getInstance().generateQRImage(approval.getQrCode());
                if (qrImg != null) qrImageView.setImage(qrImg);
                qrDisplayBox.setVisible(true); qrDisplayBox.setManaged(true);
                AnimationUtils.fadeIn(qrDisplayBox, 300);
                loadApprovals();
                clearInputs();
            } else {
                showStatus("Failed to create approval.", "error-label");
            }
        } catch (NumberFormatException e) {
            showStatus("Invalid duration. Enter a number.", "error-label");
        } catch (IllegalArgumentException e) {
            showStatus(e.getMessage(), "error-label");
        } catch (DateTimeParseException e) {
            showStatus("Invalid time format. Use HH:MM (e.g. 09:00)", "error-label");
        }
    }

    @FXML
    private void handleCancelApproval() {
        Approval selected = approvalsTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showStatus("Select an approval to cancel.", "warning-label"); return; }
        boolean cancelled = approvalCtrl.cancelApproval(selected.getApprovalId());
        showStatus(cancelled ? "Approval cancelled." : "Cannot cancel this approval.",
                cancelled ? "success-label" : "error-label");
        loadApprovals();
    }

    @FXML private void handleClear() {
        pendingArrivalRequestId = -1;
        clearInputs();
        qrDisplayBox.setVisible(false); qrDisplayBox.setManaged(false); qrImageView.setImage(null);
    }

    private void clearInputs() {
        visitorNameField.clear(); visitorContactField.clear(); purposeField.clear();
        durationField.setText("60"); timeHourField.clear(); timeMinField.clear(); timeEndField.clear();
        categoryCombo.getSelectionModel().clearSelection();
    }

    // === Arrival Requests ===

    @FXML
    private void handleApproveArrival() {
        String[] selected = arrivalRequestsTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        pendingArrivalRequestId = Integer.parseInt(selected[0]);

        visitorNameField.setText(selected.length > 1 ? selected[1] : "");
        visitorContactField.setText(selected.length > 2 ? selected[2] : "");
        purposeField.setText(selected.length > 3 ? selected[3] : "");
        visitDatePicker.setValue(LocalDate.now());

        LocalTime now = LocalTime.now();
        timeHourField.setText(String.format("%02d", now.getHour()));
        timeMinField.setText(String.format("%02d", now.getMinute()));
        durationField.clear();
        timeEndField.setText("--:--");
        categoryCombo.getSelectionModel().clearSelection();
        qrDisplayBox.setVisible(false); qrDisplayBox.setManaged(false); qrImageView.setImage(null);

        showSection(0);
        showStatus("Walk-in visitor details loaded. Select a category and enter visit duration.", "warning-label");
    }

    @FXML
    private void handleRejectArrival() {
        String[] selected = arrivalRequestsTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        approvalCtrl.respondToArrivalRequest(Integer.parseInt(selected[0]), false);
        loadArrivalRequests();
    }

    // === Data Loading ===

    @FXML
    public void loadApprovals() {
        List<Approval> list = approvalCtrl.getApprovalsByResident(session.getUserId());
        approvalsTable.setItems(FXCollections.observableArrayList(list));
    }

    @FXML
    public void loadArrivalRequests() {
        String[][] requests = approvalCtrl.getPendingArrivalRequests(session.getUserId());
        arrivalRequestsTable.setItems(FXCollections.observableArrayList(requests));
    }

    @FXML
    public void loadNotifications() {
        List<String[]> notifs = notifService.getUnreadNotifications(session.getUserId());
        ObservableList<String> items = FXCollections.observableArrayList();
        for (String[] n : notifs) items.add("[" + n[2] + "] " + n[1] + "  (" + n[3] + ")");
        notificationsList.setItems(items);
        notifBadge.setText("🔔 " + notifs.size());
    }

    @FXML private void showNotifications() { showSection(3); }
    @FXML private void markAllRead() { notifService.markAllAsRead(session.getUserId()); loadNotifications(); }

    @FXML
    public void loadRules() {
        List<AccessRule> rules = approvalCtrl.getActiveRules();
        rulesTable.setItems(FXCollections.observableArrayList(rules));
    }

    @FXML
    private void handleExitApp() {
        Platform.exit();
    }

    @FXML
    private void handleLogout() {
        Stage stage = (Stage) welcomeLabel.getScene().getWindow();
        Scene scene = welcomeLabel.getScene();
        StackPane host = (StackPane) scene.getRoot();
        javafx.scene.Node currentView = host.getChildren().isEmpty() ? scene.getRoot() : host.getChildren().get(0);
        AnimationUtils.sceneTransition(currentView, () -> {
            try {
                new LoginController().logout();
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
                javafx.scene.Parent root = loader.load();
                StackPane nextView = AnimationUtils.createScaledContent(root, scene, 560, 660);
                nextView.setOpacity(0.0);
                stage.setTitle("Smart Society - Login");
                AnimationUtils.applyFullScreen(stage);
                host.getChildren().setAll(nextView);
                AnimationUtils.fadeIn(nextView, 260);
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    private void showStatus(String msg, String styleClass) {
        approvalStatusLabel.setText(msg);
        approvalStatusLabel.getStyleClass().setAll(styleClass);
    }
}
