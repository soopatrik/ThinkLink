package main.java.application;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import main.java.auth.User;
import main.java.network.ServerConnection;
import main.java.board.BoardPanel;
import main.java.notes.NotePanel;
import main.java.checklist.ChecklistPanel;
import main.java.calendar.CalendarPanel;
import main.java.board.SharedBoardPanel;
import org.json.*;

public class Dashboard extends JFrame {
    private User user;
    private JTabbedPane tabbedPane;
    private ServerConnection serverConnection;

    public Dashboard(User user) {
        this(user, null);
    }

    public Dashboard(User user, ServerConnection serverConnection) {
        this.user = user;
        this.serverConnection = serverConnection;

        setTitle("ThinkLink - " + user.getUserEmail() +
                (user.isAdministrator() ? " (Administrator)" : ""));
        setSize(1024, 768);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        tabbedPane = new JTabbedPane();

        // Create a BoardPanel with a FIXED board ID for all users
        BoardPanel globalBoardPanel = new BoardPanel(user, serverConnection, "shared-global-board", this);

        // Create notes and checklist panels for all users
        NotePanel notePanel = new NotePanel(user.getUserEmail(), user.isAdministrator());
        ChecklistPanel checklistPanel = new ChecklistPanel(user, serverConnection);

        // Add calendar panel for all users, but with different permissions
        CalendarPanel calendarPanel = new CalendarPanel(user);

        // Add tabs - Board first as primary feature
        tabbedPane.addTab("🎯 Mind Map", globalBoardPanel);
        tabbedPane.addTab("📝 Notes", notePanel);
        tabbedPane.addTab("🏆 Goals", checklistPanel);
        tabbedPane.addTab("📅 Calendar", calendarPanel);

        // Only add shared board tab for administrators
        if (user.isAdministrator()) {
            SharedBoardPanel sharedBoardPanel = new SharedBoardPanel(user);
            tabbedPane.addTab("🔧 Admin", sharedBoardPanel);
        }

        add(tabbedPane, BorderLayout.CENTER);

        // Add status bar
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createEtchedBorder());

        JLabel userLabel = new JLabel(" Logged in as: " + user.getUserEmail() +
                " (" + user.getRole() + ")");
        statusBar.add(userLabel, BorderLayout.WEST);

        // Add role-specific information to status bar
        JLabel roleInfoLabel = new JLabel("");
        if (user.isAdministrator()) {
            roleInfoLabel.setText("Administrator access: Can manage shared boards and deadlines ");
        } else {
            roleInfoLabel.setText("Regular user access: Can create and edit personal content ");
        }
        statusBar.add(roleInfoLabel, BorderLayout.EAST);

        add(statusBar, BorderLayout.SOUTH);

        // Simplified header panel WITHOUT exit button
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel appTitle = new JLabel("ThinkLink - Collaborative Mind Mapping & Productivity");
        appTitle.setFont(new Font("Arial", Font.BOLD, 16));
        appTitle.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        headerPanel.add(appTitle, BorderLayout.WEST);

        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
        add(headerPanel, BorderLayout.NORTH);

        // Add window listener for graceful shutdown when X is clicked
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                shutdown();
            }
        });
    }

    /**
     * Simplified shutdown process that won't hang
     */
    private void shutdown() {
        try {
            // Quick save attempt - don't let it hang the shutdown
            SwingUtilities.invokeLater(() -> {
                try {
                    // Save current board state quickly
                    for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                        Component comp = tabbedPane.getComponentAt(i);
                        if (comp instanceof BoardPanel) {
                            ((BoardPanel) comp).saveCurrentState();
                            break; // Only save the first one quickly
                        }
                    }
                } catch (Exception ex) {
                    // Don't let save errors prevent shutdown
                    System.err.println("Error during quick save: " + ex.getMessage());
                }

                // Disconnect from server
                if (serverConnection != null) {
                    try {
                        serverConnection.disconnect();
                    } catch (Exception ex) {
                        // Don't let disconnect errors prevent shutdown
                        System.err.println("Error disconnecting: " + ex.getMessage());
                    }
                }

                // Force exit
                System.exit(0);
            });
        } catch (Exception e) {
            // If anything goes wrong, force exit anyway
            System.err.println("Error during shutdown: " + e.getMessage());
            System.exit(0);
        }
    }

    public void handleServerMessage(JSONObject message) {
        try {
            String type = message.optString("type", "");
            String boardIdFromMessage = message.optString("boardId", null);

            // Handle goal updates
            if (type.equals("goal_update")) {
                // Find the ChecklistPanel and forward the message
                for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                    Component comp = tabbedPane.getComponentAt(i);
                    if (comp instanceof ChecklistPanel) {
                        ((ChecklistPanel) comp).handleServerMessage(message);
                        break;
                    }
                }
                return;
            }

            // Special handling for initial_board_state
            if (type.equals("initial_board_state")) {
                boolean forwarded = false;
                for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                    Component selectedComponent = tabbedPane.getComponentAt(i);
                    if (selectedComponent instanceof BoardPanel) {
                        BoardPanel currentBoardPanel = (BoardPanel) selectedComponent;
                        if ("shared-global-board".equals(currentBoardPanel.getBoardId())) {
                            currentBoardPanel.handleServerMessage(message);
                            forwarded = true;
                            break;
                        }
                    }
                }
                return;
            }

            // Route messages that have a specific boardId
            if (boardIdFromMessage != null) {
                for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                    Component selectedComponent = tabbedPane.getComponentAt(i);
                    if (selectedComponent instanceof BoardPanel) {
                        BoardPanel currentBoardPanel = (BoardPanel) selectedComponent;
                        try {
                            String panelBoardId = currentBoardPanel.getBoardId();
                            if (boardIdFromMessage.equals(panelBoardId)) {
                                currentBoardPanel.handleServerMessage(message);
                                break;
                            }
                        } catch (Exception e) {
                            System.err.println("Error handling message: " + e.getMessage());
                        }
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Dashboard: General error in handleServerMessage: " + e.getMessage());
        }
    }

    public void broadcastMessage(String message) {
        if (serverConnection != null) {
            try {
                org.json.JSONObject jsonMessage = new org.json.JSONObject(message);
                serverConnection.sendMessage(jsonMessage);
            } catch (Exception e) {
                System.err.println("Dashboard: Error broadcasting message to server: " + e.getMessage());
            }
        } else {
            System.err.println("Dashboard: Cannot broadcast message, serverConnection is null.");
        }
    }
}
