package com.smartsociety.ui;

import com.smartsociety.controller.ApprovalController;
import com.smartsociety.controller.GateController;
import com.smartsociety.controller.LoginController;
import com.smartsociety.dao.UserDAO;
import com.smartsociety.model.*;
import com.smartsociety.service.NotificationService;
import com.smartsociety.service.QRCodeService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.concurrent.Task;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.scene.shape.Rectangle;
import javafx.stage.Screen;
import javafx.geometry.Rectangle2D;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.util.Duration;
import java.util.List;
import java.awt.image.BufferedImage;

public class GuardDashboardController {

    @FXML private Label welcomeLabel, avatarLabel, occupancyLabel, scanResultLabel;
    @FXML private Label verifyVisitorName, verifyDetails, walkInStatusLabel;
    @FXML private TextField qrInputField, walkInVisitorName, walkInVisitorContact, walkInPurpose;
    @FXML private VBox verifyResultBox;
    @FXML private Button registerEntryBtn, reportViolationBtn;

    @FXML private VBox cameraUiBox;
    @FXML private StackPane manualInputBox;
    @FXML private ImageView cameraFeedView;
    @FXML private Rectangle cameraFrame;
    @FXML private StackPane cameraLoadingLayer;
    @FXML private ProgressIndicator cameraLoadingSpinner;
    @FXML private Label instructionLabel, timerLabel, warningLabel;
    @FXML private Button retryBtn;

    @FXML private ComboBox<String> residentCombo;
    @FXML private ListView<String> guardAlertsList;

    @FXML private TableView<EntryLog> activeEntriesTable;
    @FXML private TableColumn<EntryLog, String> colName, colType, colCat, colEntry, colDuration, colLogStatus;

    @FXML private TableView<Approval> todayApprovalsTable;
    @FXML private TableColumn<Approval, String> colTVisitor, colTResident, colTCategory, colTTime, colTStatus;

    // Sidebar nav
    @FXML private VBox section0, section1, section2, section3;
    @FXML private Button navBtn0, navBtn1, navBtn2, navBtn3;
    @FXML private Pane ambientLayer;
    private int currentSection = 0;

    private Task<String> webcamTask;
    private final GateController gateCtrl = new GateController();
    private final ApprovalController approvalCtrl = new ApprovalController();
    private final NotificationService notificationService = NotificationService.getInstance();
    private final UserDAO userDAO = new UserDAO();
    private UserSession session;
    private int verifiedApprovalId = -1;
    private String verifiedQrData = null;
    private String verifiedQrType = null;

    private double cameraMaxW = 460;
    private double cameraMaxH = 340;

    public static final class PreloadData {
        private final List<User> residents;
        private final List<EntryLog> entries;
        private final List<Approval> approvals;
        private final List<String[]> notifications;
        private final int occupancyCount;

        public PreloadData(List<User> residents, List<EntryLog> entries, List<Approval> approvals,
                           List<String[]> notifications, int occupancyCount) {
            this.residents = residents;
            this.entries = entries;
            this.approvals = approvals;
            this.notifications = notifications;
            this.occupancyCount = occupancyCount;
        }
    }

    private static PreloadData preloadCache;

    public static void setPreloadedData(PreloadData data) {
        preloadCache = data;
    }

    @FXML
    public void initialize() {
        session = LoginController.getCurrentSession();
        if (session != null) {
            welcomeLabel.setText(session.getFullName());
            avatarLabel.setText(String.valueOf(session.getFullName().charAt(0)).toUpperCase());
        }

        colName.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(
            cd.getValue().getPersonName() != null ? cd.getValue().getPersonName() : "ID:" + cd.getValue().getPersonId()));
        colType.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getPersonType().name()));
        colCat.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getCategory()));
        colEntry.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(
            cd.getValue().getEntryTimestamp() != null ? cd.getValue().getEntryTimestamp().toString().replace("T", " ") : ""));
        colDuration.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getDurationFormatted()));
        colLogStatus.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getStatusString()));

        colTVisitor.setCellValueFactory(new PropertyValueFactory<>("visitorName"));
        colTResident.setCellValueFactory(new PropertyValueFactory<>("residentName"));
        colTCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colTTime.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getTimeWindow()));
        colTStatus.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getStatusString()));

        PreloadData preload = preloadCache;
        preloadCache = null;
        if (preload != null) {
            applyPreloadedData(preload);
        } else {
            loadResidents();
            loadActiveEntries();
            loadTodayApprovals();
            loadGuardAlerts();
            updateOccupancy();
        }
        handleEnterManually();

        DashboardUiUtils.useConstrainedTableColumns(activeEntriesTable, todayApprovalsTable);
        DashboardUiUtils.initializeSidebar(new Button[] { navBtn0, navBtn1, navBtn2, navBtn3 }, currentSection);

        AnimationUtils.installAmbientMotion(ambientLayer);
        Button[] animatedButtons = { navBtn0, navBtn1, navBtn2, navBtn3, registerEntryBtn, reportViolationBtn, retryBtn };
        for (Button button : animatedButtons) if (button != null) AnimationUtils.addHoverLift(button);
        AnimationUtils.introAnimation(section0);

        cameraFrame.widthProperty().bind(cameraFeedView.fitWidthProperty());
        cameraFrame.heightProperty().bind(cameraFeedView.fitHeightProperty());
        cameraLoadingLayer.prefWidthProperty().bind(cameraFrame.widthProperty());
        cameraLoadingLayer.prefHeightProperty().bind(cameraFrame.heightProperty());
        cameraLoadingLayer.minWidthProperty().bind(cameraFrame.widthProperty());
        cameraLoadingLayer.minHeightProperty().bind(cameraFrame.heightProperty());
        cameraLoadingLayer.maxWidthProperty().bind(cameraFrame.widthProperty());
        cameraLoadingLayer.maxHeightProperty().bind(cameraFrame.heightProperty());
        applyCameraSizingForScreen();
        manualInputBox.maxWidthProperty().bind(section0.widthProperty().multiply(0.35));
        manualInputBox.prefWidthProperty().bind(section0.widthProperty().multiply(0.35));
        showCameraLoading(false);
    }

    private void applyPreloadedData(PreloadData preload) {
        residentCombo.getItems().clear();
        for (User r : preload.residents) {
            residentCombo.getItems().add(r.getUserId() + " - " + r.getFullName() + " (" + r.getUnitNumber() + ")");
        }
        activeEntriesTable.setItems(FXCollections.observableArrayList(preload.entries));
        todayApprovalsTable.setItems(FXCollections.observableArrayList(preload.approvals));
        javafx.collections.ObservableList<String> items = FXCollections.observableArrayList();
        for (String[] notification : preload.notifications) {
            if (notification.length > 2 && "GUARD_ALERT".equals(notification[2])
                    && notification[1].startsWith("Arrival request")) {
                items.add(notification[1] + "  (" + notification[3] + ")");
            }
        }
        guardAlertsList.setItems(items);
        guardAlertsList.setPlaceholder(new Label("No new resident responses."));
        occupancyLabel.setText("Occupancy: " + preload.occupancyCount);
    }

    // === Sidebar Navigation ===

    @FXML private void showSection0() { showSection(0); }
    @FXML private void showSection1() { showSection(1); }
    @FXML private void showSection2() { showSection(2); }
    @FXML private void showSection3() { showSection(3); }

    private void showSection(int index) {
        if (index == currentSection) return;
        VBox[] sections = { section0, section1, section2, section3 };
        AnimationUtils.switchSection(sections[currentSection], sections[index],
                index > currentSection ? 1 : -1);
        activateNav(index);
        currentSection = index;
    }

    private void activateNav(int index) {
        DashboardUiUtils.activateSidebarButton(new Button[] { navBtn0, navBtn1, navBtn2, navBtn3 }, index);
    }

    @FXML
    private void handleVerifyQR() {
        String qrData = qrInputField.getText().trim();
        if (qrData.isEmpty()) { scanResultLabel.setText("Enter QR code data."); return; }

        QRCodeService qrService = QRCodeService.getInstance();
        String qrType = qrService.getQRType(qrData);
        verifiedQrData = qrData;
        verifiedQrType = qrType;
        verifiedApprovalId = -1;
        reportViolationBtn.setVisible(false);
        reportViolationBtn.setManaged(false);

        if ("APPROVAL".equals(qrType)) {
            String result = gateCtrl.verifyVisitorApproval(qrData);
            if (result.startsWith("VALID:")) {
                verifiedApprovalId = Integer.parseInt(result.split(":")[1]);
                Approval a = approvalCtrl.getApprovalById(verifiedApprovalId);
                verifyVisitorName.setText("Visitor: " + a.getVisitorName());
                verifyDetails.setText("Category: " + a.getCategory() + " | Time: " + a.getTimeWindow() +
                    "\nResident: " + a.getResidentName() + " | Unit: " + a.getResidentUnit());
                verifyResultBox.setVisible(true); verifyResultBox.setManaged(true);
                registerEntryBtn.setText("Register Visitor Entry");
                reportViolationBtn.setVisible(false); reportViolationBtn.setManaged(false);
                scanResultLabel.setText("Visitor approval verified!");
                scanResultLabel.getStyleClass().setAll("success-label");
            } else if (result.startsWith("STATUS_ENTERED:")) {
                verifiedApprovalId = Integer.parseInt(result.split(":")[1]);
                Approval a = approvalCtrl.getApprovalById(verifiedApprovalId);
                verifyVisitorName.setText("Visitor: " + a.getVisitorName() + " (ALREADY INSIDE)");
                verifyDetails.setText("Category: " + a.getCategory() + " | Time: " + a.getTimeWindow() +
                    "\nResident: " + a.getResidentName() + " | Unit: " + a.getResidentUnit());
                verifyResultBox.setVisible(true); verifyResultBox.setManaged(true);
                registerEntryBtn.setText("Register Exit");
                reportViolationBtn.setVisible(true); reportViolationBtn.setManaged(true);
                scanResultLabel.setText("Visitor already inside. Register exit or report QR sharing.");
                scanResultLabel.getStyleClass().setAll("warning-label");
            } else {
                verifyResultBox.setVisible(false); verifyResultBox.setManaged(false);
                scanResultLabel.setText("Verification failed: " + result);
                scanResultLabel.getStyleClass().setAll("error-label");
            }
        } else if ("RESIDENT".equals(qrType)) {
            User resident = gateCtrl.getResidentByQR(qrData);
            if (resident != null) {
                showResidentVerification(resident);
            } else {
                scanResultLabel.setText("Resident not found.");
                scanResultLabel.getStyleClass().setAll("error-label");
                verifyResultBox.setVisible(false); verifyResultBox.setManaged(false);
            }
        } else {
            User u = gateCtrl.getResidentByQR(qrData);
            if (u != null) {
                verifiedQrType = "RESIDENT";
                showResidentVerification(u);
            } else {
                Approval apr = gateCtrl.getApprovalByQR(qrData);
                if (apr != null) {
                    verifiedQrType = "APPROVAL"; verifiedApprovalId = apr.getApprovalId();
                    verifyVisitorName.setText("Visitor: " + apr.getVisitorName());
                    verifyDetails.setText("Category: " + apr.getCategory() + " | Status: " + apr.getStatusString());
                    verifyResultBox.setVisible(true); verifyResultBox.setManaged(true);
                    registerEntryBtn.setText("Register Entry");
                    scanResultLabel.setText("Approval found!"); scanResultLabel.getStyleClass().setAll("success-label");
                } else {
                    scanResultLabel.setText("Unknown QR code format.");
                    scanResultLabel.getStyleClass().setAll("error-label");
                    verifyResultBox.setVisible(false); verifyResultBox.setManaged(false);
                }
            }
        }
    }

    private void showResidentVerification(User resident) {
        boolean alreadyInside = resident.getStatus() == User.Status.INSIDE;
        verifyVisitorName.setText("Resident: " + resident.getFullName() + (alreadyInside ? " (ALREADY INSIDE)" : ""));
        verifyDetails.setText("Unit: " + resident.getUnitNumber() + " | Status: " + resident.getStatus());
        verifyResultBox.setVisible(true); verifyResultBox.setManaged(true);
        registerEntryBtn.setText(alreadyInside ? "Register Resident Exit" : "Register Resident Entry");
        reportViolationBtn.setVisible(alreadyInside); reportViolationBtn.setManaged(alreadyInside);
        scanResultLabel.setText(alreadyInside
                ? "Resident already inside. Register exit or report QR sharing."
                : "Resident QR verified!");
        scanResultLabel.getStyleClass().setAll(alreadyInside ? "warning-label" : "success-label");
    }

    @FXML
    private void handleRegisterEntry() {
        if ("APPROVAL".equals(verifiedQrType) && verifiedApprovalId > 0) {
            if (registerEntryBtn.getText().contains("Exit")) {
                EntryLog result = gateCtrl.registerVisitorExit(verifiedApprovalId);
                scanResultLabel.setText(result != null ? "Exit registered. Duration: " + result.getDurationFormatted() : "Exit failed.");
                scanResultLabel.getStyleClass().setAll(result != null ? "success-label" : "error-label");
            } else {
                EntryLog log = gateCtrl.registerVisitorEntry(verifiedApprovalId, session.getUserId());
                scanResultLabel.setText(log != null ? "Visitor entry registered!" : "Entry registration failed.");
                scanResultLabel.getStyleClass().setAll(log != null ? "success-label" : "error-label");
            }
        } else if ("RESIDENT".equals(verifiedQrType)) {
            User r = gateCtrl.getResidentByQR(verifiedQrData);
            if (r != null && registerEntryBtn.getText().contains("Exit")) {
                EntryLog log = gateCtrl.registerResidentExit(verifiedQrData);
                scanResultLabel.setText(log != null ? "Resident exit registered! Duration: " + log.getDurationFormatted() : "Exit failed.");
            } else {
                EntryLog log = gateCtrl.registerResidentEntry(verifiedQrData, session.getUserId());
                scanResultLabel.setText(log != null ? "Resident entry registered!" : "Entry failed.");
            }
            scanResultLabel.getStyleClass().setAll(scanResultLabel.getText().contains("registered") ? "success-label" : "error-label");
        }
        verifyResultBox.setVisible(false); verifyResultBox.setManaged(false);
        loadActiveEntries(); updateOccupancy(); loadTodayApprovals();
    }

    @FXML
    private void handleReportViolation() {
        if ("APPROVAL".equals(verifiedQrType) && verifiedApprovalId > 0) {
            boolean success = gateCtrl.reportQRSharingViolation(verifiedApprovalId, session.getUserId());
            scanResultLabel.setText(success ? "QR Sharing Violation reported!" : "Failed to report violation.");
            scanResultLabel.getStyleClass().setAll(success ? "success-label" : "error-label");
            verifyResultBox.setVisible(false); verifyResultBox.setManaged(false);
            loadActiveEntries(); updateOccupancy(); loadTodayApprovals();
        } else if ("RESIDENT".equals(verifiedQrType)) {
            boolean success = gateCtrl.reportResidentQRSharingViolation(verifiedQrData, session.getUserId());
            scanResultLabel.setText(success ? "Resident QR sharing violation reported!" : "Failed to report violation.");
            scanResultLabel.getStyleClass().setAll(success ? "success-label" : "error-label");
            verifyResultBox.setVisible(false); verifyResultBox.setManaged(false);
            loadActiveEntries(); updateOccupancy(); loadTodayApprovals();
        }
    }

    private void startWebcamScan() {
        if (manualInputBox.isVisible()) {
            animateSwap(manualInputBox, cameraUiBox);
        }
        warningLabel.setVisible(false); warningLabel.setManaged(false);
        retryBtn.setVisible(false); retryBtn.setManaged(false);
        timerLabel.setText("15s");
        instructionLabel.setText("Please hold up QR");
        showCameraLoading(true);
        cameraFeedView.setImage(null);

        if (webcamTask != null && webcamTask.isRunning()) webcamTask.cancel();

        webcamTask = new Task<String>() {
            @Override
            protected String call() throws Exception {
                long start = System.currentTimeMillis();
                long timeout = 15000;
                QRCodeService qrService = QRCodeService.getInstance();
                while (System.currentTimeMillis() - start < timeout) {
                    if (isCancelled()) return null;
                    long elapsed = System.currentTimeMillis() - start;
                    int rem = (int) ((timeout - elapsed) / 1000);
                    Platform.runLater(() -> timerLabel.setText(rem + "s"));
                    BufferedImage frame = qrService.getWebcamFrame();
                    if (frame != null) {
                        try {
                            Image fxImage = javafx.embed.swing.SwingFXUtils.toFXImage(frame, null);
                            Platform.runLater(() -> {
                                updateCameraViewSize(fxImage);
                                cameraFeedView.setImage(fxImage);
                                showCameraLoading(false);
                            });
                        } catch (Exception ignored) {}
                        String result = qrService.decodeImage(frame);
                        if (result != null) return result;
                    }
                    Thread.sleep(100);
                }
                return null;
            }
        };

        webcamTask.setOnSucceeded(e -> {
            QRCodeService.getInstance().closeWebcam();
            String result = webcamTask.getValue();
            if (result != null) {
                qrInputField.setText(result);
                handleEnterManually();
                handleVerifyQR();
            } else {
                showWarning();
            }
        });
        webcamTask.setOnCancelled(e -> { QRCodeService.getInstance().closeWebcam(); cameraFeedView.setImage(null); showCameraLoading(false); });
        webcamTask.setOnFailed(e -> { QRCodeService.getInstance().closeWebcam(); cameraFeedView.setImage(null); showCameraLoading(false); });

        Thread thread = new Thread(webcamTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void showWarning() {
        instructionLabel.setText("");
        timerLabel.setText("0s");
        warningLabel.setVisible(true); warningLabel.setManaged(true);
        retryBtn.setVisible(true); retryBtn.setManaged(true);
        showCameraLoading(false);
        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(
            javafx.util.Duration.millis(400), warningLabel);
        ft.setFromValue(0.0); ft.setToValue(1.0); ft.play();
    }

    private void showCameraLoading(boolean show) {
        cameraLoadingLayer.setVisible(show);
        cameraLoadingLayer.setManaged(show);
        cameraLoadingLayer.setOpacity(show ? 1.0 : 0.0);
    }

    private void animateSwap(javafx.scene.Node from, javafx.scene.Node to) {
        if (from == null || to == null) return;
        if (!from.isVisible() && to.isVisible()) return;

        FadeTransition outFade = new FadeTransition(Duration.millis(180), from);
        outFade.setFromValue(1.0);
        outFade.setToValue(0.0);
        TranslateTransition outSlide = new TranslateTransition(Duration.millis(180), from);
        outSlide.setFromY(0);
        outSlide.setToY(-8);

        ParallelTransition out = new ParallelTransition(outFade, outSlide);
        out.setOnFinished(e -> {
            from.setVisible(false);
            from.setManaged(false);
            from.setOpacity(1.0);
            from.setTranslateY(0);

            to.setOpacity(0.0);
            to.setTranslateY(8);
            to.setVisible(true);
            to.setManaged(true);

            FadeTransition inFade = new FadeTransition(Duration.millis(220), to);
            inFade.setFromValue(0.0);
            inFade.setToValue(1.0);
            TranslateTransition inSlide = new TranslateTransition(Duration.millis(220), to);
            inSlide.setFromY(8);
            inSlide.setToY(0);
            new ParallelTransition(inFade, inSlide).play();
        });
        out.play();
    }

    private void updateCameraViewSize(Image image) {
        if (image == null) return;
        double imgW = image.getWidth();
        double imgH = image.getHeight();
        if (imgW <= 0 || imgH <= 0) return;

        double scale = Math.min(cameraMaxW / imgW, cameraMaxH / imgH);
        cameraFeedView.setFitWidth(imgW * scale);
        cameraFeedView.setFitHeight(imgH * scale);
    }

    private void applyCameraSizingForScreen() {
        Rectangle2D bounds = Screen.getPrimary().getBounds();
        double base = Math.min(bounds.getWidth(), bounds.getHeight());
        cameraMaxW = Math.max(520, base * 0.42);
        cameraMaxH = Math.max(390, base * 0.32);
        cameraFeedView.setFitWidth(cameraMaxW);
        cameraFeedView.setFitHeight(cameraMaxH);
    }

    @FXML
    private void handleEnterManually() {
        if (webcamTask != null && webcamTask.isRunning()) webcamTask.cancel();
        showCameraLoading(false);
        if (cameraUiBox.isVisible()) {
            animateSwap(cameraUiBox, manualInputBox);
        }
    }

    @FXML private void handleRetryScan() { startWebcamScan(); }
    @FXML private void handleStartWebcam() { startWebcamScan(); }

    @FXML
    private void handleWalkInRequest() {
        String name = walkInVisitorName.getText().trim();
        String contact = walkInVisitorContact.getText().trim();
        String purpose = walkInPurpose.getText().trim();
        String residentSelection = residentCombo.getValue();
        if (name.isEmpty() || contact.isEmpty() || residentSelection == null) {
            walkInStatusLabel.setText("Fill name, contact number and select resident."); return;
        }
        int residentId = Integer.parseInt(residentSelection.split(" - ")[0]);
        int reqId = approvalCtrl.createArrivalRequest(session.getUserId(), residentId, name, contact, purpose);
        walkInStatusLabel.setText(reqId > 0 ? "Request sent to resident!" : "Failed.");
        walkInStatusLabel.getStyleClass().setAll(reqId > 0 ? "success-label" : "error-label");
        walkInVisitorName.clear(); walkInVisitorContact.clear(); walkInPurpose.clear();
        loadGuardAlerts();
    }

    @FXML public void loadActiveEntries() {
        List<EntryLog> entries = gateCtrl.getActiveEntries();
        activeEntriesTable.setItems(FXCollections.observableArrayList(entries));
    }

    @FXML public void loadTodayApprovals() {
        List<Approval> approvals = approvalCtrl.getTodayApprovals();
        todayApprovalsTable.setItems(FXCollections.observableArrayList(approvals));
    }

    @FXML public void loadGuardAlerts() {
        List<String[]> notifications = notificationService.getUnreadNotifications(session.getUserId());
        javafx.collections.ObservableList<String> items = FXCollections.observableArrayList();
        for (String[] notification : notifications) {
            if (notification.length > 2 && "GUARD_ALERT".equals(notification[2])
                    && notification[1].startsWith("Arrival request")) {
                items.add(notification[1] + "  (" + notification[3] + ")");
            }
        }
        guardAlertsList.setItems(items);
        guardAlertsList.setPlaceholder(new Label("No new resident responses."));
    }

    private void updateOccupancy() {
        occupancyLabel.setText("Occupancy: " + gateCtrl.getOccupancyCount());
    }

    @FXML
    private void handleExitApp() {
        Platform.exit();
    }

    private void loadResidents() {
        List<User> residents = userDAO.getAllResidents();
        residentCombo.getItems().clear();
        for (User r : residents) {
            residentCombo.getItems().add(r.getUserId() + " - " + r.getFullName() + " (" + r.getUnitNumber() + ")");
        }
    }

    @FXML
    private void handleLogout() {
        Stage stage = (Stage) welcomeLabel.getScene().getWindow();
        if (webcamTask != null && webcamTask.isRunning()) webcamTask.cancel();
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
}
