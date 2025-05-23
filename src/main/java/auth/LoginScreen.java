package main.java.auth;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import main.java.application.Dashboard;
import main.java.network.ServerConnection;

/**
 * Login screen for ThinkLink application.
 * Handles user authentication and role selection.
 */
public class LoginScreen extends JFrame {
    // Define the interface at the class level
    public interface DashboardCreatedCallback {
        void onDashboardCreated(Dashboard dashboard);
    }

    private JTextField emailField;
    private JPasswordField passwordField;
    private JRadioButton adminButton;
    private JRadioButton userButton;
    private JButton loginButton;
    private ServerConnection serverConnection;
    private DashboardCreatedCallback dashboardCallback;

    public LoginScreen() {
        this.serverConnection = null;
        setTitle("ThinkLink - Collaborative Mind Mapping");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(520, 480);
        setLocationRelativeTo(null);
        setResizable(false);

        // Main panel with clean design
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(40, 50, 40, 50));
        mainPanel.setBackground(new Color(248, 249, 250)); // Very light gray

        // Title section
        JLabel titleLabel = new JLabel("ThinkLink");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 26));
        titleLabel.setForeground(new Color(70, 130, 180));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Collaborative Mind Mapping & Project Management");
        subtitleLabel.setFont(new Font("Arial", Font.ITALIC, 13));
        subtitleLabel.setForeground(new Color(108, 117, 125));
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Email field
        JLabel emailLabel = new JLabel("Email:");
        emailLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        emailLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        emailField = new JTextField();
        emailField.setFont(new Font("Arial", Font.PLAIN, 14));
        emailField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        emailField.setAlignmentX(Component.LEFT_ALIGNMENT);
        emailField.setBackground(Color.WHITE);
        emailField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(206, 212, 218), 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        emailField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    login();
                }
            }
        });

        // Password field
        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        passwordLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        passwordField = new JPasswordField();
        passwordField.setFont(new Font("Arial", Font.PLAIN, 14));
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);
        passwordField.setBackground(Color.WHITE);
        passwordField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(206, 212, 218), 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        passwordField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    login();
                }
            }
        });

        // User type section
        JLabel userTypeLabel = new JLabel("User Type:");
        userTypeLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        userTypeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel radioPanel = new JPanel();
        radioPanel.setLayout(new BoxLayout(radioPanel, BoxLayout.Y_AXIS));
        radioPanel.setBackground(Color.WHITE);
        radioPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(206, 212, 218), 1),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));
        radioPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        radioPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        ButtonGroup group = new ButtonGroup();
        userButton = new JRadioButton("Regular User", true);
        userButton.setFont(new Font("Arial", Font.PLAIN, 13));
        userButton.setBackground(Color.WHITE);

        adminButton = new JRadioButton("Administrator");
        adminButton.setFont(new Font("Arial", Font.BOLD, 13));
        adminButton.setForeground(new Color(220, 53, 69));
        adminButton.setBackground(Color.WHITE);

        group.add(userButton);
        group.add(adminButton);

        radioPanel.add(userButton);
        radioPanel.add(Box.createVerticalStrut(6));
        radioPanel.add(adminButton);

        // CLEAN WHITE LOGIN BUTTON
        loginButton = new JButton("LOGIN");
        loginButton.setFont(new Font("Arial", Font.BOLD, 16));
        loginButton.setPreferredSize(new Dimension(160, 44));
        loginButton.setMaximumSize(new Dimension(160, 44));
        loginButton.setMinimumSize(new Dimension(160, 44));
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Clean white design
        loginButton.setBackground(Color.WHITE);
        loginButton.setForeground(new Color(70, 130, 180));
        loginButton.setOpaque(true);
        loginButton.setBorderPainted(true);
        loginButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(70, 130, 180), 2),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)));
        loginButton.setFocusPainted(false);
        loginButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Subtle hover effect
        loginButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                loginButton.setBackground(new Color(70, 130, 180));
                loginButton.setForeground(Color.WHITE);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                loginButton.setBackground(Color.WHITE);
                loginButton.setForeground(new Color(70, 130, 180));
            }
        });

        loginButton.addActionListener(e -> login());

        // Description
        JTextArea descArea = new JTextArea(
                "ThinkLink lets you create and share mind maps, track tasks, " +
                        "and manage projects in a single workspace. Regular users can " +
                        "create and edit content, while administrators have additional " +
                        "management capabilities.");
        descArea.setWrapStyleWord(true);
        descArea.setLineWrap(true);
        descArea.setOpaque(false);
        descArea.setEditable(false);
        descArea.setFocusable(false);
        descArea.setFont(new Font("Arial", Font.PLAIN, 11));
        descArea.setForeground(new Color(108, 117, 125));
        descArea.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Add all components with clean spacing
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(4));
        mainPanel.add(subtitleLabel);
        mainPanel.add(Box.createVerticalStrut(30));

        mainPanel.add(emailLabel);
        mainPanel.add(Box.createVerticalStrut(6));
        mainPanel.add(emailField);
        mainPanel.add(Box.createVerticalStrut(16));

        mainPanel.add(passwordLabel);
        mainPanel.add(Box.createVerticalStrut(6));
        mainPanel.add(passwordField);
        mainPanel.add(Box.createVerticalStrut(16));

        mainPanel.add(userTypeLabel);
        mainPanel.add(Box.createVerticalStrut(6));
        mainPanel.add(radioPanel);
        mainPanel.add(Box.createVerticalStrut(24));

        mainPanel.add(loginButton);
        mainPanel.add(Box.createVerticalStrut(24));

        mainPanel.add(descArea);

        add(mainPanel);
    }

    public LoginScreen(ServerConnection serverConnection) {
        this();
        this.serverConnection = serverConnection;
    }

    /**
     * Handles the login process
     */
    private void login() {
        String email = emailField.getText().trim();

        if (email.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter an email address.",
                    "Input Required",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean isAdmin = adminButton.isSelected();
        String role = isAdmin ? User.ROLE_ADMINISTRATOR : User.ROLE_CUSTOMARY;

        // DEBUG OUTPUT
        System.out.println("=== LOGIN DEBUG ===");
        System.out.println("Email: " + email);
        System.out.println("Admin button selected: " + isAdmin);
        System.out.println("Role being set: " + role);
        System.out.println("==================");

        // Try to connect to server if available
        if (serverConnection != null) {
            try {
                boolean serverConnected = serverConnection.connect(email, role);
                if (!serverConnected) {
                    JOptionPane.showMessageDialog(this,
                            "Could not connect to collaboration server. You can still use the application in offline mode.",
                            "Server Connection Warning",
                            JOptionPane.WARNING_MESSAGE);
                }
            } catch (Exception e) {
                System.out.println("Error connecting to server: " + e.getMessage());
            }
        }

        User user = new User(email, role);
        Dashboard dashboard = new Dashboard(user, serverConnection);

        if (dashboardCallback != null) {
            dashboardCallback.onDashboardCreated(dashboard);
        }

        dashboard.setVisible(true);
        this.dispose();
    }

    /**
     * Creates and displays the login screen
     */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            LoginScreen loginScreen = new LoginScreen(null);
            loginScreen.setVisible(true);
        });
    }

    public void setServerConnection(ServerConnection serverConnection) {
        this.serverConnection = serverConnection;
    }

    public void setDashboardCreatedCallback(DashboardCreatedCallback callback) {
        this.dashboardCallback = callback;
    }
}
