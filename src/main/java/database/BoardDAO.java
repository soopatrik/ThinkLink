package main.java.database;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object for Board operations using PostgreSQL
 */
public class BoardDAO {
    private final DatabaseConnectionManager dbManager;

    public BoardDAO() {
        this.dbManager = DatabaseConnectionManager.getInstance();
    }

    /**
     * Creates a new board
     */
    public boolean createBoard(String boardId, String boardName, String creatorEmail, boolean isShared) {
        String sql = "INSERT INTO boards (board_id, board_name, creator_email, is_shared) VALUES (?, ?, ?, ?) ON CONFLICT (board_id) DO UPDATE SET board_name = EXCLUDED.board_name, updated_at = CURRENT_TIMESTAMP";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, boardId);
            stmt.setString(2, boardName);
            stmt.setString(3, creatorEmail);
            stmt.setBoolean(4, isShared);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error creating board: " + e.getMessage());
            return false;
        }
    }

    /**
     * Retrieves board information
     */
    public Map<String, Object> getBoardById(String boardId) {
        String sql = "SELECT * FROM boards WHERE board_id = ?";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, boardId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Map<String, Object> board = new HashMap<>();
                board.put("board_id", rs.getString("board_id"));
                board.put("board_name", rs.getString("board_name"));
                board.put("creator_email", rs.getString("creator_email"));
                board.put("is_shared", rs.getBoolean("is_shared"));
                board.put("created_at", rs.getTimestamp("created_at"));
                board.put("updated_at", rs.getTimestamp("updated_at"));
                return board;
            }

        } catch (SQLException e) {
            System.err.println("Error retrieving board: " + e.getMessage());
        }

        return null;
    }

    /**
     * Gets all boards accessible to a user
     */
    public List<Map<String, Object>> getBoardsForUser(String userEmail, boolean includeShared) {
        List<Map<String, Object>> boards = new ArrayList<>();
        String sql;

        if (includeShared) {
            sql = "SELECT * FROM boards WHERE creator_email = ? OR is_shared = TRUE ORDER BY updated_at DESC";
        } else {
            sql = "SELECT * FROM boards WHERE creator_email = ? ORDER BY updated_at DESC";
        }

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, userEmail);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Map<String, Object> board = new HashMap<>();
                board.put("board_id", rs.getString("board_id"));
                board.put("board_name", rs.getString("board_name"));
                board.put("creator_email", rs.getString("creator_email"));
                board.put("is_shared", rs.getBoolean("is_shared"));
                board.put("created_at", rs.getTimestamp("created_at"));
                board.put("updated_at", rs.getTimestamp("updated_at"));
                boards.add(board);
            }

        } catch (SQLException e) {
            System.err.println("Error retrieving boards for user: " + e.getMessage());
        }

        return boards;
    }

    /**
     * Updates board information
     */
    public boolean updateBoard(String boardId, String newName, boolean isShared) {
        String sql = "UPDATE boards SET board_name = ?, is_shared = ?, updated_at = CURRENT_TIMESTAMP WHERE board_id = ?";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, newName);
            stmt.setBoolean(2, isShared);
            stmt.setString(3, boardId);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error updating board: " + e.getMessage());
            return false;
        }
    }

    /**
     * Deletes a board and all associated data
     */
    public boolean deleteBoard(String boardId) {
        String sql = "DELETE FROM boards WHERE board_id = ?";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, boardId);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error deleting board: " + e.getMessage());
            return false;
        }
    }

    /**
     * Gets all shared boards
     */
    public List<Map<String, Object>> getSharedBoards() {
        List<Map<String, Object>> boards = new ArrayList<>();
        String sql = "SELECT * FROM boards WHERE is_shared = TRUE ORDER BY updated_at DESC";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> board = new HashMap<>();
                board.put("board_id", rs.getString("board_id"));
                board.put("board_name", rs.getString("board_name"));
                board.put("creator_email", rs.getString("creator_email"));
                board.put("is_shared", rs.getBoolean("is_shared"));
                board.put("created_at", rs.getTimestamp("created_at"));
                board.put("updated_at", rs.getTimestamp("updated_at"));
                boards.add(board);
            }

        } catch (SQLException e) {
            System.err.println("Error retrieving shared boards: " + e.getMessage());
        }

        return boards;
    }

    /**
     * Updates board's last modified timestamp
     */
    public boolean touchBoard(String boardId) {
        String sql = "UPDATE boards SET updated_at = CURRENT_TIMESTAMP WHERE board_id = ?";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, boardId);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error touching board: " + e.getMessage());
            return false;
        }
    }
}