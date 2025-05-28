package main.java.utils;

import main.java.database.DatabaseService;
import main.java.board.BoxList;
import main.java.board.Box;
import org.json.JSONObject;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Enhanced SaveAndLoad that integrates with PostgreSQL database
 * while maintaining compatibility with existing file-based operations
 */
public class DatabaseSaveAndLoad extends SaveAndLoad {
    private final DatabaseService databaseService;
    private boolean useDatabasePrimary = true;

    public DatabaseSaveAndLoad() {
        super();
        this.databaseService = new DatabaseService();

        // Initialize PostgreSQL database on first use
        try {
            databaseService.initializeDatabase();
            System.out.println("PostgreSQL database connected successfully!");
        } catch (SQLException e) {
            System.err.println(
                    "Failed to connect to PostgreSQL database, falling back to file-only mode: " + e.getMessage());
            useDatabasePrimary = false;
        }
    }

    /**
     * Save shared board with database integration
     */
    @Override
    public void saveSharedBoard(BoxList boxList) {
        if (useDatabasePrimary) {
            try {
                String boardId = "shared-global-board";
                String boardName = "Global Shared Board";
                String creatorEmail = "system@thinklink.com";

                boolean dbSaved = databaseService.saveBoardToDatabase(boardId, boardName, creatorEmail, boxList);
                if (dbSaved) {
                    System.out.println("Shared board saved to PostgreSQL database successfully");
                } else {
                    System.err.println("Failed to save to database, falling back to file");
                    super.saveSharedBoard(boxList);
                }
            } catch (Exception e) {
                System.err.println("Database error, falling back to file: " + e.getMessage());
                super.saveSharedBoard(boxList);
            }
        } else {
            super.saveSharedBoard(boxList);
        }
    }

    /**
     * Load shared board with database integration
     */
    @Override
    public JSONObject loadSharedBoard() {
        if (useDatabasePrimary) {
            try {
                String boardId = "shared-global-board";
                JSONObject boardData = databaseService.boardToJSON(boardId);
                if (boardData != null) {
                    System.out.println("Shared board loaded from PostgreSQL database successfully");
                    return boardData;
                } else {
                    System.out.println("No data in database, trying file fallback");
                    return super.loadSharedBoard();
                }
            } catch (Exception e) {
                System.err.println("Database error, falling back to file: " + e.getMessage());
                return super.loadSharedBoard();
            }
        } else {
            return super.loadSharedBoard();
        }
    }

    /**
     * Save individual box update to database
     */
    public boolean saveBoxUpdate(Box box, String boardId) {
        if (useDatabasePrimary) {
            return databaseService.saveBoxUpdate(box, boardId);
        }
        return false;
    }

    /**
     * Delete box from database
     */
    public boolean deleteBoxFromDatabase(int boxId, String boardId) {
        if (useDatabasePrimary) {
            return databaseService.deleteBoxFromDatabase(boxId, boardId);
        }
        return false;
    }

    /**
     * Enhanced note saving with database
     */
    @Override
    public boolean saveNote(String title, String content, String username) {
        if (useDatabasePrimary) {
            boolean dbSaved = databaseService.getNoteDAO().saveNote(username, title, content);
            if (dbSaved) {
                SaveLog.getInstance().addLog("Saved note to PostgreSQL database: " + title + " for user " + username);
                return true;
            }
        }
        // Fallback to file-based save
        return super.saveNote(title, content, username);
    }

    /**
     * Enhanced note loading with database
     */
    @Override
    public Map<String, String> loadNote(String title, String username) {
        if (useDatabasePrimary) {
            Map<String, String> note = databaseService.getNoteDAO().loadNote(username, title);
            if (note != null) {
                SaveLog.getInstance()
                        .addLog("Loaded note from PostgreSQL database: " + title + " for user " + username);
                return note;
            }
        }
        // Fallback to file-based load
        return super.loadNote(title, username);
    }

    /**
     * Enhanced note list with database
     */
    @Override
    public List<String> getNoteList(String username) {
        if (useDatabasePrimary) {
            List<String> notes = databaseService.getNoteDAO().getNoteList(username);
            if (!notes.isEmpty()) {
                return notes;
            }
        }
        // Fallback to file-based list
        return super.getNoteList(username);
    }

    /**
     * Enhanced checklist saving with database
     */
    @Override
    public boolean saveChecklist(String title, List<Map<String, Object>> items, String username) {
        if (useDatabasePrimary) {
            boolean dbSaved = databaseService.getChecklistDAO().saveChecklist(username, title, items);
            if (dbSaved) {
                SaveLog.getInstance()
                        .addLog("Saved checklist to PostgreSQL database: " + title + " for user " + username);
                return true;
            }
        }
        // Fallback to file-based save
        return super.saveChecklist(title, items, username);
    }

    /**
     * Enhanced checklist loading with database
     */
    @Override
    public Map<String, Object> loadChecklist(String title, String username) {
        if (useDatabasePrimary) {
            Map<String, Object> checklist = databaseService.getChecklistDAO().loadChecklist(username, title);
            if (checklist != null) {
                SaveLog.getInstance()
                        .addLog("Loaded checklist from PostgreSQL database: " + title + " for user " + username);
                return checklist;
            }
        }
        // Fallback to file-based load
        return super.loadChecklist(title, username);
    }

    /**
     * Get database service for direct access
     */
    public DatabaseService getDatabaseService() {
        return databaseService;
    }

    /**
     * Enable/disable database primary mode
     */
    public void setUseDatabasePrimary(boolean useDatabasePrimary) {
        this.useDatabasePrimary = useDatabasePrimary;
    }

    /**
     * Check if database is available and primary
     */
    public boolean isDatabasePrimary() {
        return useDatabasePrimary;
    }
}