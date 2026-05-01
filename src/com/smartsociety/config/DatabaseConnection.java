package com.smartsociety.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * GoF: Singleton Pattern
 * Thread-safe database connection manager for MS SQL Server.
 * Provides a single shared connection instance across the application.
 */
public class DatabaseConnection {

    private static volatile DatabaseConnection instance;
    private Connection connection;

    // Database configuration – modify these for your environment
    private static final String SERVER = "localhost";
    private static final String PORT = "1433";
    private static final String DATABASE = "SmartSocietyDB";
    private static final String USER = "sdaproject";
    private static final String PASSWORD = "sdaproject"; // Change this

    // JDBC URL for SQL Server
    private static final String URL = "jdbc:sqlserver://" + SERVER + ":" + PORT
            + ";databaseName=" + DATABASE
            + ";encrypt=true;trustServerCertificate=true";

    /**
     * Private constructor – prevents external instantiation.
     * Loads the JDBC driver and establishes the connection.
     */
    private DatabaseConnection() {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            this.connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("[DatabaseConnection] Connected to SQL Server successfully.");
        } catch (ClassNotFoundException e) {
            System.err.println("[DatabaseConnection] JDBC Driver not found. Ensure mssql-jdbc JAR is in classpath.");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("[DatabaseConnection] Failed to connect to database.");
            e.printStackTrace();
        }
    }

    /**
     * Returns the singleton instance using double-checked locking.
     * 
     * @return the DatabaseConnection singleton
     */
    public static DatabaseConnection getInstance() {
        if (instance == null) {
            synchronized (DatabaseConnection.class) {
                if (instance == null) {
                    instance = new DatabaseConnection();
                }
            }
        }
        return instance;
    }

    /**
     * Returns the active JDBC connection.
     * If the connection is closed or null, it re-establishes it.
     * 
     * @return a valid Connection object
     */
    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                synchronized (DatabaseConnection.class) {
                    if (connection == null || connection.isClosed()) {
                        connection = DriverManager.getConnection(URL, USER, PASSWORD);
                        System.out.println("[DatabaseConnection] Reconnected to SQL Server.");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[DatabaseConnection] Error reconnecting to database.");
            e.printStackTrace();
        }
        return connection;
    }

    /**
     * Closes the database connection gracefully.
     */
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("[DatabaseConnection] Connection closed.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
