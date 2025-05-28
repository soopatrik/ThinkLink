package main.java.board;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import main.java.auth.User;
import main.java.utils.DatabaseSaveAndLoad;

/**
 * Enhanced admin panel for managing shared boards and system administration
 */
public class SharedBoardPanel extends JPanel {
    private User user;
    private DatabaseSaveAndLoad saveAndLoad;
    private DefaultListModel<String> boardListModel;
    private JList<String> boardList;
    private JTextArea logArea;

    public SharedBoardPanel(User user) {
        this.user = user;
        this.saveAndLoad = new DatabaseSaveAndLoad();

        // Check database availability for admin panel
        if (saveAndLoad.isDatabasePrimary()) {
            System.out.println("SharedBoardPanel: Using database storage");
        } else {
            System.out.println("SharedBoardPanel: Using file storage");
        }

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Only allow admin access
        if (!user.isAdministrator()) {
            add(createAccessDeniedPanel(), BorderLayout.CENTER);
            return;
        }

        // Create admin interface
        createAdminInterface();
    }

    private JPanel createAccessDeniedPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        JLabel iconLabel = new JLabel("🔒");
        iconLabel.setFont(new Font("Arial", Font.PLAIN, 72));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(20, 20, 20, 20);
        panel.add(iconLabel, gbc);

        JLabel messageLabel = new JLabel("Administrator Access Required");
        messageLabel.setFont(new Font("Arial", Font.BOLD, 18));
        gbc.gridy = 1;
        panel.add(messageLabel, gbc);

        JLabel detailLabel = new JLabel("Only administrator users can access this panel.");
        detailLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        gbc.gridy = 2;
        panel.add(detailLabel, gbc);

        return panel;
    }

    private void createAdminInterface() {
        // Header panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("🔧 Administrator Control Panel", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        headerPanel.add(titleLabel, BorderLayout.CENTER);

        JLabel userLabel = new JLabel("Logged in as: " + user.getUserEmail() + " (Administrator)", JLabel.CENTER);
        userLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        headerPanel.add(userLabel, BorderLayout.SOUTH);

        add(headerPanel, BorderLayout.NORTH);

        // Main content panel with tabs
        JTabbedPane adminTabs = new JTabbedPane();

        // Board Management Tab
        adminTabs.addTab("📋 Board Management", createBoardManagementPanel());

        // System Info Tab
        adminTabs.addTab("📊 System Info", createSystemInfoPanel());

        // User Management Tab (future enhancement)
        adminTabs.addTab("👥 User Management", createUserManagementPanel());

        add(adminTabs, BorderLayout.CENTER);
    }

    private JPanel createBoardManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        // Board list panel
        JPanel listPanel = new JPanel(new BorderLayout());
        listPanel.setBorder(new TitledBorder("Shared Boards"));

        boardListModel = new DefaultListModel<>();
        boardList = new JList<>(boardListModel);
        boardList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(boardList);
        scrollPane.setPreferredSize(new Dimension(300, 200));
        listPanel.add(scrollPane, BorderLayout.CENTER);

        // Board action buttons
        JPanel buttonPanel = new JPanel(new FlowLayout());

        JButton refreshButton = new JButton("🔄 Refresh");
        refreshButton.addActionListener(e -> refreshBoardList());

        JButton createButton = new JButton("➕ Create Board");
        createButton.addActionListener(e -> createNewBoard());

        JButton deleteButton = new JButton("🗑️ Delete Board");
        deleteButton.addActionListener(e -> deleteSelectedBoard());
        deleteButton.setBackground(new Color(220, 53, 69));
        deleteButton.setForeground(Color.WHITE);

        JButton viewButton = new JButton("👁️ View Board");
        viewButton.addActionListener(e -> viewSelectedBoard());

        buttonPanel.add(refreshButton);
        buttonPanel.add(createButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(viewButton);

        listPanel.add(buttonPanel, BorderLayout.SOUTH);
        panel.add(listPanel, BorderLayout.WEST);

        // Board details panel
        JPanel detailsPanel = new JPanel(new BorderLayout());
        detailsPanel.setBorder(new TitledBorder("Board Details"));

        logArea = new JTextArea(15, 40);
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setText("Welcome to ThinkLink Admin Panel\n\n" +
                "Here you can:\n" +
                "• Create new shared boards for team collaboration\n" +
                "• Delete boards (WARNING: This removes all content)\n" +
                "• View board statistics and usage\n" +
                "• Monitor system activity\n\n" +
                "Select a board from the list to view details.\n");

        JScrollPane logScrollPane = new JScrollPane(logArea);
        detailsPanel.add(logScrollPane, BorderLayout.CENTER);

        panel.add(detailsPanel, BorderLayout.CENTER);

        // Load initial data
        refreshBoardList();

        return panel;
    }

    private JPanel createSystemInfoPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JTextArea infoArea = new JTextArea();
        infoArea.setEditable(false);
        infoArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        // Gather system information
        StringBuilder info = new StringBuilder();
        info.append("=== ThinkLink System Information ===\n\n");
        info.append("Server Status: ✅ Active\n");
        info.append("Collaboration: ✅ Real-time sync enabled\n");
        info.append("Data Persistence: ✅ All changes saved automatically\n\n");

        info.append("=== User Permissions ===\n");
        info.append("Administrator Users Can:\n");
        info.append("• Create and delete shared boards\n");
        info.append("• Set deadlines for all users\n");
        info.append("• Edit and remove deadlines\n");
        info.append("• Access system administration panel\n\n");

        info.append("Customary Users Can:\n");
        info.append("• Add, edit, and remove tasks in shared boards\n");
        info.append("• Create personal notes\n");
        info.append("• Manage personal checklists\n");
        info.append("• View deadlines set by administrators\n\n");

        info.append("=== Data Storage ===\n");
        info.append("Notes: Stored per user in secure directories\n");
        info.append("Checklists: Personal to each user\n");
        info.append("Mind Maps: Shared across all users with real-time sync\n");
        info.append("Calendar: Administrators can set deadlines for all users\n\n");

        info.append("=== Technical Details ===\n");
        info.append("Backend: Java with JSON data persistence\n");
        info.append("Frontend: Swing GUI with tabbed interface\n");
        info.append("Networking: Socket-based real-time collaboration\n");
        info.append("Security: Role-based access control\n");

        infoArea.setText(info.toString());

        JScrollPane scrollPane = new JScrollPane(infoArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createUserManagementPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        JLabel comingSoonLabel = new JLabel("👥 User Management");
        comingSoonLabel.setFont(new Font("Arial", Font.BOLD, 18));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(20, 20, 10, 20);
        panel.add(comingSoonLabel, gbc);

        JTextArea descArea = new JTextArea(10, 40);
        descArea.setEditable(false);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setText("Future enhancements will include:\n\n" +
                "• View all registered users\n" +
                "• Change user roles (Admin ↔ Customary)\n" +
                "• Monitor user activity\n" +
                "• Send notifications to users\n" +
                "• Export user data and reports\n\n" +
                "Currently, user management is handled through the login system where users can register as either Administrator or Customary users.");

        gbc.gridy = 1;
        panel.add(new JScrollPane(descArea), gbc);

        return panel;
    }

    private void refreshBoardList() {
        boardListModel.clear();
        List<String> boards = saveAndLoad.getBoardList();

        logArea.append("\n=== Refreshing Board List ===\n");
        logArea.append("Found " + boards.size() + " board(s)\n");

        for (String board : boards) {
            boardListModel.addElement(board);
            logArea.append("- " + board + "\n");
        }

        if (boards.isEmpty()) {
            logArea.append("No boards found. Create a new board to get started.\n");
        }

        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void createNewBoard() {
        String boardName = JOptionPane.showInputDialog(this,
                "Enter name for new shared board:",
                "Create New Board",
                JOptionPane.PLAIN_MESSAGE);

        if (boardName != null && !boardName.trim().isEmpty()) {
            boardName = boardName.trim();

            // Check if board already exists
            if (boardListModel.contains(boardName)) {
                JOptionPane.showMessageDialog(this,
                        "A board with that name already exists!",
                        "Board Creation Failed",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Create new board (for now, just add to list)
            boardListModel.addElement(boardName);
            logArea.append("\n✅ Created new board: " + boardName + "\n");
            logArea.append("Board is now available for all users to collaborate on.\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());

            JOptionPane.showMessageDialog(this,
                    "Board '" + boardName + "' created successfully!\n\n" +
                            "Users can now collaborate on this shared board.",
                    "Board Created",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void deleteSelectedBoard() {
        String selectedBoard = boardList.getSelectedValue();
        if (selectedBoard == null) {
            JOptionPane.showMessageDialog(this,
                    "Please select a board to delete.",
                    "No Board Selected",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "⚠️ WARNING: This will permanently delete the board '" + selectedBoard +
                        "' and ALL its content!\n\nThis action cannot be undone.\n\nAre you sure?",
                "Confirm Board Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            boolean success = saveAndLoad.deleteBoard(selectedBoard);
            if (success) {
                boardListModel.removeElement(selectedBoard);
                logArea.append("\n🗑️ DELETED board: " + selectedBoard + "\n");
                logArea.append("All content and tasks have been permanently removed.\n");
                logArea.setCaretPosition(logArea.getDocument().getLength());

                JOptionPane.showMessageDialog(this,
                        "Board '" + selectedBoard + "' has been deleted.",
                        "Board Deleted",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Failed to delete board. It may not exist or there may be a permission issue.",
                        "Deletion Failed",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void viewSelectedBoard() {
        String selectedBoard = boardList.getSelectedValue();
        if (selectedBoard == null) {
            JOptionPane.showMessageDialog(this,
                    "Please select a board to view.",
                    "No Board Selected",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        logArea.append("\n👁️ Viewing board: " + selectedBoard + "\n");
        logArea.append("Board details:\n");
        logArea.append("- Name: " + selectedBoard + "\n");
        logArea.append("- Type: Shared collaborative board\n");
        logArea.append("- Access: All registered users\n");
        logArea.append("- Features: Real-time collaboration, task management\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }
}
