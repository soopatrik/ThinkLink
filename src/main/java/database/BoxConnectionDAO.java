package main.java.database;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object for Box Connection operations
 */
public class BoxConnectionDAO {
    private final DatabaseConnectionManager dbManager;

    public BoxConnectionDAO() {
        this.dbManager = DatabaseConnectionManager.getInstance();
    }

    /**
     * Creates a connection between two boxes
     */
    public boolean createConnection(int sourceBoxId, int targetBoxId) {
        String sql = "INSERT INTO box_connections (source_box_id, target_box_id) VALUES (?, ?) ON CONFLICT (source_box_id, target_box_id) DO NOTHING";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, sourceBoxId);
            stmt.setInt(2, targetBoxId);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error creating box connection: " + e.getMessage());
            return false;
        }
    }

    /**
     * Removes a connection between two boxes
     */
    public boolean removeConnection(int sourceBoxId, int targetBoxId) {
        String sql = "DELETE FROM box_connections WHERE source_box_id = ? AND target_box_id = ?";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, sourceBoxId);
            stmt.setInt(2, targetBoxId);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error removing box connection: " + e.getMessage());
            return false;
        }
    }

    /**
     * Gets all connections for a specific box (outgoing connections)
     */
    public List<Integer> getConnectionsForBox(int boxId) {
        List<Integer> connections = new ArrayList<>();
        String sql = "SELECT target_box_id FROM box_connections WHERE source_box_id = ?";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, boxId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                connections.add(rs.getInt("target_box_id"));
            }

        } catch (SQLException e) {
            System.err.println("Error retrieving connections for box: " + e.getMessage());
        }

        return connections;
    }

    /**
     * Gets all incoming connections for a specific box
     */
    public List<Integer> getIncomingConnectionsForBox(int boxId) {
        List<Integer> connections = new ArrayList<>();
        String sql = "SELECT source_box_id FROM box_connections WHERE target_box_id = ?";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, boxId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                connections.add(rs.getInt("source_box_id"));
            }

        } catch (SQLException e) {
            System.err.println("Error retrieving incoming connections for box: " + e.getMessage());
        }

        return connections;
    }

    /**
     * Gets all connections for boxes in a specific board
     */
    public List<Map<String, Object>> getConnectionsForBoard(String boardId) {
        List<Map<String, Object>> connections = new ArrayList<>();
        String sql = """
                    SELECT bc.source_box_id, bc.target_box_id, bc.created_at
                    FROM box_connections bc
                    JOIN boxes b1 ON bc.source_box_id = b1.box_id
                    JOIN boxes b2 ON bc.target_box_id = b2.box_id
                    WHERE b1.board_id = ? AND b2.board_id = ?
                """;

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, boardId);
            stmt.setString(2, boardId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Map<String, Object> connection = new HashMap<>();
                connection.put("source_box_id", rs.getInt("source_box_id"));
                connection.put("target_box_id", rs.getInt("target_box_id"));
                connection.put("created_at", rs.getTimestamp("created_at"));
                connections.add(connection);
            }

        } catch (SQLException e) {
            System.err.println("Error retrieving connections for board: " + e.getMessage());
        }

        return connections;
    }

    /**
     * Removes all connections for a specific box
     */
    public boolean removeAllConnectionsForBox(int boxId) {
        String sql = "DELETE FROM box_connections WHERE source_box_id = ? OR target_box_id = ?";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, boxId);
            stmt.setInt(2, boxId);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected >= 0; // Return true even if no connections were removed

        } catch (SQLException e) {
            System.err.println("Error removing all connections for box: " + e.getMessage());
            return false;
        }
    }

    /**
     * Checks if a connection exists between two boxes
     */
    public boolean connectionExists(int sourceBoxId, int targetBoxId) {
        String sql = "SELECT 1 FROM box_connections WHERE source_box_id = ? AND target_box_id = ?";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, sourceBoxId);
            stmt.setInt(2, targetBoxId);
            ResultSet rs = stmt.executeQuery();

            return rs.next();

        } catch (SQLException e) {
            System.err.println("Error checking connection existence: " + e.getMessage());
            return false;
        }
    }

    /**
     * Updates all connections for a box (replaces existing connections)
     */
    public boolean updateConnectionsForBox(int boxId, List<Integer> newConnections) {
        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // Remove existing connections
                String deleteSQL = "DELETE FROM box_connections WHERE source_box_id = ?";
                try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSQL)) {
                    deleteStmt.setInt(1, boxId);
                    deleteStmt.executeUpdate();
                }

                // Add new connections
                if (!newConnections.isEmpty()) {
                    String insertSQL = "INSERT INTO box_connections (source_box_id, target_box_id) VALUES (?, ?)";
                    try (PreparedStatement insertStmt = conn.prepareStatement(insertSQL)) {
                        for (Integer targetId : newConnections) {
                            insertStmt.setInt(1, boxId);
                            insertStmt.setInt(2, targetId);
                            insertStmt.addBatch();
                        }
                        insertStmt.executeBatch();
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
            System.err.println("Error updating connections for box: " + e.getMessage());
            return false;
        }
    }
}