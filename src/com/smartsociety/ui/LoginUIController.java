package com.smartsociety.ui;

import com.smartsociety.controller.LoginController;
import com.smartsociety.model.UserSession;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class LoginUIController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;
    @FXML private Button loginButton;
    @FXML private VBox loginCard;

    private final LoginController loginController = new LoginController();

    @FXML
    public void initialize() {
        passwordField.setOnAction(e -> handleLogin());
        AnimationUtils.introAnimation(loginCard);
        AnimationUtils.addHoverScale(loginButton);
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

        statusLabel.setText("Authenticating...");
        statusLabel.getStyleClass().setAll("label");

        UserSession session = loginController.login(username, password);
        if (session == null) {
            AnimationUtils.showAutoFadeStatus(statusLabel, "Invalid username or password.", "error-label");
            AnimationUtils.shakeNode(loginCard);
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
                    return;
            }

            Stage stage = (Stage) loginButton.getScene().getWindow();
            final String fp = fxmlPath;
            final String t = title;

            AnimationUtils.sceneTransition(loginButton.getScene().getRoot(), () -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource(fp));
                    Scene scene = new Scene(loader.load(), 1100, 700);
                    scene.getStylesheets().add(getClass().getResource("/css/glassmorphism.css").toExternalForm());
                    stage.setTitle("Smart Society - " + t);
                    stage.setScene(scene);
                    stage.centerOnScreen();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

        } catch (Exception e) {
            statusLabel.setText("Error loading dashboard: " + e.getMessage());
            statusLabel.getStyleClass().setAll("error-label");
            e.printStackTrace();
        }
    }
}
