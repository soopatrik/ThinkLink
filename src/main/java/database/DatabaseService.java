package main.java.database;

import main.java.auth.User;
import main.java.board.Box;
import main.java.board.BoxList;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Service layer that coordinates database operations and provides
 * high-level database functionality for the ThinkLink application
 * Using PostgreSQL for multi-user database support
 */
public class DatabaseService {
    private final UserDAO userDAO;
    private final BoardDAO boardDAO;
    private final BoxDAO boxDAO;
    private final BoxConnectionDAO connectionDAO;
    private final NoteDAO noteDAO;
    private final ChecklistDAO checklistDAO;
    private final DeadlineDAO deadlineDAO;

    public DatabaseService() {
        this.userDAO = new UserDAO();
        this.boardDAO = new BoardDAO();
        this.boxDAO = new BoxDAO();
        this.connectionDAO = new BoxConnectionDAO();
        this.noteDAO = new NoteDAO();
        this.checklistDAO = new ChecklistDAO();
        this.deadlineDAO = new DeadlineDAO();
    }

    /**
     * Initialize the database schema
     */
    public void initializeDatabase() throws SQLException {
        DatabaseConnectionManager.getInstance().initializeDatabase();
    }

    /**
     * Handles user login - creates user if doesn't exist, updates last login
     */
    public User handleUserLogin(String email, String role) {
        User user = new User(email, role);
        userDAO.createUser(user); // Creates or updates
        userDAO.updateLastLogin(email);
        return userDAO.getUserByEmail(email);
    }

    /**
     * Saves complete board state to database
     */
    public boolean saveBoardToDatabase(String boardId, String boardName, String creatorEmail, BoxList boxList) {
        try {
            // Create/update board
            boardDAO.createBoard(boardId, boardName, creatorEmail, true);

            // Clear existing boxes for this board (cascade will handle connections)
            // This is a simplified approach - in production you might want more
            // sophisticated sync

            Box current = boxList.getFirstNode();
            while (current != null) {
                // Create/update box
                if (current.getId() > 0) {
                    boxDAO.updateBox(current);
                } else {
                    int newBoxId = boxDAO.createBox(boardId, current);
                    current.setId(newBoxId);
                }

                // Update connections
                connectionDAO.updateConnectionsForBox(current.getId(), current.getConnectedBoxIds());

                current = current.getNext();
            }

            // Update board timestamp
            boardDAO.touchBoard(boardId);
            return true;

        } catch (Exception e) {
            System.err.println("Error saving board to PostgreSQL database: " + e.getMessage());
            return false;
        }
    }

    /**
     * Loads board state from database
     */
    public BoxList loadBoardFromDatabase(String boardId) {
        BoxList boxList = new BoxList();

        try {
            // Get all boxes for the board
            List<Map<String, Object>> boxes = boxDAO.getBoxesForBoard(boardId);

            for (Map<String, Object> boxData : boxes) {
                Box box = boxDAO.mapToBox(boxData);
                if (box != null) {
                    // Load connections for this box
                    List<Integer> connections = connectionDAO.getConnectionsForBox(box.getId());
                    box.setConnectedBoxIds(connections);

                    boxList.addNode(box);
                }
            }

        } catch (Exception e) {
            System.err.println("Error loading board from PostgreSQL database: " + e.getMessage());
        }

        return boxList;
    }

    /**
     * Saves a single box update to database
     */
    public boolean saveBoxUpdate(Box box, String boardId) {
        try {
            if (box.getId() > 0) {
                // Update existing box
                boolean boxUpdated = boxDAO.updateBox(box);
                boolean connectionsUpdated = connectionDAO.updateConnectionsForBox(box.getId(),
                        box.getConnectedBoxIds());
                boardDAO.touchBoard(boardId);
                return boxUpdated && connectionsUpdated;
            } else {
                // Create new box
                int newBoxId = boxDAO.createBox(boardId, box);
                if (newBoxId > 0) {
                    box.setId(newBoxId);
                    connectionDAO.updateConnectionsForBox(newBoxId, box.getConnectedBoxIds());
                    boardDAO.touchBoard(boardId);
                    return true;
                }
            }
        } catch (Exception e) {
            System.err.println("Error saving box update to PostgreSQL: " + e.getMessage());
        }
        return false;
    }

    /**
     * Deletes a box from database
     */
    public boolean deleteBoxFromDatabase(int boxId, String boardId) {
        try {
            boolean deleted = boxDAO.deleteBox(boxId);
            if (deleted) {
                boardDAO.touchBoard(boardId);
            }
            return deleted;
        } catch (Exception e) {
            System.err.println("Error deleting box from PostgreSQL database: " + e.getMessage());
            return false;
        }
    }

    /**
     * Converts BoxList to JSON format
     */
    public JSONObject boardToJSON(String boardId) {
        try {
            BoxList boxList = loadBoardFromDatabase(boardId);
            JSONObject boardData = new JSONObject();
            boardData.put("boardId", boardId);
            boardData.put("lastUpdated", System.currentTimeMillis());

            JSONArray boxesArray = new JSONArray();
            Box current = boxList.getFirstNode();

            while (current != null) {
                JSONObject boxJson = new JSONObject();
                boxJson.put("id", current.getId());
                boxJson.put("title", current.getTitle());
                boxJson.put("content", current.getContent());
                boxJson.put("x", current.getBoxX());
                boxJson.put("y", current.getBoxY());

                JSONArray connectionsArray = new JSONArray();
                for (Integer connectionId : current.getConnectedBoxIds()) {
                    connectionsArray.put(connectionId);
                }
                boxJson.put("connections", connectionsArray);

                boxesArray.put(boxJson);
                current = current.getNext();
            }

            boardData.put("boxes", boxesArray);
            return boardData;

        } catch (Exception e) {
            System.err.println("Error converting board to JSON: " + e.getMessage());
            return null;
        }
    }

    // Delegate methods for direct DAO access when needed
    public UserDAO getUserDAO() {
        return userDAO;
    }

    public BoardDAO getBoardDAO() {
        return boardDAO;
    }

    public BoxDAO getBoxDAO() {
        return boxDAO;
    }

    public BoxConnectionDAO getConnectionDAO() {
        return connectionDAO;
    }

    public NoteDAO getNoteDAO() {
        return noteDAO;
    }

    public ChecklistDAO getChecklistDAO() {
        return checklistDAO;
    }

    public DeadlineDAO getDeadlineDAO() {
        return deadlineDAO;
    }
}