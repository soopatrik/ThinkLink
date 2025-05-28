package main.java.checklist;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.io.*;
import java.nio.file.*;
import main.java.auth.User;
import main.java.network.ServerConnection;
import main.java.utils.DatabaseSaveAndLoad;
import org.json.*;

public class ChecklistPanel extends JPanel {
    private List<Checklist> checklists = new ArrayList<>();
    private Checklist currentChecklist;
    private DefaultListModel<ChecklistItem> itemsModel;
    private JList<ChecklistItem> itemsList;
    private JTextField inputField;
    private User user;
    private ServerConnection serverConnection;
    private DatabaseSaveAndLoad saveAndLoad;
    private static int nextId = 1;
    private static final String CHECKLIST_NAME = "Team Goals";
    private static final String GOALS_FILE = "data/shared_goals.json"; // Custom file for goals

    public ChecklistPanel() {
        this(null, null);
    }

    public ChecklistPanel(User user, ServerConnection serverConnection) {
        this.user = user;
        this.serverConnection = serverConnection;
        this.saveAndLoad = new DatabaseSaveAndLoad();

        setLayout(new BorderLayout());

        // Create header
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("🏆 Team Goals", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        headerPanel.add(titleLabel, BorderLayout.CENTER);

        JLabel descLabel = new JLabel("Shared goals - visible to all team members", JLabel.CENTER);
        descLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        headerPanel.add(descLabel, BorderLayout.SOUTH);

        add(headerPanel, BorderLayout.NORTH);

        // Create the list model and list component
        itemsModel = new DefaultListModel<>();
        itemsList = new JList<>(itemsModel);
        itemsList.setCellRenderer(new ChecklistRenderer());

        // Create input field for new items
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        JButton addButton = new JButton("Add Goal");

        inputPanel.add(new JLabel("New Goal: "), BorderLayout.WEST);
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(addButton, BorderLayout.EAST);

        // Action listeners
        addButton.addActionListener(e -> addGoal());
        inputField.addActionListener(e -> addGoal());

        itemsList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 1) {
                    int index = itemsList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        toggleGoalCompletion(index);
                    }
                }
            }
        });

        // Toolbar with actions
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);

        JButton clearCompletedButton = new JButton("Clear Completed");
        clearCompletedButton.addActionListener(e -> clearCompletedItems());

        JButton removeButton = new JButton("Remove Selected");
        removeButton.addActionListener(e -> removeSelectedItem());

        toolbar.add(clearCompletedButton);
        toolbar.add(removeButton);

        // Create main content panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(new JScrollPane(itemsList), BorderLayout.CENTER);
        mainPanel.add(inputPanel, BorderLayout.SOUTH);

        add(toolbar, BorderLayout.CENTER);
        add(mainPanel, BorderLayout.SOUTH);

        // Initialize checklist and load existing goals
        currentChecklist = new Checklist(CHECKLIST_NAME);
        checklists.add(currentChecklist);

        // Check database availability
        if (saveAndLoad.isDatabasePrimary()) {
            System.out.println("ChecklistPanel: Using database storage");
        } else {
            System.out.println("ChecklistPanel: Using file storage");
        }

        // ALWAYS load saved goals first using our custom JSON method
        loadGoalsFromDisk();

        // Add sample items ONLY if no goals exist at all
        if (itemsModel.isEmpty()) {
            System.out.println("No existing goals found, adding sample goals");
            addSampleItems();
            saveGoalsToDisk(); // Save sample items immediately
        } else {
            System.out.println("Loaded " + itemsModel.getSize() + " existing goals from disk");
        }
    }

    private void addGoal() {
        String goalText = inputField.getText().trim();

        if (!goalText.isEmpty()) {
            // Create a new item with unique ID
            ChecklistItem item = new ChecklistItem(nextId++, goalText, false);

            // Add to model locally
            itemsModel.addElement(item);

            // Add to current checklist
            if (currentChecklist != null) {
                currentChecklist.addItem(item);
            }

            // Save to disk immediately
            saveGoalsToDisk();

            // Send to server if connected
            if (serverConnection != null && user != null) {
                sendGoalUpdate("add", item);
            }

            // Clear input field
            inputField.setText("");

            System.out.println("Added new goal: " + goalText + " (ID: " + item.getId() + ")");
        }
    }

    private void toggleGoalCompletion(int index) {
        if (index < 0 || index >= itemsModel.getSize())
            return;

        ChecklistItem item = itemsModel.getElementAt(index);
        item.setCompleted(!item.isCompleted());

        // Update visual
        itemsModel.set(index, item);

        // Update in checklist
        if (currentChecklist != null && index < currentChecklist.getItems().size()) {
            currentChecklist.getItems().get(index).setCompleted(item.isCompleted());
        }

        // Save to disk
        saveGoalsToDisk();

        // Send to server if connected
        if (serverConnection != null && user != null) {
            sendGoalUpdate("toggle", item);
        }

        System.out.println("Toggled goal completion: " + item.getText() + " -> " + item.isCompleted());
    }

    private void removeSelectedItem() {
        int selectedIndex = itemsList.getSelectedIndex();
        if (selectedIndex >= 0) {
            ChecklistItem item = itemsModel.getElementAt(selectedIndex);
            itemsModel.remove(selectedIndex);

            // Remove from checklist
            if (currentChecklist != null) {
                currentChecklist.getItems().removeIf(i -> i.getId() == item.getId());
            }

            // Save to disk
            saveGoalsToDisk();

            // Send to server if connected
            if (serverConnection != null && user != null) {
                sendGoalUpdate("remove", item);
            }

            System.out.println("Removed goal: " + item.getText());
        }
    }

    private void clearCompletedItems() {
        List<ChecklistItem> itemsToRemove = new ArrayList<>();

        for (int i = itemsModel.getSize() - 1; i >= 0; i--) {
            ChecklistItem item = itemsModel.getElementAt(i);
            if (item.isCompleted()) {
                itemsToRemove.add(item);
                itemsModel.remove(i);
            }
        }

        // Remove from checklist
        if (currentChecklist != null) {
            for (ChecklistItem item : itemsToRemove) {
                currentChecklist.getItems().removeIf(i -> i.getId() == item.getId());
            }
        }

        // Save to disk
        saveGoalsToDisk();

        // Send removal to server for each item
        if (serverConnection != null && user != null) {
            for (ChecklistItem item : itemsToRemove) {
                sendGoalUpdate("remove", item);
            }
        }

        System.out.println("Cleared " + itemsToRemove.size() + " completed goals");
    }

    /**
     * Enhanced save method using JSON to preserve all data including IDs
     */
    private void saveGoalsToDisk() {
        try {
            // Create data directory if it doesn't exist
            Files.createDirectories(Paths.get("data"));

            JSONObject goalsData = new JSONObject();
            JSONArray goalsArray = new JSONArray();

            int maxId = 0;
            for (int i = 0; i < itemsModel.getSize(); i++) {
                ChecklistItem item = itemsModel.getElementAt(i);
                JSONObject goalJson = new JSONObject();
                goalJson.put("id", item.getId());
                goalJson.put("text", item.getText());
                goalJson.put("completed", item.isCompleted());
                goalsArray.put(goalJson);
                maxId = Math.max(maxId, item.getId());
            }

            goalsData.put("goals", goalsArray);
            goalsData.put("nextId", maxId + 1);
            goalsData.put("lastUpdated", System.currentTimeMillis());

            // Write to file
            try (FileWriter writer = new FileWriter(GOALS_FILE)) {
                writer.write(goalsData.toString(2)); // Pretty print with 2-space indent
            }

            // Update nextId
            nextId = maxId + 1;

            System.out.println("Saved " + goalsArray.length() + " goals to disk. Next ID: " + nextId);

        } catch (Exception e) {
            System.err.println("Error saving goals to disk: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Enhanced load method using JSON to preserve all data including IDs
     */
    private void loadGoalsFromDisk() {
        try {
            File goalsFile = new File(GOALS_FILE);
            if (!goalsFile.exists()) {
                System.out.println("No saved goals file found at: " + GOALS_FILE);
                return;
            }

            // Read the entire file
            String content = new String(Files.readAllBytes(Paths.get(GOALS_FILE)));
            JSONObject goalsData = new JSONObject(content);

            if (goalsData.has("goals")) {
                JSONArray goalsArray = goalsData.getJSONArray("goals");

                System.out.println("Loading " + goalsArray.length() + " goals from disk...");

                int maxId = 0;
                for (int i = 0; i < goalsArray.length(); i++) {
                    JSONObject goalJson = goalsArray.getJSONObject(i);

                    int id = goalJson.getInt("id");
                    String text = goalJson.getString("text");
                    boolean completed = goalJson.getBoolean("completed");

                    ChecklistItem item = new ChecklistItem(id, text, completed);
                    itemsModel.addElement(item);

                    if (currentChecklist != null) {
                        currentChecklist.addItem(item);
                    }

                    maxId = Math.max(maxId, id);
                    System.out.println("  Loaded: " + text + " (ID: " + id + ", completed: " + completed + ")");
                }

                // Set nextId from saved data or calculate from max
                if (goalsData.has("nextId")) {
                    nextId = goalsData.getInt("nextId");
                } else {
                    nextId = maxId + 1;
                }

                System.out.println("Successfully loaded " + goalsArray.length() + " goals. Next ID: " + nextId);
            }

        } catch (Exception e) {
            System.err.println("Error loading goals from disk: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendGoalUpdate(String action, ChecklistItem item) {
        try {
            JSONObject message = new JSONObject();
            message.put("type", "goal_update");
            message.put("action", action);
            message.put("userEmail", user.getUserEmail());
            message.put("goalId", item.getId());
            message.put("text", item.getText());
            message.put("completed", item.isCompleted());

            serverConnection.sendMessage(message);
        } catch (Exception e) {
            System.err.println("Error sending goal update: " + e.getMessage());
        }
    }

    public void handleServerMessage(JSONObject message) {
        try {
            String action = message.getString("action");
            int goalId = message.getInt("goalId");
            String text = message.getString("text");
            boolean completed = message.getBoolean("completed");

            switch (action) {
                case "add":
                    ChecklistItem newItem = new ChecklistItem(goalId, text, completed);
                    // Check if item already exists
                    boolean exists = false;
                    for (int i = 0; i < itemsModel.getSize(); i++) {
                        if (itemsModel.getElementAt(i).getId() == goalId) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        itemsModel.addElement(newItem);
                        if (currentChecklist != null) {
                            currentChecklist.addItem(newItem);
                        }
                        // Save when receiving from server
                        saveGoalsToDisk();
                        System.out.println("Received new goal from server: " + text);
                    }
                    break;

                case "toggle":
                    for (int i = 0; i < itemsModel.getSize(); i++) {
                        ChecklistItem item = itemsModel.getElementAt(i);
                        if (item.getId() == goalId) {
                            item.setCompleted(completed);
                            itemsModel.set(i, item);
                            // Save when receiving from server
                            saveGoalsToDisk();
                            System.out.println("Updated goal from server: " + text + " -> " + completed);
                            break;
                        }
                    }
                    break;

                case "remove":
                    for (int i = 0; i < itemsModel.getSize(); i++) {
                        if (itemsModel.getElementAt(i).getId() == goalId) {
                            itemsModel.remove(i);
                            if (currentChecklist != null) {
                                currentChecklist.getItems().removeIf(item -> item.getId() == goalId);
                            }
                            // Save when receiving from server
                            saveGoalsToDisk();
                            System.out.println("Removed goal from server: " + text);
                            break;
                        }
                    }
                    break;
            }
        } catch (Exception e) {
            System.err.println("Error handling goal message: " + e.getMessage());
        }
    }

    private void addSampleItems() {
        ChecklistItem item1 = new ChecklistItem(nextId++, "Create project report", false);
        ChecklistItem item2 = new ChecklistItem(nextId++, "Design user interface", false);
        ChecklistItem item3 = new ChecklistItem(nextId++, "Implement core functionality", false);

        itemsModel.addElement(item1);
        itemsModel.addElement(item2);
        itemsModel.addElement(item3);

        if (currentChecklist != null) {
            currentChecklist.addItem(item1);
            currentChecklist.addItem(item2);
            currentChecklist.addItem(item3);
        }
    }

    // Enhanced ChecklistItem class with ID
    public static class ChecklistItem {
        private int id;
        private String text;
        private boolean completed;

        public ChecklistItem(int id, String text, boolean completed) {
            this.id = id;
            this.text = text;
            this.completed = completed;
        }

        public int getId() {
            return id;
        }

        public String getText() {
            return text;
        }

        public boolean isCompleted() {
            return completed;
        }

        public void setCompleted(boolean completed) {
            this.completed = completed;
        }

        @Override
        public String toString() {
            return text + (completed ? " ✓" : "");
        }
    }

    // Custom renderer for checklist items
    private class ChecklistRenderer extends JCheckBox implements ListCellRenderer<ChecklistItem> {
        @Override
        public Component getListCellRendererComponent(
                JList<? extends ChecklistItem> list, ChecklistItem value,
                int index, boolean isSelected, boolean cellHasFocus) {

            setText(value.getText());
            setSelected(value.isCompleted());

            setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
            setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());

            return this;
        }
    }
}
