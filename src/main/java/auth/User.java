package main.java.auth;

/**
 * User entity as defined in the UML class diagram.
 * Represents a user of the system with role-based permissions.
 */
public class User {
    private String userEmail;
    private String role;

    // Role constants
    public static final String ROLE_ADMINISTRATOR = "Administrator";
    public static final String ROLE_CUSTOMARY = "Customary";

    public User(String userEmail, String role) {
        this.userEmail = userEmail;
        // Validate and set role
        if (role.equals(ROLE_ADMINISTRATOR) || role.equals(ROLE_CUSTOMARY)) {
            this.role = role;
        } else {
            // Default to customary user if invalid role
            this.role = ROLE_CUSTOMARY;
        }
    }

    public String getUserEmail() {
        return userEmail;
    }

    public String getRole() {
        return role;
    }

    public boolean isAdministrator() {
        return role.equals(ROLE_ADMINISTRATOR);
    }

    public boolean canManageSharedBoards() {
        return isAdministrator();
    }

    public boolean canManageDeadlines() {
        return isAdministrator();
    }

    @Override
    public String toString() {
        return userEmail + " (" + role + ")";
    }
}
