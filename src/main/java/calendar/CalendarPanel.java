package main.java.calendar;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.List;
import java.text.SimpleDateFormat;
import main.java.auth.User;
import javax.swing.table.AbstractTableModel;
import main.java.calendar.Deadline;

public class CalendarPanel extends JPanel {
    private LocalDate currentDate = LocalDate.now();
    private JLabel monthYearLabel;
    private JPanel calendarGrid;
    private Map<LocalDate, List<Deadline>> deadlines = new HashMap<>();
    private boolean isAdmin;
    private User user;
    private JTable calendarTable;
    private java.util.Calendar currentCalendar;
    private ArrayList<Deadline> deadlinesList;

    private String[] weekdays = { "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" };
    private String[] months = {
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
    };

    // Deadline class to hold task deadline information
    public static class Deadline {
        private int id;
        private static int nextId = 1;
        private String title;
        private String description;
        private LocalDate date;
        private boolean completed;

        public Deadline(String title, String description, LocalDate date) {
            this.id = nextId++;
            this.title = title;
            this.description = description;
            this.date = date;
            this.completed = false;
        }

        // Getters and setters
        public int getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public boolean isCompleted() {
            return completed;
        }

        public void setCompleted(boolean completed) {
            this.completed = completed;
        }

        @Override
        public String toString() {
            return title + " (" + date.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")) + ")";
        }
    }

    public CalendarPanel(User user) {
        this.user = user;
        this.deadlinesList = new ArrayList<>();
        this.isAdmin = user.isAdministrator();
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create current calendar
        currentCalendar = java.util.Calendar.getInstance();

        // Create navigation panel at top
        createNavigationPanel();

        // Create calendar grid in center
        calendarGrid = new JPanel(new GridLayout(0, 7));
        JScrollPane scrollPane = new JScrollPane(calendarGrid);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        add(scrollPane, BorderLayout.CENTER);

        // Create action panel at bottom (for admin only)
        if (isAdmin) {
            createActionPanel();
        } else {
            // For non-admin users, show a message about not being able to edit
            JPanel infoPanel = new JPanel();
            infoPanel.add(new JLabel("Only administrators can set deadlines. Contact your administrator for changes."));
            add(infoPanel, BorderLayout.SOUTH);
        }

        // Add sample deadlines
        addSampleDeadlines();

        // Update calendar view
        updateCalendarView();
    }

    private void createNavigationPanel() {
        JPanel navPanel = new JPanel(new BorderLayout());

        // Month navigation buttons
        JButton prevButton = new JButton("<");
        prevButton.addActionListener(e -> changeMonth(-1));

        monthYearLabel = new JLabel("", JLabel.CENTER);
        monthYearLabel.setFont(new Font("Arial", Font.BOLD, 16));

        JButton nextButton = new JButton(">");
        nextButton.addActionListener(e -> changeMonth(1));

        JButton todayButton = new JButton("Today");
        todayButton.addActionListener(e -> {
            currentDate = LocalDate.now();
            updateCalendarView();
        });

        // Add components to panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(prevButton);
        buttonPanel.add(todayButton);
        buttonPanel.add(nextButton);

        navPanel.add(monthYearLabel, BorderLayout.CENTER);
        navPanel.add(buttonPanel, BorderLayout.EAST);

        add(navPanel, BorderLayout.NORTH);
    }

    private void createActionPanel() {
        JPanel actionPanel = new JPanel();

        JButton addDeadlineButton = new JButton("Set Deadline");
        addDeadlineButton.addActionListener(e -> showAddDeadlineDialog());

        JButton viewDeadlinesButton = new JButton("View All Deadlines");
        viewDeadlinesButton.addActionListener(e -> showAllDeadlines());

        actionPanel.add(addDeadlineButton);
        actionPanel.add(viewDeadlinesButton);

        add(actionPanel, BorderLayout.SOUTH);
    }

    private void updateCalendarView() {
        calendarGrid.removeAll();

        // Set month and year label
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy");
        monthYearLabel.setText(currentDate.format(formatter));

        // Add day of week headers
        String[] daysOfWeek = { "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" };
        for (String day : daysOfWeek) {
            JLabel label = new JLabel(day, JLabel.CENTER);
            label.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            label.setFont(label.getFont().deriveFont(Font.BOLD));
            calendarGrid.add(label);
        }

        // Calculate first day of month and total days
        LocalDate firstOfMonth = currentDate.withDayOfMonth(1);
        int monthValue = currentDate.getMonthValue();

        // Fill in empty cells before first day of month
        int dayOfWeek = firstOfMonth.getDayOfWeek().getValue() % 7;
        for (int i = 0; i < dayOfWeek; i++) {
            calendarGrid.add(new JLabel());
        }

        // Add cells for each day of the month
        LocalDate date = firstOfMonth;
        while (date.getMonthValue() == monthValue) {
            // Create day panel
            final LocalDate currentDate = date; // Need final reference for lambda
            JPanel dayPanel = createDayPanel(currentDate);
            calendarGrid.add(dayPanel);

            // Move to next day
            date = date.plusDays(1);
        }

        // Update UI
        calendarGrid.revalidate();
        calendarGrid.repaint();
    }

    private JPanel createDayPanel(LocalDate date) {
        JPanel dayPanel = new JPanel(new BorderLayout());
        dayPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

        // Date label
        JLabel dateLabel = new JLabel(String.valueOf(date.getDayOfMonth()));
        dateLabel.setHorizontalAlignment(JLabel.CENTER);

        // Highlight today's date
        if (date.equals(LocalDate.now())) {
            dateLabel.setFont(dateLabel.getFont().deriveFont(Font.BOLD));
            dateLabel.setForeground(Color.RED);
        }

        dayPanel.add(dateLabel, BorderLayout.NORTH);

        // Add deadline indicators if there are any for this date
        List<Deadline> dayDeadlines = deadlines.get(date);
        if (dayDeadlines != null && !dayDeadlines.isEmpty()) {
            JPanel deadlinePanel = new JPanel();
            deadlinePanel.setLayout(new BoxLayout(deadlinePanel, BoxLayout.Y_AXIS));

            for (Deadline deadline : dayDeadlines) {
                JLabel deadlineLabel = new JLabel("• " + deadline.getTitle());
                deadlineLabel.setFont(deadlineLabel.getFont().deriveFont(9.0f));
                deadlinePanel.add(deadlineLabel);
            }

            dayPanel.add(new JScrollPane(deadlinePanel), BorderLayout.CENTER);

            // Make the day clickable to show deadline details
            dayPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    showDeadlinesForDate(date);
                }
            });

            // Set a different background color for days with deadlines
            dayPanel.setBackground(new Color(240, 248, 255)); // Light blue
        }

        return dayPanel;
    }

    private void showAddDeadlineDialog() {
        if (!isAdmin) {
            JOptionPane.showMessageDialog(this,
                    "Only administrators can set deadlines.",
                    "Permission Denied",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                "Set Deadline", true);
        dialog.setLayout(new BorderLayout());

        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTextField titleField = new JTextField();
        panel.add(new JLabel("Title:"));
        panel.add(titleField);

        JSpinner dateSpinner = new JSpinner(new SpinnerDateModel());
        panel.add(new JLabel("Date:"));
        panel.add(dateSpinner);

        JTextArea descArea = new JTextArea(3, 20);
        descArea.setLineWrap(true);
        JScrollPane descScrollPane = new JScrollPane(descArea);

        JPanel descPanel = new JPanel(new BorderLayout());
        descPanel.add(new JLabel("Description:"), BorderLayout.NORTH);
        descPanel.add(descScrollPane, BorderLayout.CENTER);

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(panel, BorderLayout.NORTH);
        contentPanel.add(descPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");

        saveButton.addActionListener(e -> {
            if (titleField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                        "Please enter a title for the deadline.",
                        "Missing Title",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            LocalDate deadlineDate = convertToLocalDate((Date) dateSpinner.getValue());
            addDeadline(deadlineDate, titleField.getText(), descArea.getText());
            dialog.dispose();
            updateCalendarView();
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        dialog.add(contentPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void showDeadlinesForDate(LocalDate date) {
        List<Deadline> dayDeadlines = deadlines.get(date);
        if (dayDeadlines == null || dayDeadlines.isEmpty())
            return;

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                "Deadlines for " + formatDate(date), true);
        dialog.setLayout(new BorderLayout());

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

        for (Deadline deadline : dayDeadlines) {
            JPanel itemPanel = createDeadlinePanel(deadline, dialog);
            listPanel.add(itemPanel);
            listPanel.add(Box.createVerticalStrut(10));
        }

        JScrollPane scrollPane = new JScrollPane(listPanel);
        scrollPane.setPreferredSize(new Dimension(400, 300));

        dialog.add(scrollPane, BorderLayout.CENTER);

        if (isAdmin) {
            JPanel buttonPanel = new JPanel();
            JButton addButton = new JButton("Add Deadline");
            addButton.addActionListener(e -> {
                dialog.dispose();
                showAddDeadlineDialog();
            });
            buttonPanel.add(addButton);
            dialog.add(buttonPanel, BorderLayout.SOUTH);
        }

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private JPanel createDeadlinePanel(Deadline deadline, JDialog parentDialog) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        JLabel titleLabel = new JLabel(deadline.getTitle());
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));

        JTextArea descArea = new JTextArea(deadline.getDescription());
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setEditable(false);
        descArea.setBackground(panel.getBackground());

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(descArea, BorderLayout.CENTER);

        // Add edit/delete buttons for admin
        if (isAdmin) {
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

            JButton editButton = new JButton("Edit");
            editButton.addActionListener(e -> {
                parentDialog.dispose();
                showEditDeadlineDialog(deadline);
            });

            JButton deleteButton = new JButton("Delete");
            deleteButton.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(
                        parentDialog,
                        "Are you sure you want to delete this deadline?",
                        "Confirm Deletion",
                        JOptionPane.YES_NO_OPTION);

                if (confirm == JOptionPane.YES_OPTION) {
                    deleteDeadline(deadline);
                    parentDialog.dispose();
                    updateCalendarView();
                }
            });

            buttonPanel.add(editButton);
            buttonPanel.add(deleteButton);
            panel.add(buttonPanel, BorderLayout.SOUTH);
        }

        return panel;
    }

    private void showEditDeadlineDialog(Deadline deadline) {
        if (!isAdmin) {
            JOptionPane.showMessageDialog(this,
                    "Only administrators can edit deadlines.",
                    "Permission Denied",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                "Edit Deadline", true);
        dialog.setLayout(new BorderLayout());

        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTextField titleField = new JTextField(deadline.getTitle());
        panel.add(new JLabel("Title:"));
        panel.add(titleField);

        // Date spinner with current date
        SpinnerDateModel dateModel = new SpinnerDateModel();
        java.util.Calendar cal = java.util.Calendar.getInstance();
        if (deadline != null && deadline.getDate() != null) {
            cal.setTime(java.sql.Date.valueOf(deadline.getDate()));
        } else {
            cal.setTime(new java.util.Date()); // Use current date/time
        }
        dateModel.setValue(cal.getTime());
        JSpinner dateSpinner = new JSpinner(dateModel);

        panel.add(new JLabel("Date:"));
        panel.add(dateSpinner);

        JTextArea descArea = new JTextArea(deadline.getDescription(), 3, 20);
        descArea.setLineWrap(true);
        JScrollPane descScrollPane = new JScrollPane(descArea);

        JPanel descPanel = new JPanel(new BorderLayout());
        descPanel.add(new JLabel("Description:"), BorderLayout.NORTH);
        descPanel.add(descScrollPane, BorderLayout.CENTER);

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(panel, BorderLayout.NORTH);
        contentPanel.add(descPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");

        saveButton.addActionListener(e -> {
            if (titleField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                        "Please enter a title for the deadline.",
                        "Missing Title",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Remove from old date
            deleteDeadline(deadline);

            // Update deadline
            LocalDate newDate = convertToLocalDate((Date) dateSpinner.getValue());
            deadline.setTitle(titleField.getText());
            deadline.setDescription(descArea.getText());
            deadline.setDate(newDate);

            // Add to new date
            addDeadline(deadline);

            dialog.dispose();
            updateCalendarView();
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        dialog.add(contentPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void showAllDeadlines() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                "All Deadlines", true);
        dialog.setLayout(new BorderLayout());

        // Create a list model and JList
        DefaultListModel<Deadline> listModel = new DefaultListModel<>();

        // Add all deadlines to the list, sorted by date
        List<Deadline> allDeadlines = new ArrayList<>();
        for (List<Deadline> dayDeadlines : deadlines.values()) {
            allDeadlines.addAll(dayDeadlines);
        }

        // Sort by date
        allDeadlines.sort(Comparator.comparing(Deadline::getDate));

        for (Deadline deadline : allDeadlines) {
            listModel.addElement(deadline);
        }

        JList<Deadline> deadlineList = new JList<>(listModel);
        deadlineList.setCellRenderer(new DeadlineListCellRenderer());

        JScrollPane scrollPane = new JScrollPane(deadlineList);
        scrollPane.setPreferredSize(new Dimension(400, 300));

        JPanel buttonPanel = new JPanel();
        JButton viewButton = new JButton("View Details");
        viewButton.addActionListener(e -> {
            Deadline selected = deadlineList.getSelectedValue();
            if (selected != null) {
                showDeadlinesForDate(selected.getDate());
                dialog.dispose();
            }
        });

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(viewButton);
        buttonPanel.add(closeButton);

        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    // Custom cell renderer for deadline list
    private class DeadlineListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {

            Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof Deadline) {
                Deadline deadline = (Deadline) value;
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
                setText(deadline.getTitle() + " - " + deadline.getDate().format(formatter));

                // Highlight overdue items
                if (deadline.getDate().isBefore(LocalDate.now()) && !deadline.isCompleted()) {
                    setForeground(Color.RED);
                }
            }

            return c;
        }
    }

    // Helper methods
    private void addDeadline(LocalDate date, String title, String description) {
        Deadline deadline = new Deadline(title, description, date);
        addDeadline(deadline);
    }

    private void addDeadline(Deadline deadline) {
        deadlines.computeIfAbsent(deadline.getDate(), k -> new ArrayList<>())
                .add(deadline);
    }

    private void deleteDeadline(Deadline deadline) {
        List<Deadline> dateDeadlines = deadlines.get(deadline.getDate());
        if (dateDeadlines != null) {
            dateDeadlines.remove(deadline);
            if (dateDeadlines.isEmpty()) {
                deadlines.remove(deadline.getDate());
            }
        }
    }

    private LocalDate convertToLocalDate(Date date) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(date.toJavaDate());
        int year = cal.get(java.util.Calendar.YEAR);
        int month = cal.get(java.util.Calendar.MONTH);
        int day = cal.get(java.util.Calendar.DAY_OF_MONTH);
        return LocalDate.of(year, month + 1, day);
    }

    private String formatDate(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));
    }

    private void addSampleDeadlines() {
        // Add sample deadlines for demonstration
        LocalDate today = LocalDate.now();

        addDeadline(today, "Team Meeting", "Weekly project sync-up");
        addDeadline(today.plusDays(3), "Initial Design Due", "Complete UI mockups");
        addDeadline(today.plusDays(7), "Milestone 1", "Core functionality complete");
        addDeadline(today.plusWeeks(2), "Prototype Demo", "Present working prototype to stakeholders");
    }

    // Methods to allow external components to interact with the calendar
    public void addDeadlineForTask(String taskTitle, LocalDate date) {
        if (!isAdmin)
            return;

        addDeadline(date, taskTitle, "Task deadline from board");
        updateCalendarView();
    }

    public List<Deadline> getDeadlinesForDate(LocalDate date) {
        return deadlines.getOrDefault(date, new ArrayList<>());
    }

    public void setAdminMode(boolean isAdmin) {
        this.isAdmin = isAdmin;
        removeAll();

        // Recreate UI with new permissions
        createNavigationPanel();
        add(calendarGrid, BorderLayout.CENTER);

        if (isAdmin) {
            createActionPanel();
        } else {
            JPanel infoPanel = new JPanel();
            infoPanel.add(new JLabel("Only administrators can set deadlines. Contact your administrator for changes."));
            add(infoPanel, BorderLayout.SOUTH);
        }

        updateCalendarView();
        revalidate();
        repaint();
    }

    private void changeMonth(int delta) {
        // Update the java.util.Calendar
        currentCalendar.add(java.util.Calendar.MONTH, delta);

        // Also update the LocalDate
        if (delta > 0) {
            currentDate = currentDate.plusMonths(delta);
        } else {
            currentDate = currentDate.minusMonths(Math.abs(delta));
        }

        // Update the calendar display
        updateCalendarView();
    }

    // Add an inner class for the table model
    private class CalendarTableModel extends AbstractTableModel {
        private int[][] calendarData = new int[6][7]; // 6 weeks, 7 days

        public void updateCalendar(java.util.Calendar calendar) {
            // Create a copy of the calendar to manipulate
            java.util.Calendar cal = (java.util.Calendar) calendar.clone();

            // Clear the calendar data
            for (int row = 0; row < 6; row++) {
                for (int col = 0; col < 7; col++) {
                    calendarData[row][col] = 0;
                }
            }

            // Set to first day of the month
            cal.set(java.util.Calendar.DAY_OF_MONTH, 1);

            // Get first day of week and days in month
            int firstDayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK) - 1;
            int daysInMonth = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH);

            // Fill the calendar
            int day = 1;
            for (int row = 0; row < 6; row++) {
                for (int col = 0; col < 7; col++) {
                    if (row == 0 && col < firstDayOfWeek) {
                        calendarData[row][col] = 0; // Empty cell
                    } else if (day > daysInMonth) {
                        calendarData[row][col] = 0; // Empty cell
                    } else {
                        calendarData[row][col] = day++;
                    }
                }
            }

            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return 6;
        }

        @Override
        public int getColumnCount() {
            return 7;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return calendarData[rowIndex][columnIndex];
        }
    }
}
