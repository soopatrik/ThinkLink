package main.java.database;

import main.java.auth.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for User operations using PostgreSQL
 */
public class UserDAO {
    private final DatabaseConnectionManager dbManager;

    public UserDAO() {
        this.dbManager = DatabaseConnectionManager.getInstance();
    }

    /**
     * Creates a new user in the database
     */
    public boolean createUser(User user) {
        String sql = "INSERT INTO users (user_email, role, last_login) VALUES (?, ?, CURRENT_TIMESTAMP) ON CONFLICT (user_email) DO UPDATE SET last_login = CURRENT_TIMESTAMP, role = EXCLUDED.role";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getUserEmail());
            stmt.setString(2, user.getRole());

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error creating/updating user: " + e.getMessage());
            return false;
        }
    }

    /**
     * Retrieves a user by email
     */
    public User getUserByEmail(String email) {
        String sql = "SELECT user_email, role FROM users WHERE user_email = ? AND is_active = TRUE";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new User(rs.getString("user_email"), rs.getString("role"));
            }

        } catch (SQLException e) {
            System.err.println("Error retrieving user: " + e.getMessage());
        }

        return null;
    }

    /**
     * Gets all users in the system
     */
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT user_email, role FROM users WHERE is_active = TRUE ORDER BY user_email";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                users.add(new User(rs.getString("user_email"), rs.getString("role")));
            }

        } catch (SQLException e) {
            System.err.println("Error retrieving all users: " + e.getMessage());
        }

        return users;
    }

    /**
     * Updates user role
     */
    public boolean updateUserRole(String email, String newRole) {
        String sql = "UPDATE users SET role = ?, updated_at = CURRENT_TIMESTAMP WHERE user_email = ?";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, newRole);
            stmt.setString(2, email);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error updating user role: " + e.getMessage());
            return false;
        }
    }

    /**
     * Updates user's last login timestamp
     */
    public boolean updateLastLogin(String email) {
        String sql = "UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE user_email = ?";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error updating last login: " + e.getMessage());
            return false;
        }
    }

    /**
     * Deactivates a user (soft delete)
     */
    public boolean deactivateUser(String email) {
        String sql = "UPDATE users SET is_active = FALSE, updated_at = CURRENT_TIMESTAMP WHERE user_email = ?";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error deactivating user: " + e.getMessage());
            return false;
        }
    }

    /**
     * Gets all administrator users
     */
    public List<User> getAdministrators() {
        List<User> admins = new ArrayList<>();
        String sql = "SELECT user_email, role FROM users WHERE role = ? AND is_active = TRUE ORDER BY user_email";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, User.ROLE_ADMINISTRATOR);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                admins.add(new User(rs.getString("user_email"), rs.getString("role")));
            }

        } catch (SQLException e) {
            System.err.println("Error retrieving administrators: " + e.getMessage());
        }

        return admins;
    }
}