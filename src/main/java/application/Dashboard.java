package main.java.application;

import javax.swing.*;
import java.awt.*;
import main.java.board.BoardPanel;
import main.notes.NotePanel;
import main.checklist.ChecklistPanel;
import main.java.calendar.CalendarPanel;
import main.java.auth.User;
import main.java.board.SharedBoardPanel;

public class Dashboard extends JFrame {
    private User user;
    private JTabbedPane tabbedPane;

    public Dashboard(User user) {
        this.user = user;

        setTitle("ThinkLink - " + user.getUserEmail() +
                (user.isAdministrator() ? " (Administrator)" : ""));
        setSize(1024, 768);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        tabbedPane = new JTabbedPane();

        // Standard board for all users
        BoardPanel boardPanel = new BoardPanel(user);

        // Create panels for notes and checklists
        NotePanel notePanel = new NotePanel(user.getUserEmail(), user.isAdministrator());
        ChecklistPanel checklistPanel = new ChecklistPanel();

        // Add calendar panel for all users, but with different permissions
        CalendarPanel calendarPanel = new CalendarPanel(user);

        // Add tabs
        tabbedPane.addTab("Board", boardPanel);
        tabbedPane.addTab("Notes", notePanel);
        tabbedPane.addTab("Checklist", checklistPanel);
        tabbedPane.addTab("Calendar", calendarPanel);

        // Only add shared board tab for administrators
        if (user.isAdministrator()) {
            SharedBoardPanel sharedBoardPanel = new SharedBoardPanel(user);
            tabbedPane.addTab("Shared Boards", sharedBoardPanel);
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
    }
}
