package main.java.database;

import main.java.calendar.Deadline;
import main.java.auth.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Data Access Object for Deadline operations
 */
public class DeadlineDAO {
    private final DatabaseConnectionManager dbManager;

    public DeadlineDAO() {
        this.dbManager = DatabaseConnectionManager.getInstance();
    }

    /**
     * Creates a new deadline
     */
    public boolean createDeadline(String description, Date dueDate, String assignedToEmail, String createdByEmail) {
        String sql = "INSERT INTO deadlines (description, due_date, assigned_to, created_by) VALUES (?, ?, ?, ?)";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, description);
            stmt.setTimestamp(2, new Timestamp(dueDate.getTime()));
            stmt.setString(3, assignedToEmail);
            stmt.setString(4, createdByEmail);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error creating deadline: " + e.getMessage());
            return false;
        }
    }

    /**
     * Gets all deadlines assigned to a user
     */
    public List<Deadline> getDeadlinesForUser(String userEmail) {
        List<Deadline> deadlines = new ArrayList<>();
        String sql = """
                    SELECT d.description, d.due_date, d.assigned_to, d.is_completed,
                           u1.user_email as assigned_email, u1.role as assigned_role,
                           u2.user_email as creator_email, u2.role as creator_role
                    FROM deadlines d
                    LEFT JOIN users u1 ON d.assigned_to = u1.user_email
                    LEFT JOIN users u2 ON d.created_by = u2.user_email
                    WHERE d.assigned_to = ?
                    ORDER BY d.due_date
                """;

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, userEmail);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String description = rs.getString("description");
                Date dueDate = new Date(rs.getTimestamp("due_date").getTime());

                String assignedEmail = rs.getString("assigned_email");
                String assignedRole = rs.getString("assigned_role");
                User assignedUser = new User(assignedEmail, assignedRole);

                Deadline deadline = new Deadline(description, dueDate, assignedUser);
                deadlines.add(deadline);
            }

        } catch (SQLException e) {
            System.err.println("Error retrieving deadlines for user: " + e.getMessage());
        }

        return deadlines;
    }

    /**
     * Gets all deadlines in the system (admin function)
     */
    public List<Deadline> getAllDeadlines() {
        List<Deadline> deadlines = new ArrayList<>();
        String sql = """
                    SELECT d.description, d.due_date, d.assigned_to, d.is_completed,
                           u1.user_email as assigned_email, u1.role as assigned_role
                    FROM deadlines d
                    LEFT JOIN users u1 ON d.assigned_to = u1.user_email
                    ORDER BY d.due_date
                """;

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String description = rs.getString("description");
                Date dueDate = new Date(rs.getTimestamp("due_date").getTime());

                String assignedEmail = rs.getString("assigned_email");
                String assignedRole = rs.getString("assigned_role");
                User assignedUser = new User(assignedEmail, assignedRole);

                Deadline deadline = new Deadline(description, dueDate, assignedUser);
                deadlines.add(deadline);
            }

        } catch (SQLException e) {
            System.err.println("Error retrieving all deadlines: " + e.getMessage());
        }

        return deadlines;
    }

    /**
     * Updates deadline completion status
     */
    public boolean updateDeadlineCompletion(String description, String assignedTo, boolean isCompleted) {
        String sql = "UPDATE deadlines SET is_completed = ?, updated_at = CURRENT_TIMESTAMP WHERE description = ? AND assigned_to = ?";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBoolean(1, isCompleted);
            stmt.setString(2, description);
            stmt.setString(3, assignedTo);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error updating deadline completion: " + e.getMessage());
            return false;
        }
    }

    /**
     * Deletes a deadline
     */
    public boolean deleteDeadline(String description, String assignedTo) {
        String sql = "DELETE FROM deadlines WHERE description = ? AND assigned_to = ?";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, description);
            stmt.setString(2, assignedTo);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error deleting deadline: " + e.getMessage());
            return false;
        }
    }
}