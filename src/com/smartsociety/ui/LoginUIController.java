package com.smartsociety.ui;

import com.smartsociety.controller.LoginController;
import com.smartsociety.model.UserSession;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

/**
 * UI Controller for the Login screen.
 * Delegates authentication to LoginController (GRASP Controller).
 */
public class LoginUIController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;
    @FXML private Button loginButton;

    private final LoginController loginController = new LoginController();

    @FXML
    public void initialize() {
        // Allow Enter key to trigger login
        passwordField.setOnAction(e -> handleLogin());
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Please enter both username and password.");
            statusLabel.getStyleClass().setAll("error-label");
            return;
        }

        statusLabel.setText("Authenticating...");
        statusLabel.getStyleClass().setAll("label");

        UserSession session = loginController.login(username, password);
        if (session == null) {
            statusLabel.setText("Invalid username or password. Please try again.");
            statusLabel.getStyleClass().setAll("error-label");
            return;
        }

        // Navigate to role-specific dashboard
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

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Scene scene = new Scene(loader.load(), 1100, 700);
            scene.getStylesheets().add(getClass().getResource("/css/glassmorphism.css").toExternalForm());

            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setTitle("Smart Society - " + title);
            stage.setScene(scene);
            stage.centerOnScreen();

        } catch (Exception e) {
            statusLabel.setText("Error loading dashboard: " + e.getMessage());
            statusLabel.getStyleClass().setAll("error-label");
            e.printStackTrace();
        }
    }
}
