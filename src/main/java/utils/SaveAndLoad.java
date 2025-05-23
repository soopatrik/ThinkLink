package main.java.utils;

/////////////////////////////////////////////////////
//////Class for Saving and Loading the MindMap///////
/////////////////////////////////////////////////////

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.json.*;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

import main.java.board.Box;
import main.java.board.BoxList;

/**
 * Simplified SaveAndLoad class focused on shared board state
 */
public class SaveAndLoad {
	private static final String DATA_DIR = "data";
	private static final String SHARED_BOARD_FILE = DATA_DIR + "/shared_board.json";
	private static final String BOARDS_DIR = DATA_DIR + "/boards";
	private static final String NOTES_DIR = DATA_DIR + "/notes";
	private static final String CHECKLISTS_DIR = DATA_DIR + "/checklists";
	private static final String CALENDAR_DIR = DATA_DIR + "/calendar";

	// Constructor - ensure data directory exists
	public SaveAndLoad() {
		try {
			Files.createDirectories(Paths.get(DATA_DIR));
		} catch (IOException e) {
			System.err.println("Error creating data directory: " + e.getMessage());
		}
	}

	/**
	 * Saves the shared board state that all users can access
	 */
	public void saveSharedBoard(BoxList boxList) {
		try {
			JSONObject boardData = new JSONObject();
			boardData.put("lastUpdated", System.currentTimeMillis());

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
			try (FileWriter writer = new FileWriter(SHARED_BOARD_FILE)) {
				writer.write(boardData.toString(2));
				System.out.println("Shared board saved with " + boxCount + " boxes");
			}

		} catch (Exception e) {
			System.err.println("Error saving shared board: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Loads the shared board state
	 */
	public JSONObject loadSharedBoard() {
		try {
			File boardFile = new File(SHARED_BOARD_FILE);
			if (!boardFile.exists()) {
				System.out.println("No shared board file exists yet");
				return null;
			}

			StringBuilder content = new StringBuilder();
			try (BufferedReader reader = new BufferedReader(new FileReader(SHARED_BOARD_FILE))) {
				String line;
				while ((line = reader.readLine()) != null) {
					content.append(line);
				}
			}

			JSONObject boardData = new JSONObject(content.toString());
			System.out.println("Shared board loaded successfully");
			return boardData;

		} catch (Exception e) {
			System.err.println("Error loading shared board: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Saves a board to the specified file using the original format
	 */
	public void saveMap(String fileName, BoxList list) {
		try {
			String filePath = BOARDS_DIR + "/" + sanitizeFileName(fileName) + ".txt";

			try (PrintWriter file = new PrintWriter(new FileWriter(filePath))) {
				Box current = list.getFirstNode();
				while (current != null) {
					file.println("");
					file.println(current.getID()); // ID

					// Write title
					if (current.getTitle() != null && !current.getTitle().trim().isEmpty()) {
						file.println(current.getTitle()); // Title
					} else {
						file.println("default");
					}

					// Write body/description
					if (current.getBody() != null && !current.getBody().trim().isEmpty()) {
						file.println(current.getBody()); // Description
					} else {
						file.println("Please Add Description");
					}

					file.println(current.getBoxX()); // Box X
					file.println(current.getBoxY()); // Box Y

					// Write connections
					List<Integer> connections = current.getConnectedBoxIds();
					file.println(connections.size()); // Length of arrays

					for (Integer connectedId : connections) {
						file.println(connectedId);
					}

					current = current.getNext();
				}

				// Log the save
				SaveLog.getInstance().addLog("Saved board: " + fileName);
			}
		} catch (IOException e) {
			System.err.println("Error saving board: " + e.getMessage());
			JOptionPane.showMessageDialog(null,
					"Could not save board: " + e.getMessage(),
					"Save Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * Loads a board from file using the original format
	 */
	public void readMap(String fileName, BoxList list) {
		String filePath = BOARDS_DIR + "/" + sanitizeFileName(fileName) + ".txt";

		try {
			// Clear the existing list
			list.clearList();

			// FileReader reads text files in the default encoding
			try (BufferedReader bufferedReader = new BufferedReader(new FileReader(filePath))) {
				String line;

				while ((line = bufferedReader.readLine()) != null) {
					Box newBox = new Box();
					newBox.setID(Integer.parseInt(bufferedReader.readLine())); // ID
					newBox.setTitle(bufferedReader.readLine()); // Title
					newBox.setBody(bufferedReader.readLine()); // Description
					newBox.setBoxX(Integer.parseInt(bufferedReader.readLine())); // BoxX
					newBox.setBoxY(Integer.parseInt(bufferedReader.readLine())); // BoxY

					// Number of Boxes it connects to
					int connectionCount = Integer.parseInt(bufferedReader.readLine());

					// Repeats "length of the array list" times
					for (int i = 0; i < connectionCount; i++) {
						newBox.addConnection(Integer.parseInt(bufferedReader.readLine()));
					}

					// Adds object to the linked list to store it in memory
					list.addNode(newBox);
				}
			}

			// Resets the "graphical" connections for box
			Box currentBox = list.getFirstNode();
			int boxIndex = 0;
			while (currentBox != null) {
				List<Integer> connections = currentBox.getConnectedBoxIds();

				for (Integer connectedId : connections) {
					// Use getBoxById instead of getNode(connectedId)
					Box connectedBox = list.getBoxById(connectedId);
					if (connectedBox != null) {
						connectedBox.setIsConnectedBy(boxIndex);
					}
				}

				currentBox = currentBox.getNext();
				boxIndex++;
			}

			// Log the load
			SaveLog.getInstance().addLog("Loaded board: " + fileName);

		} catch (FileNotFoundException ex) {
			System.err.println("Unable to open file '" + filePath + "'");
			SaveLog.getInstance().addLog("Failed to load non-existent board: " + fileName);
		} catch (IOException ex) {
			System.err.println("Error reading file '" + filePath + "'");
			SaveLog.getInstance().addLog("Error reading board: " + fileName);
		} catch (NumberFormatException ex) {
			System.err.println("Error parsing number in file '" + filePath + "'");
			SaveLog.getInstance().addLog("Error parsing board file: " + fileName);
		}
	}

	/**
	 * Grabs the buffered image from render panel and saves it as a .jpg file
	 */
	public void saveMapAsImage(String fileName, BufferedImage image) {
		try {
			// Clean up filename
			fileName = sanitizeFileName(fileName);
			while (fileName.endsWith(".")) {
				fileName = fileName.substring(0, fileName.length() - 1);
			}

			// Error trapping; No Name
			if (fileName.trim().isEmpty()) {
				fileName = "NoName";
			}

			// Ensure directory exists
			Files.createDirectories(Paths.get(BOARDS_DIR));

			// Save the image
			String filePath = BOARDS_DIR + "/" + fileName + ".jpg";
			File f = new File(filePath);

			if (!ImageIO.write(image, "JPEG", f)) {
				throw new RuntimeException("Unexpected error writing image");
			}

			// Log the save
			SaveLog.getInstance().addLog("Saved board image: " + fileName);

		} catch (IOException e) {
			System.err.println("Error saving image: " + e.getMessage());
			JOptionPane.showMessageDialog(null,
					"Could not save image: " + e.getMessage(),
					"Save Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * Saves a note to the specified file
	 */
	public boolean saveNote(String title, String content, String username) {
		try {
			String userDir = NOTES_DIR + "/" + sanitizeFileName(username);
			Files.createDirectories(Paths.get(userDir));

			String filePath = userDir + "/" + sanitizeFileName(title) + ".note";

			try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
				writer.println(title);
				writer.println("---"); // Separator
				writer.print(content);

				// Log the save
				SaveLog.getInstance().addLog("Saved note: " + title + " for user " + username);
				return true;
			}
		} catch (IOException e) {
			System.err.println("Error saving note: " + e.getMessage());
			return false;
		}
	}

	/**
	 * Loads a note for the specified user
	 */
	public Map<String, String> loadNote(String title, String username) {
		try {
			String filePath = NOTES_DIR + "/" + sanitizeFileName(username) +
					"/" + sanitizeFileName(title) + ".note";

			StringBuilder content = new StringBuilder();
			String loadedTitle = "";

			try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
				loadedTitle = reader.readLine();

				// Skip separator line
				reader.readLine();

				// Read all remaining lines as content
				String line;
				while ((line = reader.readLine()) != null) {
					content.append(line).append("\n");
				}

				// Log the load
				SaveLog.getInstance().addLog("Loaded note: " + title + " for user " + username);

				Map<String, String> note = new HashMap<>();
				note.put("title", loadedTitle);
				note.put("content", content.toString());
				return note;
			}
		} catch (IOException e) {
			System.err.println("Error loading note: " + e.getMessage());
			return null;
		}
	}

	/**
	 * Gets a list of all note titles for a user
	 */
	public List<String> getNoteList(String username) {
		List<String> notes = new ArrayList<>();

		try {
			String userDir = NOTES_DIR + "/" + sanitizeFileName(username);
			Path dirPath = Paths.get(userDir);

			if (Files.exists(dirPath)) {
				try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath, "*.note")) {
					for (Path path : stream) {
						String fileName = path.getFileName().toString();
						notes.add(fileName.substring(0, fileName.length() - 5)); // Remove ".note"
					}
				}
			}
		} catch (IOException e) {
			System.err.println("Error listing notes: " + e.getMessage());
		}

		return notes;
	}

	/**
	 * Saves a checklist
	 */
	public boolean saveChecklist(String title, List<Map<String, Object>> items, String username) {
		try {
			String userDir = CHECKLISTS_DIR + "/" + sanitizeFileName(username);
			Files.createDirectories(Paths.get(userDir));

			String filePath = userDir + "/" + sanitizeFileName(title) + ".checklist";

			try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
				writer.println(title);
				writer.println("---"); // Separator

				// Write each checklist item
				for (Map<String, Object> item : items) {
					writer.println(item.get("text") + "," + item.get("completed"));
				}

				SaveLog.getInstance().addLog("Saved checklist: " + title + " for user " + username);
				return true;
			}
		} catch (IOException e) {
			System.err.println("Error saving checklist: " + e.getMessage());
			return false;
		}
	}

	/**
	 * Loads a checklist
	 */
	public Map<String, Object> loadChecklist(String title, String username) {
		try {
			String filePath = CHECKLISTS_DIR + "/" + sanitizeFileName(username) +
					"/" + sanitizeFileName(title) + ".checklist";

			String loadedTitle = "";
			List<Map<String, Object>> items = new ArrayList<>();

			try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
				loadedTitle = reader.readLine();

				// Skip separator
				reader.readLine();

				// Read items
				String line;
				while ((line = reader.readLine()) != null) {
					String[] parts = line.split(",", 2);
					if (parts.length == 2) {
						Map<String, Object> item = new HashMap<>();
						item.put("text", parts[0]);
						item.put("completed", Boolean.parseBoolean(parts[1]));
						items.add(item);
					}
				}

				SaveLog.getInstance().addLog("Loaded checklist: " + title + " for user " + username);

				Map<String, Object> result = new HashMap<>();
				result.put("title", loadedTitle);
				result.put("items", items);
				return result;
			}
		} catch (IOException e) {
			System.err.println("Error loading checklist: " + e.getMessage());
			return null;
		}
	}

	/**
	 * Sanitizes a filename by removing invalid characters
	 */
	private String sanitizeFileName(String input) {
		return input.replaceAll("[^a-zA-Z0-9.-]", "_");
	}

	/**
	 * Gets a list of all saved boards
	 */
	public List<String> getBoardList() {
		List<String> boards = new ArrayList<>();

		try {
			Path dirPath = Paths.get(BOARDS_DIR);

			if (Files.exists(dirPath)) {
				try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath, "*.txt")) {
					for (Path path : stream) {
						String fileName = path.getFileName().toString();
						boards.add(fileName.substring(0, fileName.length() - 4)); // Remove ".txt"
					}
				}
			}
		} catch (IOException e) {
			System.err.println("Error listing boards: " + e.getMessage());
		}

		return boards;
	}

	/**
	 * Deletes a board file
	 */
	public boolean deleteBoard(String boardName) {
		try {
			String filePath = BOARDS_DIR + "/" + sanitizeFileName(boardName) + ".txt";
			Files.deleteIfExists(Paths.get(filePath));

			// Also try to delete any associated image
			String imagePath = BOARDS_DIR + "/" + sanitizeFileName(boardName) + ".jpg";
			Files.deleteIfExists(Paths.get(imagePath));

			SaveLog.getInstance().addLog("Deleted board: " + boardName);
			return true;
		} catch (IOException e) {
			System.err.println("Error deleting board: " + e.getMessage());
			return false;
		}
	}

	public boolean deleteNote(String title, String username) {
		try {
			String filePath = NOTES_DIR + "/" + username + "_" + sanitizeFileName(title) + ".txt";
			File file = new File(filePath);
			boolean result = file.delete();
			if (result) {
				SaveLog.getInstance().addLog("Deleted note: " + title);
			}
			return result;
		} catch (Exception e) {
			System.err.println("Error deleting note: " + e.getMessage());
			return false;
		}
	}

	// Similar methods for checklists and calendar events
}
