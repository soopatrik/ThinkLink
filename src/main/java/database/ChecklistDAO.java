package main.java.database;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object for Checklist operations
 */
public class ChecklistDAO {
    private final DatabaseConnectionManager dbManager;

    public ChecklistDAO() {
        this.dbManager = DatabaseConnectionManager.getInstance();
    }

    /**
     * Saves a checklist with its items
     */
    public boolean saveChecklist(String userEmail, String title, List<Map<String, Object>> items) {
        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // Create or update checklist
                String checklistSQL = "INSERT INTO checklists (user_email, title) VALUES (?, ?) ON CONFLICT (user_email, title) DO UPDATE SET updated_at = CURRENT_TIMESTAMP RETURNING checklist_id";
                int checklistId;

                try (PreparedStatement stmt = conn.prepareStatement(checklistSQL)) {
                    stmt.setString(1, userEmail);
                    stmt.setString(2, title);
                    ResultSet rs = stmt.executeQuery();

                    if (rs.next()) {
                        checklistId = rs.getInt("checklist_id");
                    } else {
                        // If it was an update, get the existing ID
                        String getIdSQL = "SELECT checklist_id FROM checklists WHERE user_email = ? AND title = ?";
                        try (PreparedStatement getIdStmt = conn.prepareStatement(getIdSQL)) {
                            getIdStmt.setString(1, userEmail);
                            getIdStmt.setString(2, title);
                            ResultSet idRS = getIdStmt.executeQuery();
                            if (idRS.next()) {
                                checklistId = idRS.getInt("checklist_id");
                            } else {
                                throw new SQLException("Failed to get checklist ID");
                            }
                        }
                    }
                }

                // Clear existing items
                String clearSQL = "DELETE FROM checklist_items WHERE checklist_id = ?";
                try (PreparedStatement clearStmt = conn.prepareStatement(clearSQL)) {
                    clearStmt.setInt(1, checklistId);
                    clearStmt.executeUpdate();
                }

                // Insert new items
                if (!items.isEmpty()) {
                    String itemSQL = "INSERT INTO checklist_items (checklist_id, text, is_completed, position_order) VALUES (?, ?, ?, ?)";
                    try (PreparedStatement itemStmt = conn.prepareStatement(itemSQL)) {
                        for (int i = 0; i < items.size(); i++) {
                            Map<String, Object> item = items.get(i);
                            itemStmt.setInt(1, checklistId);
                            itemStmt.setString(2, (String) item.get("text"));
                            itemStmt.setBoolean(3, (Boolean) item.get("completed"));
                            itemStmt.setInt(4, i);
                            itemStmt.addBatch();
                        }
                        itemStmt.executeBatch();
                    }
                }

                conn.commit();
                return true;

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.err.println("Error saving checklist: " + e.getMessage());
            return false;
        }
    }

    /**
     * Loads a checklist with its items
     */
    public Map<String, Object> loadChecklist(String userEmail, String title) {
        String sql = """
                    SELECT c.title, ci.text, ci.is_completed
                    FROM checklists c
                    LEFT JOIN checklist_items ci ON c.checklist_id = ci.checklist_id
                    WHERE c.user_email = ? AND c.title = ?
                    ORDER BY ci.position_order
                """;

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, userEmail);
            stmt.setString(2, title);
            ResultSet rs = stmt.executeQuery();

            Map<String, Object> result = new HashMap<>();
            List<Map<String, Object>> items = new ArrayList<>();
            String checklistTitle = null;

            while (rs.next()) {
                if (checklistTitle == null) {
                    checklistTitle = rs.getString("title");
                }

                String itemText = rs.getString("text");
                if (itemText != null) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("text", itemText);
                    item.put("completed", rs.getBoolean("is_completed"));
                    items.add(item);
                }
            }

            if (checklistTitle != null) {
                result.put("title", checklistTitle);
                result.put("items", items);
                return result;
            }

        } catch (SQLException e) {
            System.err.println("Error loading checklist: " + e.getMessage());
        }

        return null;
    }

    /**
     * Gets all checklist titles for a user
     */
    public List<String> getChecklistList(String userEmail) {
        List<String> checklists = new ArrayList<>();
        String sql = "SELECT title FROM checklists WHERE user_email = ? ORDER BY updated_at DESC";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, userEmail);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                checklists.add(rs.getString("title"));
            }

        } catch (SQLException e) {
            System.err.println("Error retrieving checklist list: " + e.getMessage());
        }

        return checklists;
    }

    /**
     * Deletes a checklist and all its items
     */
    public boolean deleteChecklist(String userEmail, String title) {
        String sql = "DELETE FROM checklists WHERE user_email = ? AND title = ?";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, userEmail);
            stmt.setString(2, title);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error deleting checklist: " + e.getMessage());
            return false;
        }
    }
}