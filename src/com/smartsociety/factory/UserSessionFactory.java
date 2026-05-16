package com.smartsociety.factory;

import com.smartsociety.model.User;
import com.smartsociety.model.UserSession;
import java.util.UUID;

/**
 * GoF: Factory Method Pattern
 * Creates appropriate UserSession objects based on user role.
 * Used in UC-00: System Login to create role-specific sessions.
 */
public class UserSessionFactory {

    /**
     * Factory method that creates a UserSession for the authenticated user.
     * Generates a unique session token and wraps the user in a session object.
     *
     * @param user the authenticated user object
     * @return a new UserSession instance
     * @throws IllegalArgumentException if user or role is null
     */
    public static UserSession createSession(User user) {
        if (user == null || user.getRole() == null) {
            throw new IllegalArgumentException("Cannot create session: User or role is null.");
        }

        // Generate unique session token
        String sessionToken = generateSessionToken(user);

        System.out.println("[UserSessionFactory] Created " + user.getRole() + " session for: " + user.getFullName());
        return new UserSession(user, sessionToken);
    }

    /**
     * Generates a unique session token combining role, user ID, and UUID.
     */
    private static String generateSessionToken(User user) {
        return user.getRole().name() + "-" + user.getUserId() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
