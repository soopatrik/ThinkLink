package main.java.application;

import javax.swing.*;
import main.java.auth.LoginScreen;
import main.java.auth.LoginScreen.DashboardCreatedCallback;
import main.java.network.ServerConnection;
import org.json.*;

/**
 * Main entry point for the ThinkLink application
 */
public class ThinkLink {
    private static ServerConnection serverConnection;
    private static Dashboard currentDashboard;

    public static void main(String[] args) {
        // Set system look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Create server connection with message handler
        serverConnection = new ServerConnection(message -> {
            handleServerMessage(message);
        });

        // Start the application by showing the login screen
        SwingUtilities.invokeLater(() -> {
            LoginScreen loginScreen = new LoginScreen();
            loginScreen.setServerConnection(serverConnection);
            loginScreen.setDashboardCreatedCallback(dashboard -> currentDashboard = dashboard);
            loginScreen.setVisible(true);
        });
    }

    private static void handleServerMessage(JSONObject message) {
        try {
            String type = message.getString("type");
            System.out.println("Received from server: " + message.toString());

            // Use SwingUtilities to ensure UI updates happen on EDT
            SwingUtilities.invokeLater(() -> {
                try {
                    if (currentDashboard != null) {
                        currentDashboard.handleServerMessage(message);
                        System.out.println("Message forwarded to dashboard");
                    }
                } catch (Exception e) {
                    System.out.println("Error in dashboard: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            System.out.println("Error processing message: " + e.getMessage());
        }
    }
}
