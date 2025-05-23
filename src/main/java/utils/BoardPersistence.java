package main.java.utils;

import java.io.*;
import java.util.*;
import main.java.board.*;
import org.json.*;

public class BoardPersistence {
    private static final String SAVE_DIR = "saved_boards";

    // Save a board to disk
    public static void saveBoard(String boardId, BoxList boxList, String owner) {
        try {
            System.out.println("BoardPersistence: Saving board " + boardId);

            // Create directory if it doesn't exist
            File saveDir = new File(SAVE_DIR);
            if (!saveDir.exists()) {
                boolean created = saveDir.mkdirs();
                System.out.println("Created save directory: " + created);
            }

            // Create JSON representation of the board
            JSONObject boardData = new JSONObject();
            boardData.put("boardId", boardId);
            boardData.put("owner", owner);
            boardData.put("lastModified", System.currentTimeMillis());

            // Add boxes
            JSONArray boxesArray = new JSONArray();
            Box currentBox = boxList.getFirstNode();
            int boxCount = 0;

            while (currentBox != null) {
                JSONObject boxJson = new JSONObject();
                boxJson.put("id", currentBox.getId());
                boxJson.put("title", currentBox.getTitle());
                boxJson.put("x", currentBox.getBoxX());
                boxJson.put("y", currentBox.getBoxY());

                // Add content
                try {
                    boxJson.put("content", currentBox.getContent());
                } catch (Exception e) {
                    System.out.println("Warning: Could not save box content: " + e.getMessage());
                    boxJson.put("content", "");
                }

                // Add connections
                JSONArray connectionsArray = new JSONArray();
                for (Integer connectionId : currentBox.getConnectedBoxIds()) {
                    connectionsArray.put(connectionId);
                }
                boxJson.put("connections", connectionsArray);

                boxesArray.put(boxJson);
                currentBox = currentBox.getNext();
                boxCount++;
            }
            boardData.put("boxes", boxesArray);

            // Write to file
            String fileName = SAVE_DIR + "/" + boardId + ".json";
            System.out.println("Saving to file: " + fileName + " with " + boxCount + " boxes");

            try (FileWriter writer = new FileWriter(fileName)) {
                writer.write(boardData.toString(2));
            }

            System.out.println("Board saved successfully: " + boardId);
        } catch (Exception e) {
            System.out.println("Error saving board: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Load a board from disk
    public static JSONObject loadBoard(String boardId) {
        try {
            String fileName = SAVE_DIR + "/" + boardId + ".json";
            File boardFile = new File(fileName);

            if (!boardFile.exists()) {
                return null;
            }

            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line);
                }
            }

            JSONObject boardData = new JSONObject(content.toString());
            System.out.println("Board loaded: " + boardId);
            return boardData;
        } catch (Exception e) {
            System.out.println("Error loading board: " + e.getMessage());
            return null;
        }
    }

    // Get list of all saved boards
    public static List<String> getSavedBoards() {
        List<String> boardIds = new ArrayList<>();

        File saveDir = new File(SAVE_DIR);
        if (saveDir.exists() && saveDir.isDirectory()) {
            File[] files = saveDir.listFiles((dir, name) -> name.endsWith(".json"));

            if (files != null) {
                for (File file : files) {
                    String boardId = file.getName().replace(".json", "");
                    boardIds.add(boardId);
                }
            }
        }

        return boardIds;
    }
}