package main.java.server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.JSONArray; // Required for parsing JSON
import org.json.JSONObject; // Required for parsing JSON
import main.java.utils.SharedState; // To load the board and find max ID

// Attempt to trigger ClientHandler static initialization early
import main.java.server.ClientHandler; // Make sure it's imported

public class ThinkLinkServer {
    private static final int PORT = 9876;
    public static Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private static Map<String, Set<String>> boardUsers = new ConcurrentHashMap<>();
    private static AtomicInteger globalBoxIdCounter; // Initialize in main after loading board

    public static void main(String[] args) {
        System.out.println("ThinkLinkServer.main(): Attempting to explicitly initialize ClientHandler...");
        try {
            Class.forName("main.java.server.ClientHandler");
            System.out.println(
                    "ThinkLinkServer.main(): ClientHandler class loaded and initialized successfully (static block should have run).");
        } catch (ClassNotFoundException e) {
            System.err.println(
                    "ThinkLinkServer.main(): CRITICAL - ClassNotFoundException for ClientHandler. Check classpath and class name.");
            e.printStackTrace();
            return; // Stop if we can't even find the class
        } catch (Throwable t) { // Catching Throwable to see any errors during static initialization
            System.err.println(
                    "ThinkLinkServer.main(): CRITICAL - Error during ClientHandler static initialization (Class.forName).");
            t.printStackTrace();
            return; // Stop if static initialization fails
        }

        // Initialize globalBoxIdCounter after loading the board state
        int maxId = 0;
        try {
            JSONObject board = SharedState.loadSharedBoard(); // Load the board
            if (board != null && board.has("boxes")) {
                JSONArray boxes = board.getJSONArray("boxes");
                for (int i = 0; i < boxes.length(); i++) {
                    if (boxes.getJSONObject(i).has("id")) {
                        int currentId = boxes.getJSONObject(i).getInt("id");
                        if (currentId > maxId) {
                            maxId = currentId;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println(
                    "ThinkLinkServer.main(): Error loading shared board to determine max ID: " + e.getMessage());
            // Default to 0 if loading fails, so counter starts at 1. Or a higher default.
        }
        globalBoxIdCounter = new AtomicInteger(maxId + 1); // Start counter after the max existing ID
        System.out.println(
                "ThinkLinkServer.main(): Initialized globalBoxIdCounter to start at: " + globalBoxIdCounter.get());

        ThinkLinkServer server = new ThinkLinkServer();
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("ThinkLink Server started on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress().getHostAddress());

                server.acceptClient(clientSocket);
            }
        } catch (IOException e) {
            System.out.println("Server exception: " + e.getMessage());
            // Consider logging the stack trace for IOExceptions as well
            // e.printStackTrace();
        } catch (Throwable t) { // Catch any other unexpected errors in the main server loop
            System.err.println("ThinkLinkServer.main(): CRITICAL - Unhandled error in server loop.");
            t.printStackTrace();
        }
    }

    private void acceptClient(Socket clientSocket) {
        System.out.println("ThinkLinkServer.acceptClient(): Attempting to create new ClientHandler instance...");
        try {
            ClientHandler clientHandler = new ClientHandler(clientSocket, this);
            new Thread(clientHandler).start();
            System.out.println(
                    "ThinkLinkServer.acceptClient(): Successfully created and started new ClientHandler thread.");
        } catch (Throwable t) {
            System.err.println(
                    "ThinkLinkServer.acceptClient(): CRITICAL - Error creating or starting ClientHandler instance.");
            t.printStackTrace();
            // Optionally, close the clientSocket if handler creation fails
            try {
                clientSocket.close();
            } catch (IOException ioe) {
                System.err.println(
                        "ThinkLinkServer.acceptClient(): IOException while closing client socket after handler creation failure.");
                ioe.printStackTrace();
            }
        }
    }

    public static void broadcast(String message, String excludeUser) {
        for (Map.Entry<String, ClientHandler> entry : clients.entrySet()) {
            if (!entry.getKey().equals(excludeUser)) {
                entry.getValue().sendMessage(message);
            }
        }
    }

    public static void broadcastToBoard(String boardId, String message, String excludeUser) {
        System.out.println("Broadcasting to board " + boardId + ": " + message);

        // Broadcast to ALL connected clients except sender
        for (Map.Entry<String, ClientHandler> entry : clients.entrySet()) {
            // Check if client is on the target board and not the excluded user.
            // ClientHandler's boardId might be null initially, so handle that.
            ClientHandler handler = entry.getValue();
            String handlerBoardId = handler.getBoardId(); // Assuming getBoardId() exists and is safe to call

            if (handlerBoardId != null && handlerBoardId.equals(boardId) && !entry.getKey().equals(excludeUser)) {
                try {
                    System.out.println("Sending to " + entry.getKey() + " on board " + boardId);
                    handler.sendMessage(message);
                } catch (Exception e) {
                    System.out.println("Error sending to " + entry.getKey() + ": " + e.getMessage());
                    e.printStackTrace(); // Good to see stack trace for errors here
                }
            }
        }
    }

    public static void addUserToBoard(String boardId, String userEmail) {
        boardUsers.computeIfAbsent(boardId, k -> new HashSet<>()).add(userEmail);
        System.out.println("User " + userEmail + " joined board: " + boardId);
    }

    public static void removeUserFromBoard(String boardId, String userEmail) {
        Set<String> users = boardUsers.get(boardId);
        if (users != null) {
            users.remove(userEmail);
            if (users.isEmpty()) {
                boardUsers.remove(boardId);
            }
        }
        System.out.println("User " + userEmail + " left board: " + boardId);
    }

    public static int getNextGlobalBoxId() {
        int nextId = globalBoxIdCounter.getAndIncrement();
        System.out.println("ThinkLinkServer.getNextGlobalBoxId(): Assigning new ID: " + nextId);
        return nextId;
    }

    public synchronized void broadcastMessage(String message, String boardId, ClientHandler sender) {
        if (boardId == null) {
            System.err.println("broadcastMessage: boardId is null. Cannot broadcast.");
            return;
        }
        String senderEmail = (sender != null) ? sender.getUserEmail() : "null_sender";
        boolean messageSentToAnyone = false;
        for (ClientHandler client : clients.values()) {
            if (client != sender && client.getBoardId() != null && client.getBoardId().equals(boardId)) {
                System.out
                        .println("Server broadcasting (to others on board " + boardId + ") from " + senderEmail + " to "
                                + client.getUserEmail() + ": " + message);
                client.sendMessage(message);
                messageSentToAnyone = true;
            }
        }
        if (!messageSentToAnyone) {
            System.out.println("broadcastMessage: No other clients found on board " + boardId
                    + " to send message to (sender: " + senderEmail + ")");
        }
    }

    public synchronized void broadcastToBoard(String message, String boardId) {
        if (boardId == null) {
            System.err.println("broadcastToBoard: boardId is null. Cannot broadcast.");
            return;
        }
        System.out.println("Server broadcasting (to all on board " + boardId + "): " + message);
        boolean messageSentToAnyone = false;
        for (ClientHandler client : clients.values()) {
            // Ensure client has joined a board and it's the target board
            String clientBoardId = client.getBoardId(); // Assuming getBoardId() exists
            if (clientBoardId != null && clientBoardId.equals(boardId)) {
                System.out.println(
                        "broadcastToBoard: Sending to client " + client.getUserEmail() + " on board " + boardId);
                client.sendMessage(message);
                messageSentToAnyone = true;
            } else {
                System.out.println("broadcastToBoard: Skipping client " + client.getUserEmail() + " (board: "
                        + clientBoardId + ")");
            }
        }
        if (!messageSentToAnyone) {
            System.out.println("broadcastToBoard: No clients found on board " + boardId + " to send message to.");
        }
    }

    public synchronized void addClient(ClientHandler clientHandler) {
        String email = clientHandler.getUserEmail();
        if (email != null) {
            clients.put(email, clientHandler);
            System.out.println("ThinkLinkServer.addClient: Added client " + email + " to active clients map.");
        } else {
            System.err.println("ThinkLinkServer.addClient: ERROR - Attempted to add a client handler with null email.");
        }
    }

    public synchronized void removeClient(ClientHandler clientHandler) {
        String email = clientHandler.getUserEmail();
        if (email != null) {
            clients.remove(email);
            System.out.println("ThinkLinkServer.removeClient: Removed client " + email + " from active clients map.");
        } else {
            System.err.println(
                    "ThinkLinkServer.removeClient: ERROR - Attempted to remove a client handler with null email.");
            // Optionally, iterate and remove by object instance if email is null, though
            // this is less ideal
        }
    }

    public static Set<String> getClientsOnBoard(String boardId) {
        return boardUsers.getOrDefault(boardId, new HashSet<>());
    }

    public static ClientHandler getClientHandler(String userEmail) {
        return clients.get(userEmail);
    }
}