package main.java.utils;

import java.io.*;
import org.json.*;
import main.java.board.*;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.NoSuchFileException;

public class SharedState {
    private static final String APP_DATA_DIR_NAME = ".thinklink";
    private static final String SHARED_BOARD_FILE_NAME = "shared_board.json";
    private static final Path SHARED_BOARD_PATH;

    static {
        Path tempPath = null;
        System.out.println("SharedState static initializer: Starting.");
        try {
            String userHome = System.getProperty("user.home");
            if (userHome == null || userHome.trim().isEmpty()) {
                System.err.println(
                        "SharedState static initializer: ERROR - System.getProperty(\"user.home\") returned null or empty.");
            } else {
                Path appDataDir = Paths.get(userHome, APP_DATA_DIR_NAME);
                if (!Files.exists(appDataDir)) {
                    try {
                        Files.createDirectories(appDataDir);
                        System.out.println(
                                "SharedState static initializer: Created application data directory: " + appDataDir);
                    } catch (IOException e) {
                        System.err.println(
                                "SharedState static initializer: ERROR creating application data directory: "
                                        + appDataDir
                                        + " - " + e.getMessage());
                    }
                }
                if (Files.exists(appDataDir)) {
                    tempPath = appDataDir.resolve(SHARED_BOARD_FILE_NAME);
                    System.out.println(
                            "SharedState static initializer: Successfully determined SHARED_BOARD_PATH: " + tempPath);
                } else {
                    System.err.println(
                            "SharedState static initializer: ERROR - App data directory does not exist and could not be created: "
                                    + appDataDir);
                }
            }
        } catch (Throwable t) {
            System.err.println("SharedState static initializer: CRITICAL UNHANDLED ERROR during path initialization.");
            t.printStackTrace();
        }
        SHARED_BOARD_PATH = tempPath;
        if (SHARED_BOARD_PATH == null) {
            System.err.println(
                    "SharedState static initializer: CRITICAL - SHARED_BOARD_PATH is NULL after initialization attempt!");
        } else {
            System.out.println(
                    "SharedState static initializer: Final SHARED_BOARD_PATH is: " + SHARED_BOARD_PATH.toString());
            if (!Files.exists(SHARED_BOARD_PATH)) {
                System.out.println("SharedState static initializer: Shared board file does not exist at "
                        + SHARED_BOARD_PATH + ". Initializing with an empty board.");
                JSONObject emptyBoard = new JSONObject();
                emptyBoard.put("boxes", new JSONArray());
                emptyBoard.put("lastUpdated", System.currentTimeMillis());
                saveSharedBoardInternally(emptyBoard);
            }
        }
    }

    // Save the shared board state
    public static synchronized void saveSharedBoard(BoxList boxList) {
        try {
            JSONObject boardData = new JSONObject();
            boardData.put("lastUpdated", System.currentTimeMillis());

            // Add boxes
            JSONArray boxesArray = new JSONArray();
            Box currentBox = boxList.getFirstNode();
            while (currentBox != null) {
                JSONObject boxJson = new JSONObject();
                boxJson.put("id", currentBox.getId());
                boxJson.put("title", currentBox.getTitle());
                boxJson.put("content", currentBox.getContent());
                boxJson.put("x", currentBox.getBoxX());
                boxJson.put("y", currentBox.getBoxY());

                // Add connections
                JSONArray connectionsArray = new JSONArray();
                for (Integer connectionId : currentBox.getConnectedBoxIds()) {
                    connectionsArray.put(connectionId);
                }
                boxJson.put("connections", connectionsArray);

                boxesArray.put(boxJson);
                currentBox = currentBox.getNext();
            }
            boardData.put("boxes", boxesArray);

            try (BufferedWriter writer = Files.newBufferedWriter(SHARED_BOARD_PATH)) {
                writer.write(boardData.toString(2));
                System.out.println("Shared board saved successfully to: " + SHARED_BOARD_PATH.toString() + " with "
                        + boxesArray.length() + " boxes.");
            }

        } catch (Exception e) {
            System.err.println("Error saving shared board to " + SHARED_BOARD_PATH.toString() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Load the shared board state
    public static JSONObject loadSharedBoard() {
        if (SHARED_BOARD_PATH == null) {
            System.err.println("SharedState.loadSharedBoard: CRITICAL - SHARED_BOARD_PATH is null. Cannot load.");
            JSONObject errorBoard = new JSONObject();
            errorBoard.put("boxes", new JSONArray());
            errorBoard.put("error", "Shared board path not initialized");
            errorBoard.put("lastUpdated", System.currentTimeMillis());
            return errorBoard;
        }

        try {
            if (!Files.exists(SHARED_BOARD_PATH)) {
                System.out.println("SharedState.loadSharedBoard: File not found at " + SHARED_BOARD_PATH
                        + ". Returning default empty board structure.");
                JSONObject newBoard = new JSONObject();
                newBoard.put("boxes", new JSONArray());
                newBoard.put("lastUpdated", System.currentTimeMillis());
                saveSharedBoardInternally(newBoard);
                return newBoard;
            }

            String content = new String(Files.readAllBytes(SHARED_BOARD_PATH));
            if (content.trim().isEmpty()) {
                System.out.println("SharedState.loadSharedBoard: File is empty at " + SHARED_BOARD_PATH
                        + ". Returning default empty board structure.");
                JSONObject emptyBoard = new JSONObject();
                emptyBoard.put("boxes", new JSONArray());
                emptyBoard.put("lastUpdated", System.currentTimeMillis());
                saveSharedBoardInternally(emptyBoard);
                return emptyBoard;
            }
            JSONObject board = new JSONObject(content);
            int boxCount = 0;
            if (board.has("boxes") && board.get("boxes") instanceof JSONArray) {
                boxCount = board.getJSONArray("boxes").length();
            }
            System.out.println("SharedState.loadSharedBoard: Shared board loaded successfully from: "
                    + SHARED_BOARD_PATH + ". Contains " + boxCount + " boxes.");
            return board;
        } catch (NoSuchFileException e) {
            System.out.println("SharedState.loadSharedBoard: NoSuchFileException for " + SHARED_BOARD_PATH
                    + ". Returning default empty board.");
            JSONObject newBoard = new JSONObject();
            newBoard.put("boxes", new JSONArray());
            newBoard.put("lastUpdated", System.currentTimeMillis());
            saveSharedBoardInternally(newBoard);
            return newBoard;
        } catch (IOException e) {
            System.err.println("SharedState.loadSharedBoard: IOException while reading shared board from "
                    + SHARED_BOARD_PATH + " - " + e.getMessage());
            e.printStackTrace();
            return createErrorBoard("IOException during load: " + e.getMessage());
        } catch (JSONException e) {
            System.err.println("SharedState.loadSharedBoard: JSONException while parsing shared board from "
                    + SHARED_BOARD_PATH + " - " + e.getMessage());
            e.printStackTrace();
            return createErrorBoard("JSONException during load: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("SharedState.loadSharedBoard: Unexpected error loading shared board: " + e.getMessage());
            e.printStackTrace();
            return createErrorBoard("Unexpected error during load: " + e.getMessage());
        }
    }

    private static JSONObject createErrorBoard(String errorMessage) {
        JSONObject errorBoard = new JSONObject();
        errorBoard.put("boxes", new JSONArray());
        errorBoard.put("error", errorMessage);
        errorBoard.put("lastUpdated", System.currentTimeMillis());
        return errorBoard;
    }

    // Renamed to avoid conflict if you have another public saveSharedBoard
    private static synchronized void saveSharedBoardInternally(JSONObject boardState) {
        if (SHARED_BOARD_PATH == null) {
            System.err.println(
                    "SharedState.saveSharedBoardInternally: CRITICAL - SHARED_BOARD_PATH is null. Cannot save.");
            return;
        }
        try (FileWriter file = new FileWriter(SHARED_BOARD_PATH.toFile())) {
            file.write(boardState.toString(4));
            file.flush();
            int boxCount = 0;
            if (boardState.has("boxes") && boardState.get("boxes") instanceof JSONArray) {
                boxCount = boardState.getJSONArray("boxes").length();
            }
            System.out
                    .println("SharedState.saveSharedBoardInternally (JSONObject): Successfully saved shared board to: "
                            + SHARED_BOARD_PATH + ". Boxes: " + boxCount);
        } catch (IOException e) {
            System.err.println("SharedState.saveSharedBoardInternally (JSONObject): IOException while saving to "
                    + SHARED_BOARD_PATH + " - " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println(
                    "SharedState.saveSharedBoardInternally (JSONObject): Unexpected error while saving: "
                            + e.getMessage());
            e.printStackTrace();
        }
    }

    // Public method for clients (e.g., BoardPanel) to save their state
    public static synchronized void saveSharedBoard(JSONArray boxesArray) {
        JSONObject boardState = new JSONObject();
        boardState.put("boxes", boxesArray);
        boardState.put("lastUpdated", System.currentTimeMillis());
        System.out.println(
                "SharedState.saveSharedBoard (JSONArray): Preparing to save. Boxes count: " + boxesArray.length());
        saveSharedBoardInternally(boardState);
    }

    // Method for the server (ClientHandler) to update and save state
    public static synchronized void updateServerBoardState(String boardId, JSONObject newOrUpdatedBoxJSON) {
        if (boardId == null || (!"shared-global-board".equals(boardId) && !"global-shared-board".equals(boardId))) {
            System.err.println("SharedState.updateServerBoardState: Invalid or unsupported boardId: " + boardId
                    + ". Currently only supports 'shared-global-board'.");
            return;
        }

        JSONObject boardState = loadSharedBoard();
        JSONArray boxes;
        if (boardState.has("boxes") && boardState.get("boxes") instanceof JSONArray) {
            boxes = boardState.getJSONArray("boxes");
        } else {
            System.out.println(
                    "SharedState.updateServerBoardState: 'boxes' array missing or not a JSONArray. Initializing new one.");
            boxes = new JSONArray();
            boardState.put("boxes", boxes);
        }

        int boxIdToUpdate = newOrUpdatedBoxJSON.getInt("id");
        boolean boxFoundAndUpdated = false;
        for (int i = 0; i < boxes.length(); i++) {
            if (boxes.getJSONObject(i).optInt("id", -1) == boxIdToUpdate) {
                boxes.put(i, newOrUpdatedBoxJSON); // Replace existing box
                boxFoundAndUpdated = true;
                System.out
                        .println("SharedState.updateServerBoardState: Updated existing box with ID: " + boxIdToUpdate);
                break;
            }
        }

        if (!boxFoundAndUpdated) {
            boxes.put(newOrUpdatedBoxJSON); // Add as new box if not found
            System.out.println("SharedState.updateServerBoardState: Added new box with ID: " + boxIdToUpdate
                    + " as it was not found for update.");
        }

        boardState.put("lastUpdated", System.currentTimeMillis());
        saveSharedBoardInternally(boardState);
        System.out.println("SharedState.updateServerBoardState: Board saved. Total boxes: " + boxes.length());
    }

    public static void addConnectionToServerState(String boardId, int sourceBoxId, int targetBoxId) {
        try {
            JSONObject boardState = loadSharedBoard();
            if (boardState != null && boardState.has("boxes")) {
                JSONArray boxes = boardState.getJSONArray("boxes");

                // Find the source box and add the connection
                for (int i = 0; i < boxes.length(); i++) {
                    JSONObject box = boxes.getJSONObject(i);
                    if (box.getInt("id") == sourceBoxId) {
                        JSONArray connections = box.optJSONArray("connections");
                        if (connections == null) {
                            connections = new JSONArray();
                        }

                        // Add connection if not already present
                        boolean connectionExists = false;
                        for (int j = 0; j < connections.length(); j++) {
                            if (connections.getInt(j) == targetBoxId) {
                                connectionExists = true;
                                break;
                            }
                        }

                        if (!connectionExists) {
                            connections.put(targetBoxId);
                            box.put("connections", connections);
                        }
                        break;
                    }
                }

                saveSharedBoard(boxes);
                System.out.println("SharedState.addConnectionToServerState: Added connection from " + sourceBoxId
                        + " to " + targetBoxId);
            }
        } catch (Exception e) {
            System.err.println("Error adding connection to server state: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void removeConnectionFromServerState(String boardId, int sourceBoxId, int targetBoxId) {
        try {
            JSONObject currentBoard = loadSharedBoard();
            JSONArray boxes = currentBoard.optJSONArray("boxes");
            if (boxes != null) {
                for (int i = 0; i < boxes.length(); i++) {
                    JSONObject box = boxes.optJSONObject(i);
                    if (box != null && box.getInt("id") == sourceBoxId) {
                        JSONArray connections = box.optJSONArray("connections");
                        if (connections != null) {
                            for (int j = connections.length() - 1; j >= 0; j--) {
                                if (connections.getInt(j) == targetBoxId) {
                                    connections.remove(j);
                                    break;
                                }
                            }
                        }
                        break;
                    }
                }
                currentBoard.put("lastUpdated", System.currentTimeMillis());
                saveSharedBoardInternally(currentBoard);
                System.out.println("SharedState.removeConnectionFromServerState: Removed connection " + sourceBoxId
                        + " -> " + targetBoxId);
            }
        } catch (Exception e) {
            System.err.println(
                    "SharedState.removeConnectionFromServerState: Error removing connection: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static synchronized void removeBoxFromServerState(String boardId, int boxId) {
        try {
            JSONObject currentBoard = loadSharedBoard();
            JSONArray boxes = currentBoard.optJSONArray("boxes");
            if (boxes != null) {
                // Remove the box and all references to it
                for (int i = boxes.length() - 1; i >= 0; i--) {
                    JSONObject box = boxes.optJSONObject(i);
                    if (box != null) {
                        if (box.getInt("id") == boxId) {
                            boxes.remove(i);
                        } else {
                            // Remove this boxId from other boxes' connections
                            JSONArray connections = box.optJSONArray("connections");
                            if (connections != null) {
                                for (int j = connections.length() - 1; j >= 0; j--) {
                                    if (connections.getInt(j) == boxId) {
                                        connections.remove(j);
                                    }
                                }
                            }
                        }
                    }
                }
                currentBoard.put("lastUpdated", System.currentTimeMillis());
                saveSharedBoardInternally(currentBoard);
                System.out.println("SharedState.removeBoxFromServerState: Removed box " + boxId);
            }
        } catch (Exception e) {
            System.err.println(
                    "SharedState.removeBoxFromServerState: Error removing box " + boxId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}