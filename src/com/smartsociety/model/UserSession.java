package com.smartsociety.model;

/**
 * Represents the current user session after successful login.
 * GoF: Created by Factory Method pattern (UserSessionFactory).
 * Holds the authenticated user's information for the duration of their session.
 */
public class UserSession {

    private final User user;
    private final String sessionToken;
    private final long loginTimestamp;

    /**
     * Creates a new user session.
     * @param user the authenticated user
     * @param sessionToken unique session identifier
     */
    public UserSession(User user, String sessionToken) {
        this.user = user;
        this.sessionToken = sessionToken;
        this.loginTimestamp = System.currentTimeMillis();
    }

    public User getUser() { return user; }
    public int getUserId() { return user.getUserId(); }
    public String getUsername() { return user.getUsername(); }
    public User.Role getRole() { return user.getRole(); }
    public String getFullName() { return user.getFullName(); }
    public String getUnitNumber() { return user.getUnitNumber(); }
    public String getSessionToken() { return sessionToken; }
    public long getLoginTimestamp() { return loginTimestamp; }

    /**
     * Checks if the session belongs to a specific role.
     */
    public boolean isAdmin()    { return user.getRole() == User.Role.ADMIN; }
    public boolean isGuard()    { return user.getRole() == User.Role.GUARD; }
    public boolean isResident() { return user.getRole() == User.Role.RESIDENT; }

    @Override
    public String toString() {
        return "Session[" + user.getFullName() + " | " + user.getRole() + "]";
    }
}
