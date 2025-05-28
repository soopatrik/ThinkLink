package main.java.database;

import main.java.board.Box;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object for Box operations
 */
public class BoxDAO {
    private final DatabaseConnectionManager dbManager;

    public BoxDAO() {
        this.dbManager = DatabaseConnectionManager.getInstance();
    }

    /**
     * Creates a new box in the database
     */
    public int createBox(String boardId, String title, String content, int x, int y, int width, int height,
            String color) {
        String sql = "INSERT INTO boxes (board_id, title, content, position_x, position_y, width, height, color) VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING box_id";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, boardId);
            stmt.setString(2, title);
            stmt.setString(3, content);
            stmt.setInt(4, x);
            stmt.setInt(5, y);
            stmt.setInt(6, width);
            stmt.setInt(7, height);
            stmt.setString(8, color);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("box_id");
            }

        } catch (SQLException e) {
            System.err.println("Error creating box: " + e.getMessage());
        }

        return -1; // Return -1 to indicate failure
    }

    /**
     * Creates a box from a Box object
     */
    public int createBox(String boardId, Box box) {
        return createBox(boardId, box.getTitle(), box.getContent(),
                box.getBoxX(), box.getBoxY(), box.getBoxWidth(),
                box.getBoxHeight(), "#F0F0F0");
    }

    /**
     * Updates an existing box
     */
    public boolean updateBox(int boxId, String title, String content, int x, int y, int width, int height,
            String color) {
        String sql = "UPDATE boxes SET title = ?, content = ?, position_x = ?, position_y = ?, width = ?, height = ?, color = ?, updated_at = CURRENT_TIMESTAMP WHERE box_id = ?";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, title);
            stmt.setString(2, content);
            stmt.setInt(3, x);
            stmt.setInt(4, y);
            stmt.setInt(5, width);
            stmt.setInt(6, height);
            stmt.setString(7, color);
            stmt.setInt(8, boxId);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error updating box: " + e.getMessage());
            return false;
        }
    }

    /**
     * Updates a box from a Box object
     */
    public boolean updateBox(Box box) {
        return updateBox(box.getId(), box.getTitle(), box.getContent(),
                box.getBoxX(), box.getBoxY(), box.getBoxWidth(),
                box.getBoxHeight(), "#F0F0F0");
    }

    /**
     * Retrieves a box by ID
     */
    public Map<String, Object> getBoxById(int boxId) {
        String sql = "SELECT * FROM boxes WHERE box_id = ?";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, boxId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Map<String, Object> box = new HashMap<>();
                box.put("box_id", rs.getInt("box_id"));
                box.put("board_id", rs.getString("board_id"));
                box.put("title", rs.getString("title"));
                box.put("content", rs.getString("content"));
                box.put("position_x", rs.getInt("position_x"));
                box.put("position_y", rs.getInt("position_y"));
                box.put("width", rs.getInt("width"));
                box.put("height", rs.getInt("height"));
                box.put("color", rs.getString("color"));
                box.put("created_at", rs.getTimestamp("created_at"));
                box.put("updated_at", rs.getTimestamp("updated_at"));
                return box;
            }

        } catch (SQLException e) {
            System.err.println("Error retrieving box: " + e.getMessage());
        }

        return null;
    }

    /**
     * Gets all boxes for a specific board
     */
    public List<Map<String, Object>> getBoxesForBoard(String boardId) {
        List<Map<String, Object>> boxes = new ArrayList<>();
        String sql = "SELECT * FROM boxes WHERE board_id = ? ORDER BY created_at";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, boardId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Map<String, Object> box = new HashMap<>();
                box.put("box_id", rs.getInt("box_id"));
                box.put("board_id", rs.getString("board_id"));
                box.put("title", rs.getString("title"));
                box.put("content", rs.getString("content"));
                box.put("position_x", rs.getInt("position_x"));
                box.put("position_y", rs.getInt("position_y"));
                box.put("width", rs.getInt("width"));
                box.put("height", rs.getInt("height"));
                box.put("color", rs.getString("color"));
                box.put("created_at", rs.getTimestamp("created_at"));
                box.put("updated_at", rs.getTimestamp("updated_at"));
                boxes.add(box);
            }

        } catch (SQLException e) {
            System.err.println("Error retrieving boxes for board: " + e.getMessage());
        }

        return boxes;
    }

    /**
     * Deletes a box and all its connections
     */
    public boolean deleteBox(int boxId) {
        String sql = "DELETE FROM boxes WHERE box_id = ?";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, boxId);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error deleting box: " + e.getMessage());
            return false;
        }
    }

    /**
     * Updates only the position of a box
     */
    public boolean updateBoxPosition(int boxId, int x, int y) {
        String sql = "UPDATE boxes SET position_x = ?, position_y = ?, updated_at = CURRENT_TIMESTAMP WHERE box_id = ?";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, x);
            stmt.setInt(2, y);
            stmt.setInt(3, boxId);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error updating box position: " + e.getMessage());
            return false;
        }
    }

    /**
     * Updates only the size of a box
     */
    public boolean updateBoxSize(int boxId, int width, int height) {
        String sql = "UPDATE boxes SET width = ?, height = ?, updated_at = CURRENT_TIMESTAMP WHERE box_id = ?";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, width);
            stmt.setInt(2, height);
            stmt.setInt(3, boxId);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error updating box size: " + e.getMessage());
            return false;
        }
    }

    /**
     * Converts database row to Box object
     */
    public Box mapToBox(Map<String, Object> boxData) {
        if (boxData == null)
            return null;

        int x = (Integer) boxData.get("position_x");
        int y = (Integer) boxData.get("position_y");
        String title = (String) boxData.get("title");
        String content = (String) boxData.get("content");
        int id = (Integer) boxData.get("box_id");

        Box box = new Box(x, y, title, content, id);
        box.setBoxSize((Integer) boxData.get("height"), (Integer) boxData.get("width"));

        return box;
    }
}