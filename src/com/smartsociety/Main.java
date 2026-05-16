package com.smartsociety;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.geometry.Rectangle2D;
import com.smartsociety.ui.AnimationUtils;
import javafx.scene.layout.StackPane;

/**
 * Main Application Entry Point
 * Smart Society Entry Management System
 * 
 * Architecture: 3-Tier Layered (JavaFX UI → Business Logic → JDBC DAO)
 * Design Patterns:
 *   GoF: Singleton (DatabaseConnection, QRCodeService), Factory (UserSessionFactory), Observer (NotificationService)
 *   GRASP: Controller, Information Expert, Creator
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent loginRoot = loader.load();

            AnimationUtils.sizeStageToScreen(primaryStage);
            StackPane appContainer = new StackPane();
            Scene scene = new Scene(appContainer, primaryStage.getWidth(), primaryStage.getHeight());
            scene.setCamera(new PerspectiveCamera());
            scene.getStylesheets().add(getClass().getResource("/css/glassmorphism.css").toExternalForm());

            StackPane loginView = AnimationUtils.createScaledContent(loginRoot, scene, 560, 660);
            appContainer.getChildren().setAll(loginView);

            primaryStage.setTitle("Smart Society Entry Management System");
            primaryStage.setScene(scene);
            AnimationUtils.applyFullScreen(primaryStage);
            primaryStage.show();

            System.out.println("==============================================");
            System.out.println("  Smart Society Entry Management System");
            System.out.println("  Version 1.0 - SDA Course Project");
            System.out.println("==============================================");

        } catch (Exception e) {
            System.err.println("Failed to load application: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        // Clean up database connection on exit
        try {
            com.smartsociety.config.DatabaseConnection.getInstance().closeConnection();
            System.out.println("Application closed. Database connection released.");
        } catch (Exception e) {
            // ignore on shutdown
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
