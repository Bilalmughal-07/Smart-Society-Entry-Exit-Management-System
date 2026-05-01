package com.smartsociety.controller;

import com.smartsociety.dao.UserDAO;
import com.smartsociety.factory.UserSessionFactory;
import com.smartsociety.model.User;
import com.smartsociety.model.UserSession;

/**
 * GRASP: Controller for UC-00 System Login.
 * Validates credentials and creates user sessions via Factory pattern.
 */
public class LoginController {

    private final UserDAO userDAO;
    private static UserSession currentSession;

    public LoginController() {
        this.userDAO = new UserDAO();
    }

    /**
     * Authenticates user credentials and creates a session.
     * Flow: validate → authenticate via DAO → create session via Factory
     */
    public UserSession login(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            System.err.println("[LoginController] Username and password are required.");
            return null;
        }

        User user = userDAO.authenticate(username.trim(), password);
        if (user == null) {
            System.err.println("[LoginController] Invalid credentials for: " + username);
            return null;
        }

        // GoF: Factory Method – creates session based on user role
        currentSession = UserSessionFactory.createSession(user);
        System.out.println("[LoginController] Login successful: " + currentSession);
        return currentSession;
    }

    /**
     * Logs out the current user and clears the session.
     */
    public void logout() {
        if (currentSession != null) {
            System.out.println("[LoginController] Logged out: " + currentSession.getFullName());
            currentSession = null;
        }
    }

    /**
     * Returns the current active session.
     */
    public static UserSession getCurrentSession() {
        return currentSession;
    }

    public static void setCurrentSession(UserSession session) {
        currentSession = session;
    }
}
