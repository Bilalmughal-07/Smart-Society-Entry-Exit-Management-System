package com.smartsociety;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

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
            Scene scene = new Scene(loader.load(), 500, 600);
            scene.getStylesheets().add(getClass().getResource("/css/glassmorphism.css").toExternalForm());

            primaryStage.setTitle("Smart Society Entry Management System");
            primaryStage.setScene(scene);
            primaryStage.setResizable(true);
            primaryStage.setMinWidth(450);
            primaryStage.setMinHeight(500);
            primaryStage.centerOnScreen();
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
