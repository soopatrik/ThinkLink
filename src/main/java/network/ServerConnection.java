package main.java.network;

import java.io.*;
import java.net.*;
import org.json.*;
import java.util.concurrent.*;
import javax.swing.SwingUtilities;

public class ServerConnection {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 9876;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean connected = false;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private MessageHandler messageHandler;

    public interface MessageHandler {
        void handleMessage(JSONObject message);
    }

    public ServerConnection(MessageHandler handler) {
        this.messageHandler = handler;
    }

    public boolean connect(String userEmail, String role) {
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            connected = true;

            // Start listening for messages
            executor.submit(this::listenForMessages);

            // Send login message
            JSONObject loginMessage = new JSONObject();
            loginMessage.put("type", "login");
            loginMessage.put("email", userEmail);
            loginMessage.put("role", role);
            sendMessage(loginMessage);

            return true;
        } catch (IOException e) {
            System.out.println("Failed to connect to server: " + e.getMessage());
            return false;
        }
    }

    public void disconnect() {
        connected = false;
        executor.shutdown();
        try {
            if (in != null)
                in.close();
            if (out != null)
                out.close();
            if (socket != null)
                socket.close();
        } catch (IOException e) {
            System.out.println("Error disconnecting: " + e.getMessage());
        }
    }

    public void sendMessage(JSONObject message) {
        if (connected && out != null) {
            out.println(message.toString());
        }
    }

    private void listenForMessages() {
        try {
            String message;
            while (connected && (message = in.readLine()) != null) {
                final String finalMessage = message;
                // Use SwingUtilities to handle UI updates on EDT
                SwingUtilities.invokeLater(() -> {
                    try {
                        JSONObject json = new JSONObject(finalMessage);
                        if (messageHandler != null) {
                            messageHandler.handleMessage(json);
                        }
                    } catch (Exception e) {
                        System.out.println("Error parsing server message: " + e.getMessage());
                    }
                });
            }
        } catch (IOException e) {
            if (connected) {
                System.out.println("Error reading from server: " + e.getMessage());
                disconnect();
            }
        }
    }

    public void close() {
        disconnect();
    }
}