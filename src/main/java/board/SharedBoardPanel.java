package main.java.board;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.awt.Frame;
import java.awt.Window;
import main.java.auth.User;
import main.java.calendar.Deadline;
import main.java.calendar.Date;

public class SharedBoardPanel extends JPanel {
    private User user;
    private List<SharedBoard> sharedBoards;
    private JList<SharedBoard> boardList;
    private DefaultListModel<SharedBoard> boardListModel;
    private JPanel taskPanel;
    private SharedBoard currentBoard;

    public SharedBoardPanel(User user) {
        this.user = user;
        this.sharedBoards = new ArrayList<>();

        // Verify this is an administrator
        if (!user.isAdministrator()) {
            add(new JLabel("Only administrators can access shared boards"));
            return;
        }

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create board list panel
        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.setBorder(BorderFactory.createTitledBorder("Shared Boards"));

        boardListModel = new DefaultListModel<>();
        boardList = new JList<>(boardListModel);
        boardList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        boardList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                displaySelectedBoard();
            }
        });

        JScrollPane boardScrollPane = new JScrollPane(boardList);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton newBoardButton = new JButton("New Board");
        JButton deleteBoardButton = new JButton("Delete Board");

        newBoardButton.addActionListener(e -> createNewBoard());
        deleteBoardButton.addActionListener(e -> deleteSelectedBoard());

        buttonPanel.add(newBoardButton);
        buttonPanel.add(deleteBoardButton);

        leftPanel.add(boardScrollPane, BorderLayout.CENTER);
        leftPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Create task panel
        taskPanel = new JPanel(new BorderLayout(5, 5));
        taskPanel.setBorder(BorderFactory.createTitledBorder("Tasks"));

        // Create initial empty panel
        JPanel emptyPanel = new JPanel(new GridBagLayout());
        JLabel emptyLabel = new JLabel("Select a board or create a new one");
        emptyPanel.add(emptyLabel);
        taskPanel.add(emptyPanel, BorderLayout.CENTER);

        // Create split pane
        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                leftPanel,
                taskPanel);
        splitPane.setDividerLocation(250);

        add(splitPane, BorderLayout.CENTER);

        // Add some sample data for demonstration
        addSampleData();
    }

    private void addSampleData() {
        SharedBoard projectBoard = new SharedBoard("Project Planning", user);
        SharedBoard teamBoard = new SharedBoard("Team Coordination", user);

        LiveTask task1 = new LiveTask("Complete UML diagram");
        task1.setStatus(LiveTask.STATUS_COMPLETED);

        LiveTask task2 = new LiveTask("Implement user authentication");
        task2.setStatus(LiveTask.STATUS_IN_PROGRESS);

        LiveTask task3 = new LiveTask("Create shared board functionality");

        projectBoard.addTask(task1);
        projectBoard.addTask(task2);
        projectBoard.addTask(task3);

        sharedBoards.add(projectBoard);
        sharedBoards.add(teamBoard);

        // Update the list model
        boardListModel.addElement(projectBoard);
        boardListModel.addElement(teamBoard);
    }

    private void createNewBoard() {
        String boardName = JOptionPane.showInputDialog(this,
                "Enter name for new shared board:",
                "Create Shared Board",
                JOptionPane.PLAIN_MESSAGE);

        if (boardName != null && !boardName.trim().isEmpty()) {
            SharedBoard newBoard = new SharedBoard(boardName.trim(), user);
            sharedBoards.add(newBoard);
            boardListModel.addElement(newBoard);

            // Select the new board
            boardList.setSelectedValue(newBoard, true);
        }
    }

    private void deleteSelectedBoard() {
        int selectedIndex = boardList.getSelectedIndex();
        if (selectedIndex >= 0) {
            SharedBoard selectedBoard = boardListModel.getElementAt(selectedIndex);

            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to delete the board '" + selectedBoard.getName() + "'?",
                    "Confirm Deletion",
                    JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                sharedBoards.remove(selectedBoard);
                boardListModel.remove(selectedIndex);

                // Clear the task panel
                currentBoard = null;
                refreshTaskPanel();
            }
        }
    }

    private void displaySelectedBoard() {
        int selectedIndex = boardList.getSelectedIndex();
        if (selectedIndex >= 0) {
            currentBoard = boardListModel.getElementAt(selectedIndex);
            refreshTaskPanel();
        }
    }

    private void refreshTaskPanel() {
        taskPanel.removeAll();

        if (currentBoard == null) {
            JPanel emptyPanel = new JPanel(new GridBagLayout());
            JLabel emptyLabel = new JLabel("Select a board or create a new one");
            emptyPanel.add(emptyLabel);
            taskPanel.add(emptyPanel, BorderLayout.CENTER);
        } else {
            // Create header with board info
            JPanel headerPanel = new JPanel(new BorderLayout(5, 5));
            JLabel titleLabel = new JLabel(currentBoard.getName());
            titleLabel.setFont(new Font("Arial", Font.BOLD, 16));

            JLabel infoLabel = new JLabel("Created: " + currentBoard.getCreationDate());
            infoLabel.setFont(new Font("Arial", Font.ITALIC, 12));

            headerPanel.add(titleLabel, BorderLayout.NORTH);
            headerPanel.add(infoLabel, BorderLayout.SOUTH);

            // Create task list
            DefaultListModel<LiveTask> taskListModel = new DefaultListModel<>();
            for (LiveTask task : currentBoard.getTasks()) {
                taskListModel.addElement(task);
            }

            JList<LiveTask> taskList = new JList<>(taskListModel);
            taskList.setCellRenderer(new TaskCellRenderer());

            // Create button panel
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JButton addTaskButton = new JButton("Add Task");
            JButton editTaskButton = new JButton("Edit Task");
            JButton deleteTaskButton = new JButton("Delete Task");
            JButton assignDeadlineButton = new JButton("Assign Deadline");

            addTaskButton.addActionListener(e -> addTask());
            editTaskButton.addActionListener(e -> editSelectedTask(taskList));
            deleteTaskButton.addActionListener(e -> deleteSelectedTask(taskList, taskListModel));
            assignDeadlineButton.addActionListener(e -> assignDeadlineToTask(taskList));

            buttonPanel.add(addTaskButton);
            buttonPanel.add(editTaskButton);
            buttonPanel.add(deleteTaskButton);
            buttonPanel.add(assignDeadlineButton);

            // Add components to task panel
            taskPanel.add(headerPanel, BorderLayout.NORTH);
            taskPanel.add(new JScrollPane(taskList), BorderLayout.CENTER);
            taskPanel.add(buttonPanel, BorderLayout.SOUTH);
        }

        taskPanel.revalidate();
        taskPanel.repaint();
    }

    private void addTask() {
        if (currentBoard == null)
            return;

        String taskTitle = JOptionPane.showInputDialog(this,
                "Enter task title:",
                "Add New Task",
                JOptionPane.PLAIN_MESSAGE);

        if (taskTitle != null && !taskTitle.trim().isEmpty()) {
            LiveTask newTask = new LiveTask(taskTitle.trim());
            currentBoard.addTask(newTask);
            refreshTaskPanel();
        }
    }

    private void editSelectedTask(JList<LiveTask> taskList) {
        LiveTask selectedTask = taskList.getSelectedValue();
        if (selectedTask == null)
            return;

        // Create dialog for editing task
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Edit Task", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);

        JPanel formPanel = new JPanel(new GridLayout(3, 1, 5, 5));

        JPanel titlePanel = new JPanel(new BorderLayout(5, 0));
        titlePanel.add(new JLabel("Title:"), BorderLayout.WEST);
        JTextField titleField = new JTextField(selectedTask.getTitle());
        titlePanel.add(titleField, BorderLayout.CENTER);

        JPanel statusPanel = new JPanel(new BorderLayout(5, 0));
        statusPanel.add(new JLabel("Status:"), BorderLayout.WEST);
        JComboBox<String> statusCombo = new JComboBox<>(new String[] {
                LiveTask.STATUS_TODO,
                LiveTask.STATUS_IN_PROGRESS,
                LiveTask.STATUS_COMPLETED
        });
        statusCombo.setSelectedItem(selectedTask.getStatus());
        statusPanel.add(statusCombo, BorderLayout.CENTER);

        JPanel notesPanel = new JPanel(new BorderLayout(5, 0));
        notesPanel.add(new JLabel("Notes:"), BorderLayout.NORTH);
        JTextArea notesArea = new JTextArea(selectedTask.getNotes());
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        notesPanel.add(new JScrollPane(notesArea), BorderLayout.CENTER);

        formPanel.add(titlePanel);
        formPanel.add(statusPanel);
        formPanel.add(notesPanel);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");

        saveButton.addActionListener(e -> {
            selectedTask.setTitle(titleField.getText().trim());
            selectedTask.setStatus((String) statusCombo.getSelectedItem());
            selectedTask.setNotes(notesArea.getText());

            refreshTaskPanel();
            dialog.dispose();
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        dialog.add(formPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private void deleteSelectedTask(JList<LiveTask> taskList, DefaultListModel<LiveTask> taskListModel) {
        LiveTask selectedTask = taskList.getSelectedValue();
        if (selectedTask == null)
            return;

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete the task '" + selectedTask.getTitle() + "'?",
                "Confirm Task Deletion",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            currentBoard.removeTask(selectedTask);
            refreshTaskPanel();
        }
    }

    private void assignDeadlineToTask(JList<LiveTask> taskList) {
        LiveTask selectedTask = taskList.getSelectedValue();
        if (selectedTask == null)
            return;

        // Create a dialog for deadline assignment
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Assign Deadline", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(400, 250);
        dialog.setLocationRelativeTo(this);

        JPanel formPanel = new JPanel(new GridLayout(4, 1, 5, 5));

        // Task info
        JLabel taskLabel = new JLabel("Task: " + selectedTask.getTitle());
        taskLabel.setFont(new Font("Arial", Font.BOLD, 14));

        // Date fields
        JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        datePanel.add(new JLabel("Due Date (DD/MM/YYYY): "));
        JTextField dayField = new JTextField(2);
        JTextField monthField = new JTextField(2);
        JTextField yearField = new JTextField(4);

        datePanel.add(dayField);
        datePanel.add(new JLabel("/"));
        datePanel.add(monthField);
        datePanel.add(new JLabel("/"));
        datePanel.add(yearField);

        // Time field
        JPanel timePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        timePanel.add(new JLabel("Time (24hr): "));
        JTextField hourField = new JTextField(2);
        timePanel.add(hourField);
        timePanel.add(new JLabel(":00"));

        // Description field
        JPanel descPanel = new JPanel(new BorderLayout(5, 0));
        descPanel.add(new JLabel("Description:"), BorderLayout.NORTH);
        JTextField descField = new JTextField();
        descPanel.add(descField, BorderLayout.CENTER);

        formPanel.add(taskLabel);
        formPanel.add(datePanel);
        formPanel.add(timePanel);
        formPanel.add(descPanel);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Assign Deadline");
        JButton cancelButton = new JButton("Cancel");

        saveButton.addActionListener(e -> {
            try {
                int day = Integer.parseInt(dayField.getText().trim());
                int month = Integer.parseInt(monthField.getText().trim());
                int year = Integer.parseInt(yearField.getText().trim());
                int hour = Integer.parseInt(hourField.getText().trim());

                if (day < 1 || day > 31 || month < 1 || month > 12 ||
                        year < 2023 || hour < 0 || hour > 23) {
                    throw new NumberFormatException("Invalid date/time values");
                }

                // Create our custom Date
                Date dueDate = new Date(hour, day, month, year);

                // Create a description for the deadline
                String description = descField.getText().trim();
                if (description.isEmpty()) {
                    description = "Deadline for: " + selectedTask.getTitle();
                }

                // Create a Deadline
                Deadline deadline = new Deadline(description, dueDate.toJavaDate(), user);

                // This would usually save to a database or similar
                JOptionPane.showMessageDialog(dialog,
                        "Deadline set for task '" + selectedTask.getTitle() + "':\n" +
                                "Due: " + dueDate.toString() + "\n" +
                                "Description: " + description,
                        "Deadline Set",
                        JOptionPane.INFORMATION_MESSAGE);

                dialog.dispose();

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog,
                        "Please enter valid date and time values",
                        "Invalid Input",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        dialog.add(formPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    // Custom cell renderer for tasks in the list
    private class TaskCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {

            JLabel label = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);

            if (value instanceof LiveTask) {
                LiveTask task = (LiveTask) value;
                label.setText(task.getTitle());

                // Set icon based on status
                if (task.getStatus().equals(LiveTask.STATUS_COMPLETED)) {
                    label.setIcon(UIManager.getIcon("OptionPane.informationIcon"));
                } else if (task.getStatus().equals(LiveTask.STATUS_IN_PROGRESS)) {
                    label.setIcon(UIManager.getIcon("OptionPane.warningIcon"));
                } else {
                    label.setIcon(UIManager.getIcon("OptionPane.questionIcon"));
                }
            }

            return label;
        }
    }
}
