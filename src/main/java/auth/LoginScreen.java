package main.java.auth;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import main.java.application.Dashboard;

/**
 * Login screen for ThinkLink application.
 * Handles user authentication and role selection.
 */
public class LoginScreen extends JFrame {
    private JTextField usernameField;
    private JRadioButton adminButton, userButton;
    private JButton loginButton;

    public LoginScreen() {
        setTitle("ThinkLink - Collaborative Mind Mapping");
        setSize(450, 320);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Create main panel with some padding
        JPanel mainPanel = new JPanel(new BorderLayout(0, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Add title/logo at top
        JLabel titleLabel = new JLabel("ThinkLink", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(new Color(70, 130, 180)); // Steel blue color

        JLabel subtitleLabel = new JLabel("Collaborative Mind Mapping & Project Management", JLabel.CENTER);
        subtitleLabel.setFont(new Font("Arial", Font.ITALIC, 14));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        headerPanel.add(subtitleLabel, BorderLayout.SOUTH);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Login form
        JPanel formPanel = new JPanel(new GridLayout(3, 1, 10, 10));

        // Username field
        JPanel usernamePanel = new JPanel(new BorderLayout(5, 0));
        usernamePanel.add(new JLabel("Username:"), BorderLayout.WEST);
        usernameField = new JTextField();
        usernameField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    login();
                }
            }
        });
        usernamePanel.add(usernameField, BorderLayout.CENTER);

        // User type selection
        JPanel userTypePanel = new JPanel(new BorderLayout(5, 0));
        userTypePanel.add(new JLabel("User Type:"), BorderLayout.WEST);

        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        ButtonGroup group = new ButtonGroup();
        userButton = new JRadioButton("Regular User", true);
        adminButton = new JRadioButton("Administrator");

        group.add(userButton);
        group.add(adminButton);
        radioPanel.add(userButton);
        radioPanel.add(adminButton);
        userTypePanel.add(radioPanel, BorderLayout.CENTER);

        // Login button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        loginButton = new JButton("Login");
        loginButton.setPreferredSize(new Dimension(100, 30));
        loginButton.addActionListener(e -> login());
        buttonPanel.add(loginButton);

        // Add components to form
        formPanel.add(usernamePanel);
        formPanel.add(userTypePanel);
        formPanel.add(buttonPanel);

        // Add form to main panel
        mainPanel.add(formPanel, BorderLayout.CENTER);

        // Add description at bottom
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
        descArea.setFont(new Font("Arial", Font.PLAIN, 12));
        descArea.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        mainPanel.add(descArea, BorderLayout.SOUTH);

        // Add everything to frame
        add(mainPanel);
    }

    /**
     * Handles the login process
     */
    private void login() {
        String username = usernameField.getText().trim();

        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter a username",
                    "Login Error",
                    JOptionPane.ERROR_MESSAGE);
            usernameField.requestFocus();
            return;
        }

        // Create user with appropriate role
        String role = adminButton.isSelected() ? User.ROLE_ADMINISTRATOR : User.ROLE_CUSTOMARY;

        User user = new User(username, role);

        // Create and show dashboard
        Dashboard dashboard = new Dashboard(user);
        dashboard.setVisible(true);

        // Close login screen
        dispose();
    }

    /**
     * Creates and displays the login screen
     */
    public static void main(String[] args) {
        // Set system look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Show login screen
        SwingUtilities.invokeLater(() -> {
            LoginScreen loginScreen = new LoginScreen();
            loginScreen.setVisible(true);
        });
    }
}
