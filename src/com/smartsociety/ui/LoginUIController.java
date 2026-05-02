package com.smartsociety.ui;

import com.smartsociety.controller.LoginController;
import com.smartsociety.model.UserSession;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class LoginUIController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;
    @FXML private Button loginButton;
    @FXML private Button exitButton;
    @FXML private VBox loginCard;
    @FXML private Pane ambientLayer;

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

        try {
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

            Stage stage = (Stage) loginButton.getScene().getWindow();
            Scene scene = loginButton.getScene();
            StackPane host = (StackPane) scene.getRoot();
            final String fp = fxmlPath;
            final String t = title;

            javafx.scene.Node currentView = host.getChildren().isEmpty()
                ? loginButton.getScene().getRoot()
                : host.getChildren().get(0);

            AnimationUtils.sceneTransition(currentView, () -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource(fp));
                    javafx.scene.Parent root = loader.load();
                    StackPane nextView = AnimationUtils.createScaledContent(root, scene, 1280, 780);
                    nextView.setOpacity(0.0);
                    stage.setTitle("Smart Society - " + t);
                    AnimationUtils.applyFullScreen(stage);
                    host.getChildren().setAll(nextView);
                    AnimationUtils.fadeIn(nextView, 260);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

        } catch (Exception ex) {
            statusLabel.setText("Error loading dashboard: " + ex.getMessage());
            statusLabel.getStyleClass().setAll("error-label");
            ex.printStackTrace();
            setLoginEnabled(true);
        }
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
}
