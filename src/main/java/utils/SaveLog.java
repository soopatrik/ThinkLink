package main.java.utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.FileWriter;

public class SaveLog {
	private static SaveLog instance;
	private static final String LOG_FILE = "data/save_log.txt";
	private List<String> logEntries;

	/**
	 * Private constructor to enforce singleton pattern
	 */
	private SaveLog() {
		logEntries = new ArrayList<>();
		loadLog();
	}

	/**
	 * Get the singleton instance of SaveLog
	 */
	public static synchronized SaveLog getInstance() {
		if (instance == null) {
			instance = new SaveLog();
		}
		return instance;
	}

	/**
	 * Add a new log entry with the current timestamp
	 */
	public void addLog(String message) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String timestamp = dateFormat.format(new Date());
		String logEntry = timestamp + " - " + message;

		logEntries.add(logEntry);
		saveLog();
	}

	/**
	 * Get all log entries
	 */
	public List<String> getLogEntries() {
		return new ArrayList<>(logEntries); // Return a copy
	}

	/**
	 * Clear all log entries
	 */
	public void clearLog() {
		logEntries.clear();
		saveLog();
	}

	/**
	 * Load the log from file
	 */
	private void loadLog() {
		try {
			Path logPath = Paths.get(LOG_FILE);

			// Create directory if it doesn't exist
			Files.createDirectories(logPath.getParent());

			// If file exists, load entries
			if (Files.exists(logPath)) {
				try (BufferedReader reader = new BufferedReader(new FileReader(LOG_FILE))) {
					String line;
					while ((line = reader.readLine()) != null) {
						logEntries.add(line);
					}
				}
			}
		} catch (IOException e) {
			System.err.println("Error loading log: " + e.getMessage());
		}
	}

	/**
	 * Save the log to file
	 */
	private void saveLog() {
		try {
			// Create directories if they don't exist
			Files.createDirectories(Paths.get(LOG_FILE).getParent());

			try (PrintWriter writer = new PrintWriter(new FileWriter(LOG_FILE))) {
				for (String entry : logEntries) {
					writer.println(entry);
				}
			}
		} catch (IOException e) {
			System.err.println("Error saving log: " + e.getMessage());
		}
	}

	/**
	 * Get the most recent log entries (limited to count)
	 * 
	 * @param count Maximum number of entries to return
	 * @return List of the most recent log entries
	 */
	public List<String> getRecentLogs(int count) {
		int startIndex = Math.max(0, logEntries.size() - count);
		return new ArrayList<>(logEntries.subList(startIndex, logEntries.size()));
	}

	/**
	 * Search for log entries containing the search term
	 * 
	 * @param searchTerm The term to search for
	 * @return List of matching log entries
	 */
	public List<String> searchLogs(String searchTerm) {
		List<String> results = new ArrayList<>();

		for (String entry : logEntries) {
			if (entry.toLowerCase().contains(searchTerm.toLowerCase())) {
				results.add(entry);
			}
		}

		return results;
	}
}
