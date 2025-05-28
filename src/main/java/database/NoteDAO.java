package main.java.database;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object for Note operations
 */
public class NoteDAO {
    private final DatabaseConnectionManager dbManager;

    public NoteDAO() {
        this.dbManager = DatabaseConnectionManager.getInstance();
    }

    /**
     * Saves a note for a user
     */
    public boolean saveNote(String userEmail, String title, String content) {
        String sql = "INSERT INTO notes (user_email, title, content) VALUES (?, ?, ?) ON CONFLICT (user_email, title) DO UPDATE SET content = EXCLUDED.content, updated_at = CURRENT_TIMESTAMP";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, userEmail);
            stmt.setString(2, title);
            stmt.setString(3, content);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error saving note: " + e.getMessage());
            return false;
        }
    }

    /**
     * Loads a specific note for a user
     */
    public Map<String, String> loadNote(String userEmail, String title) {
        String sql = "SELECT title, content FROM notes WHERE user_email = ? AND title = ?";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, userEmail);
            stmt.setString(2, title);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Map<String, String> note = new HashMap<>();
                note.put("title", rs.getString("title"));
                note.put("content", rs.getString("content"));
                return note;
            }

        } catch (SQLException e) {
            System.err.println("Error loading note: " + e.getMessage());
        }

        return null;
    }

    /**
     * Gets all note titles for a user
     */
    public List<String> getNoteList(String userEmail) {
        List<String> notes = new ArrayList<>();
        String sql = "SELECT title FROM notes WHERE user_email = ? ORDER BY updated_at DESC";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, userEmail);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                notes.add(rs.getString("title"));
            }

        } catch (SQLException e) {
            System.err.println("Error retrieving note list: " + e.getMessage());
        }

        return notes;
    }

    /**
     * Deletes a note
     */
    public boolean deleteNote(String userEmail, String title) {
        String sql = "DELETE FROM notes WHERE user_email = ? AND title = ?";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, userEmail);
            stmt.setString(2, title);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error deleting note: " + e.getMessage());
            return false;
        }
    }
}