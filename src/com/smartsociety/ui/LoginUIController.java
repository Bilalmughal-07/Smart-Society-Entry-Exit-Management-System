package com.smartsociety.ui;

import com.smartsociety.controller.LoginController;
import com.smartsociety.model.UserSession;
import com.smartsociety.ui.AdminDashboardController;
import com.smartsociety.ui.GuardDashboardController;
import com.smartsociety.ui.ResidentDashboardController;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.util.Duration;
import java.util.List;
import com.smartsociety.controller.AdminController;
import com.smartsociety.controller.ApprovalController;
import com.smartsociety.controller.GateController;
import com.smartsociety.dao.UserDAO;
import com.smartsociety.service.NotificationService;
import com.smartsociety.model.AccessRule;
import com.smartsociety.model.EntryLog;
import com.smartsociety.model.Violation;
import com.smartsociety.model.Approval;
import com.smartsociety.model.User;

public class LoginUIController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;
    @FXML private Button loginButton;
    @FXML private Button exitButton;
    @FXML private StackPane loadingOverlay;
    @FXML private Label loadingDots;
    @FXML private VBox loginCard;
    @FXML private Pane ambientLayer;

    private Timeline loadingTimeline;

    private final LoginController loginController = new LoginController();

    @FXML
    public void initialize() {
        passwordField.setOnAction(e -> handleLogin());
        AnimationUtils.installAmbientMotion(ambientLayer);
        AnimationUtils.introAnimation(loginCard);
        AnimationUtils.addHoverLift(loginButton);
        AnimationUtils.addHoverLift(exitButton);
    }

    @FXML
    private void handleExitApp() {
        Platform.exit();
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            AnimationUtils.showAutoFadeStatus(statusLabel, "Please enter both username and password.", "error-label");
            AnimationUtils.shakeNode(loginCard);
            return;
        }

        setLoginEnabled(false);
        statusLabel.setText("Authenticating...");
        statusLabel.getStyleClass().setAll("label");

        UserSession session = loginController.login(username, password);
        if (session == null) {
            AnimationUtils.showAutoFadeStatus(statusLabel, "Invalid username or password.", "error-label");
            AnimationUtils.shakeNode(loginCard);
            setLoginEnabled(true);
            return;
        }

        String fxmlPath;
        String title;
        switch (session.getRole()) {
            case ADMIN:
                fxmlPath = "/fxml/admin_dashboard.fxml";
                title = "Admin Dashboard";
                break;
            case GUARD:
                fxmlPath = "/fxml/guard_dashboard.fxml";
                title = "Guard Dashboard";
                break;
            case RESIDENT:
                fxmlPath = "/fxml/resident_dashboard.fxml";
                title = "Resident Dashboard";
                break;
            default:
                statusLabel.setText("Unknown role.");
                setLoginEnabled(true);
                return;
        }

        startLoadingOverlay();

        Task<Object> preloadTask = new Task<Object>() {
            @Override
            protected Object call() {
                switch (session.getRole()) {
                    case ADMIN:
                        return preloadAdmin();
                    case GUARD:
                        return preloadGuard(session.getUserId());
                    case RESIDENT:
                        return preloadResident(session.getUserId());
                    default:
                        return null;
                }
            }
        };

        preloadTask.setOnSucceeded(e -> {
            try {
                Stage stage = (Stage) loginButton.getScene().getWindow();
                Scene scene = loginButton.getScene();
                StackPane host = (StackPane) scene.getRoot();
                Object preload = preloadTask.getValue();
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                    if (preload instanceof AdminDashboardController.PreloadData) {
                        AdminDashboardController.setPreloadedData((AdminDashboardController.PreloadData) preload);
                    } else if (preload instanceof GuardDashboardController.PreloadData) {
                        GuardDashboardController.setPreloadedData((GuardDashboardController.PreloadData) preload);
                    } else if (preload instanceof ResidentDashboardController.PreloadData) {
                        ResidentDashboardController.setPreloadedData((ResidentDashboardController.PreloadData) preload);
                    }

                    javafx.scene.Parent root = loader.load();
                    StackPane nextView = AnimationUtils.createScaledContent(root, scene, 1280, 780);
                    nextView.setOpacity(0.0);
                    stage.setTitle("Smart Society - " + title);
                    AnimationUtils.applyFullScreen(stage);
                    crossfadeFromLoading(host, nextView);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    stopLoadingOverlay(() -> {
                        restoreLoginCard();
                        setLoginEnabled(true);
                    });
                }
            } finally {
                // handled by stopLoadingOverlay
            }
        });

        preloadTask.setOnFailed(e -> {
            Throwable ex = preloadTask.getException();
            statusLabel.setText("Loading failed: " + (ex != null ? ex.getMessage() : "Unknown error"));
            statusLabel.getStyleClass().setAll("error-label");
            if (ex != null) ex.printStackTrace();
            stopLoadingOverlay(() -> restoreLoginCard());
            setLoginEnabled(true);
        });

        Thread preloadThread = new Thread(preloadTask, "preload-dashboard");
        preloadThread.setDaemon(true);
        preloadThread.start();
    }

    private void setLoginEnabled(boolean enabled) {
        loginButton.setDisable(!enabled);
        usernameField.setDisable(!enabled);
        passwordField.setDisable(!enabled);
        if (enabled) {
            Platform.runLater(() -> {
                usernameField.requestFocus();
                usernameField.selectAll();
            });
        }
    }

    private void startLoadingOverlay() {
        loadingOverlay.setOpacity(0.0);
        loadingOverlay.setVisible(true);
        loadingOverlay.setManaged(true);
        startLoadingDots();
        AnimationUtils.fadeOut(loginCard, 320, () -> {
            loginCard.setVisible(false);
            loginCard.setManaged(false);
            AnimationUtils.fadeIn(loadingOverlay, 320);
        });
    }

    private void stopLoadingOverlay(Runnable after) {
        AnimationUtils.fadeOut(loadingOverlay, 300, () -> {
            loadingOverlay.setVisible(false);
            loadingOverlay.setManaged(false);
            stopLoadingDots();
            if (after != null) {
                after.run();
            }
        });
    }

    private void restoreLoginCard() {
        loginCard.setVisible(true);
        loginCard.setManaged(true);
        AnimationUtils.fadeIn(loginCard, 320);
    }

    private void crossfadeFromLoading(StackPane host, StackPane nextView) {
        stopLoadingDots();
        javafx.scene.Node currentView = host.getChildren().isEmpty()
            ? null
            : host.getChildren().get(0);
        nextView.setOpacity(0.0);
        if (currentView == null) {
            host.getChildren().setAll(nextView);
            AnimationUtils.fadeIn(nextView, 260);
            return;
        }
        host.getChildren().setAll(currentView, nextView);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(380), currentView);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(380), nextView);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        ParallelTransition crossfade = new ParallelTransition(fadeOut, fadeIn);
        crossfade.setOnFinished(e -> host.getChildren().setAll(nextView));
        crossfade.play();
    }

    private void startLoadingDots() {
        stopLoadingDots();
        loadingDots.setText(".");
        loadingTimeline = new Timeline(
            new KeyFrame(Duration.millis(350), e -> {
                String text = loadingDots.getText();
                loadingDots.setText(text.length() >= 3 ? "." : text + ".");
            })
        );
        loadingTimeline.setCycleCount(Timeline.INDEFINITE);
        loadingTimeline.play();
    }

    private void stopLoadingDots() {
        if (loadingTimeline != null) {
            loadingTimeline.stop();
            loadingTimeline = null;
        }
    }

    private AdminDashboardController.PreloadData preloadAdmin() {
        AdminController adminCtrl = new AdminController();
        List<AccessRule> rules = adminCtrl.getExistingRules();
        List<EntryLog> entries = adminCtrl.getActiveEntries();
        List<EntryLog> overstays = adminCtrl.detectOverstays();
        List<Violation> violations = adminCtrl.getAllViolations();
        return new AdminDashboardController.PreloadData(rules, entries, overstays, violations);
    }

    private GuardDashboardController.PreloadData preloadGuard(int userId) {
        GateController gateCtrl = new GateController();
        ApprovalController approvalCtrl = new ApprovalController();
        NotificationService notifService = NotificationService.getInstance();
        UserDAO userDAO = new UserDAO();
        List<User> residents = userDAO.getAllResidents();
        List<EntryLog> entries = gateCtrl.getActiveEntries();
        List<Approval> approvals = approvalCtrl.getTodayApprovals();
        List<String[]> notifications = notifService.getUnreadNotifications(userId);
        int occupancyCount = gateCtrl.getOccupancyCount();
        return new GuardDashboardController.PreloadData(residents, entries, approvals, notifications, occupancyCount);
    }

    private ResidentDashboardController.PreloadData preloadResident(int userId) {
        ApprovalController approvalCtrl = new ApprovalController();
        NotificationService notifService = NotificationService.getInstance();
        List<Approval> approvals = approvalCtrl.getApprovalsByResident(userId);
        String[][] requests = approvalCtrl.getPendingArrivalRequests(userId);
        List<String[]> notifications = notifService.getUnreadNotifications(userId);
        List<AccessRule> rules = approvalCtrl.getActiveRules();
        return new ResidentDashboardController.PreloadData(approvals, requests, notifications, rules);
    }
}
