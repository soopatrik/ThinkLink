package main.java.server;

import java.io.*;
import java.net.*;
import org.json.*;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import main.java.utils.SharedState;
import java.util.List;
import java.util.Set;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private String userEmail;
    private ThinkLinkServer server;
    private String boardId;

    // Reference SharedState's path logic if possible, or redefine consistently.
    // For simplicity here, we'll assume SharedState is accessible or redefine path
    // logic.
    // Ideally, this path should come from a shared configuration.
    private static final String APP_DATA_DIR_NAME_CH = ".thinklink"; // Duplicated for now
    private static final String SHARED_BOARD_FILE_NAME_CH = "shared_board.json"; // Duplicated for now
    private static final Path SHARED_BOARD_PATH_CH;

    static {
        Path tempPath = null;
        System.out.println("ClientHandler static initializer: Starting.");
        try {
            String userHome = System.getProperty("user.home");
            if (userHome == null || userHome.trim().isEmpty()) {
                System.err.println(
                        "ClientHandler static initializer: ERROR - System.getProperty(\"user.home\") returned null or empty.");
                // Consider throwing an error or setting a default path if appropriate,
                // but for now, logging the error is key.
                // This will likely cause tempPath to remain null if not handled.
            } else {
                Path appDataDir = Paths.get(userHome, APP_DATA_DIR_NAME_CH);
                // Ensure the directory exists (SharedState usually does this, but good for CH
                // to be aware)
                if (!Files.exists(appDataDir)) {
                    try {
                        Files.createDirectories(appDataDir);
                        System.out.println("ClientHandler static initializer: Created application data directory: "
                                + appDataDir.toString());
                    } catch (IOException e) {
                        System.err
                                .println("ClientHandler static initializer: ERROR creating application data directory: "
                                        + appDataDir.toString() + " - " + e.getMessage());
                        // tempPath will remain null or an error will be thrown
                    }
                }
                if (Files.exists(appDataDir)) { // Only proceed if directory exists or was created
                    tempPath = appDataDir.resolve(SHARED_BOARD_FILE_NAME_CH);
                    System.out
                            .println("ClientHandler static initializer: Successfully determined SHARED_BOARD_PATH_CH: "
                                    + tempPath.toString());
                } else {
                    System.err.println(
                            "ClientHandler static initializer: ERROR - App data directory does not exist and could not be created: "
                                    + appDataDir.toString());
                }
            }
        } catch (Throwable t) { // Catch Throwable to see everything, including Errors
            System.err
                    .println("ClientHandler static initializer: CRITICAL UNHANDLED ERROR during path initialization.");
            t.printStackTrace();
            // tempPath will likely be null
        }
        SHARED_BOARD_PATH_CH = tempPath;
        if (SHARED_BOARD_PATH_CH == null) {
            System.err.println(
                    "ClientHandler static initializer: CRITICAL - SHARED_BOARD_PATH_CH is NULL after initialization attempt!");
        } else {
            System.out.println("ClientHandler static initializer: Final SHARED_BOARD_PATH_CH is: "
                    + SHARED_BOARD_PATH_CH.toString());
        }
    }

    public ClientHandler(Socket socket, ThinkLinkServer serverInstance) {
        this.clientSocket = socket;
        this.server = serverInstance;
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                System.out.println("SERVER RECEIVED (" + (userEmail != null ? userEmail : "PRE_LOGIN") + ", Board: "
                        + (boardId != null ? boardId : "N/A") + "): "
                        + inputLine.substring(0, Math.min(inputLine.length(), 150))); // Log truncated message
                JSONObject json = null;
                try {
                    json = new JSONObject(inputLine);
                    String type = json.optString("type", "unknown");

                    switch (type) {
                        case "login":
                            handleLogin(json);
                            break;
                        case "join_board":
                            handleJoinBoard(json);
                            break;
                        case "client_request_add_box":
                            handleClientRequestAddBox(json);
                            break;
                        case "update_box":
                            handleUpdateBox(json); // Single call to the corrected method
                            break;
                        case "delete_box":
                            handleDeleteBox(json);
                            break;
                        case "add_connection":
                            handleAddConnection(json);
                            break;
                        case "delete_connection":
                            handleDeleteConnection(json);
                            break;
                        default:
                            System.out.println("ClientHandler (" + (userEmail != null ? userEmail : "UNKNOWN_USER") +
                                    ", Board: " + (boardId != null ? boardId : "N/A") +
                                    "): Received unhandled/generic message type '" + type
                                    + "'. Relaying if on a board.");
                            if (this.boardId != null) {
                                broadcastToOthersOnBoard(inputLine);
                            } else {
                                System.out.println("ClientHandler: Message type '" + type
                                        + "' received but client not on a board. Message not relayed.");
                            }
                            break;
                    }
                } catch (JSONException e) {
                    System.err.println("ClientHandler (" + (userEmail != null ? userEmail : "UNKNOWN_USER") +
                            "): JSONException parsing message: "
                            + inputLine.substring(0, Math.min(inputLine.length(), 150)) + " - " + e.getMessage());
                }
            }
        } catch (SocketException se) {
            System.out.println("ClientHandler (" + (userEmail != null ? userEmail : "SOCKET_CLOSED") +
                    "): SocketException (client likely disconnected or stream closed): " + se.getMessage());
        } catch (IOException ioe) {
            System.err.println("ClientHandler (" + (userEmail != null ? userEmail : "IO_ERROR") +
                    "): IOException in run loop: " + ioe.getMessage());
        } catch (Exception e) {
            System.err.println("ClientHandler (" + (userEmail != null ? userEmail : "GENERAL_ERROR") +
                    "): Unexpected error in run loop: " + e.getMessage());
            e.printStackTrace();
        } finally {
            handleDisconnection();
        }
    }

    private void handleLogin(JSONObject json) {
        userEmail = json.getString("email");
        this.server.addClient(this);
        System.out.println("User logged in: " + userEmail);

        JSONObject confirmation = new JSONObject();
        confirmation.put("type", "login_confirmed");
        confirmation.put("message", "Successfully connected to ThinkLink server. Please join a board.");
        sendMessage(confirmation.toString());
    }

    private void handleJoinBoard(JSONObject json) {
        String newBoardId = json.getString("boardId");
        this.boardId = newBoardId;
        ThinkLinkServer.addUserToBoard(this.boardId, this.userEmail);
        System.out.println("ClientHandler (" + userEmail + "): User joined board: " + this.boardId);
        sendInitialBoardState(this.userEmail, this.boardId);
    }

    private void sendInitialBoardState(String username, String targetBoardId) {
        System.out.println(
                "ClientHandler (" + username + "): Preparing initial board state for board '" + targetBoardId + "'.");
        try {
            JSONObject boardStateJson = SharedState.loadSharedBoard();
            if (targetBoardId != null && !targetBoardId.equals("shared-global-board")
                    && !targetBoardId.equals("global-shared-board")) {
                System.err.println("ClientHandler (" + username + "): WARNING - Target board is '" + targetBoardId
                        + "'. SharedState currently loads a global board. Sending this global state tagged with targetBoardId.");
            }

            if (boardStateJson != null && !boardStateJson.has("error")) {
                JSONObject message = new JSONObject();
                message.put("type", "initial_board_state");
                message.put("boardId", targetBoardId);
                message.put("boardState", boardStateJson);
                sendMessage(message.toString());
                int boxCount = boardStateJson.optJSONArray("boxes") != null
                        ? boardStateJson.getJSONArray("boxes").length()
                        : 0;
                System.out.println("ClientHandler (" + username + "): Sent initial_board_state for board '"
                        + targetBoardId + "'. Boxes: " + boxCount);
            } else {
                String errorReason = boardStateJson != null
                        ? boardStateJson.optString("error", "Unknown load error from SharedState")
                        : "SharedState.loadSharedBoard() returned null";
                System.err.println(
                        "ClientHandler (" + username + "): Failed to load board state via SharedState for board '"
                                + targetBoardId + "'. Reason: " + errorReason);
                sendErrorState(targetBoardId, "Could not load board state from server: " + errorReason);
            }
        } catch (Exception e) {
            System.err.println("ClientHandler (" + username + "): Exception sending initial board state for board '"
                    + targetBoardId + "': " + e.getMessage());
            e.printStackTrace();
            sendErrorState(targetBoardId, "Server exception while preparing board state.");
        }
    }

    private void sendErrorState(String targetBoardId, String errorMessage) {
        JSONObject errorMsg = new JSONObject();
        errorMsg.put("type", "error_initial_board_state");
        errorMsg.put("boardId", targetBoardId);
        errorMsg.put("message", errorMessage);
        sendMessage(errorMsg.toString());
    }

    private void handleClientRequestAddBox(JSONObject json) {
        String userEmailRequesting = json.getString("userEmail");
        String title = json.getString("title");
        String content = json.optString("content", "");
        int x = json.getInt("x");
        int y = json.getInt("y");
        String requestBoardId = json.getString("boardId");

        if (this.boardId == null || !this.boardId.equals(requestBoardId)) {
            System.err.println("ClientHandler (" + (this.userEmail != null ? this.userEmail : "HANDLER_NO_EMAIL") +
                    ") handleClientRequestAddBox: Board ID mismatch. Handler board: " + this.boardId +
                    ", Request board: " + requestBoardId + ". Ignoring request.");
            return;
        }
        if (this.userEmail == null || !this.userEmail.equals(userEmailRequesting)) { // Check if authenticated user
                                                                                     // matches request
            System.err.println("ClientHandler (" + (this.userEmail != null ? this.userEmail : "HANDLER_NO_EMAIL") +
                    ") handleClientRequestAddBox: Mismatch/null handler user and requesting user ("
                    + userEmailRequesting + "). Using handler user if available.");
            if (this.userEmail == null) { // Cannot proceed if handler itself isn't authenticated
                System.err.println(
                        "ClientHandler: CRITICAL - handleClientRequestAddBox called but handler has no userEmail. Aborting add box.");
                return;
            }
        }

        int newBoxId = ThinkLinkServer.getNextGlobalBoxId();

        JSONObject addBoxMessageForBroadcast = new JSONObject();
        addBoxMessageForBroadcast.put("type", "add_box");
        addBoxMessageForBroadcast.put("boardId", this.boardId);
        addBoxMessageForBroadcast.put("userEmail", this.userEmail);
        addBoxMessageForBroadcast.put("boxId", newBoxId);
        addBoxMessageForBroadcast.put("title", title);
        addBoxMessageForBroadcast.put("content", content);
        addBoxMessageForBroadcast.put("x", x);
        addBoxMessageForBroadcast.put("y", y);
        addBoxMessageForBroadcast.put("connections", new JSONArray());

        this.server.broadcastToBoard(addBoxMessageForBroadcast.toString(), this.boardId);
        System.out.println("ClientHandler (" + this.userEmail + "): Server broadcasting add_box for board "
                + this.boardId + ", new ID " + newBoxId);

        JSONObject boxDataForState = new JSONObject();
        boxDataForState.put("id", newBoxId);
        boxDataForState.put("title", title);
        boxDataForState.put("content", content);
        boxDataForState.put("x", x);
        boxDataForState.put("y", y);
        boxDataForState.put("connections", new JSONArray());

        SharedState.updateServerBoardState(this.boardId, boxDataForState);
        System.out.println("ClientHandler (" + this.userEmail + "): New box ID " + newBoxId
                + " data saved to SharedState for board " + this.boardId);
    }

    // Single definition of handleUpdateBox
    private void handleUpdateBox(JSONObject jsonMessageFromClient) {
        String messageBoardId = jsonMessageFromClient.getString("boardId");
        if (this.boardId == null || !this.boardId.equals(messageBoardId)) {
            System.err.println("ClientHandler (" + (this.userEmail != null ? this.userEmail : "UNKNOWN") +
                    ") handleUpdateBox: Board ID mismatch. Handler: " + this.boardId +
                    ", Msg: " + messageBoardId + ". Ignoring.");
            return;
        }
        if (this.userEmail == null) {
            System.err.println(
                    "ClientHandler: handleUpdateBox called but handler has no userEmail. Aborting update box.");
            return;
        }

        // Ensure userEmail in the broadcast message is this client's email for
        // attribution
        String originalUser = jsonMessageFromClient.optString("userEmail", this.userEmail);
        if (!originalUser.equals(this.userEmail)) {
            System.out.println(
                    "ClientHandler (" + this.userEmail + ") handleUpdateBox: Message userEmail (" + originalUser
                            + ") differs from handler. Overwriting with handler's email for broadcast attribution.");
        }
        jsonMessageFromClient.put("userEmail", this.userEmail); // Standardize userEmail for broadcast

        System.out.println("ClientHandler (" + this.userEmail + "): Received update_box for board " + messageBoardId
                + ". Relaying.");
        broadcastToOthersOnBoard(jsonMessageFromClient.toString());

        JSONObject boxDataToUpdate = new JSONObject();
        boxDataToUpdate.put("id", jsonMessageFromClient.getInt("boxId"));
        boxDataToUpdate.put("title", jsonMessageFromClient.getString("title"));
        boxDataToUpdate.put("content", jsonMessageFromClient.optString("content", ""));
        boxDataToUpdate.put("x", jsonMessageFromClient.getInt("x"));
        boxDataToUpdate.put("y", jsonMessageFromClient.getInt("y"));
        JSONArray connections = jsonMessageFromClient.optJSONArray("connections");
        boxDataToUpdate.put("connections", connections != null ? connections : new JSONArray());

        SharedState.updateServerBoardState(this.boardId, boxDataToUpdate);
        System.out.println("ClientHandler (" + this.userEmail + "): Updated box data (ID: "
                + jsonMessageFromClient.optInt("boxId") + ") saved to SharedState for board " + this.boardId);
    }

    private void handleDeleteBox(JSONObject message) {
        int boxId = message.getInt("boxId");
        String messageBoardId = message.getString("boardId");

        System.out.println("ClientHandler (" + userEmail + "): Processing delete_box request for box " + boxId);

        // Remove from server state
        SharedState.removeBoxFromServerState(messageBoardId, boxId);

        // Broadcast to ALL clients on this board (including sender)
        Set<String> clientEmailsOnBoard = ThinkLinkServer.getClientsOnBoard(messageBoardId);
        for (String clientEmail : clientEmailsOnBoard) {
            ClientHandler client = ThinkLinkServer.getClientHandler(clientEmail);
            if (client != null) {
                client.sendMessage(message.toString());
            }
        }

        System.out.println("ClientHandler (" + userEmail + "): Deleted box " + boxId + " and broadcast to "
                + clientEmailsOnBoard.size() + " clients");
    }

    private void handleAddConnection(JSONObject jsonMessage) {
        try {
            int sourceBoxId = jsonMessage.getInt("sourceBoxId");
            int targetBoxId = jsonMessage.getInt("targetBoxId");
            String boardId = this.boardId;

            System.out.println("ClientHandler (" + userEmail + "): Received add_connection for board " + boardId
                    + ". Broadcasting to all.");

            // Use broadcastToBoard which sends to ALL clients including sender
            this.server.broadcastToBoard(jsonMessage.toString(), boardId);

            // Update server state
            SharedState.addConnectionToServerState(boardId, sourceBoxId, targetBoxId);
            System.out.println("ClientHandler (" + userEmail + "): Added connection from " + sourceBoxId + " to "
                    + targetBoxId + " in SharedState for board " + boardId);

        } catch (Exception e) {
            System.err.println("Error handling add_connection: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleDeleteConnection(JSONObject message) {
        int sourceBoxId = message.getInt("sourceBoxId");
        int targetBoxId = message.getInt("targetBoxId");
        String messageBoardId = message.getString("boardId");

        System.out.println("ClientHandler (" + userEmail + "): Processing delete_connection request: " + sourceBoxId
                + " -> " + targetBoxId);

        // Remove connection from server state
        SharedState.removeConnectionFromServerState(messageBoardId, sourceBoxId, targetBoxId);

        // Broadcast to ALL clients on this board (including sender)
        Set<String> clientEmailsOnBoard = ThinkLinkServer.getClientsOnBoard(messageBoardId);
        for (String clientEmail : clientEmailsOnBoard) {
            ClientHandler client = ThinkLinkServer.getClientHandler(clientEmail);
            if (client != null) {
                client.sendMessage(message.toString());
            }
        }

        System.out.println("ClientHandler (" + userEmail + "): Deleted connection " + sourceBoxId + " -> " + targetBoxId
                + " and broadcast to " + clientEmailsOnBoard.size() + " clients");
    }

    // Method to broadcast messages to other clients on the same board
    private void broadcastToOthersOnBoard(String message) {
        if (this.userEmail == null || this.boardId == null) {
            System.err.println("ClientHandler: Cannot broadcast. User email or boardId is not set. Email: "
                    + this.userEmail + ", BoardID: " + this.boardId);
            return;
        }
        this.server.broadcastMessage(message, this.boardId, this); // Delegates to server's method
    }

    public void sendMessage(String message) {
        if (out != null && clientSocket != null && !clientSocket.isClosed() && !out.checkError()) {
            out.println(message);
        } else {
            System.err.println("ClientHandler (" + (userEmail != null ? userEmail : "NO_USER_EMAIL")
                    + "): PrintWriter is null, socket closed, or has error. Cannot send message: "
                    + message.substring(0, Math.min(message.length(), 70)) + "...");
        }
    }

    private void handleDisconnection() {
        String userEmailAtDisconnect = this.userEmail; // Capture before nullifying
        String boardIdAtDisconnect = this.boardId;

        System.out.println(
                "ClientHandler (" + (userEmailAtDisconnect != null ? userEmailAtDisconnect : "CLOSING_CONNECTION")
                        + "): Handling disconnection.");

        if (userEmailAtDisconnect != null) {
            this.server.removeClient(this);
            if (boardIdAtDisconnect != null) {
                ThinkLinkServer.removeUserFromBoard(boardIdAtDisconnect, userEmailAtDisconnect);
                JSONObject disconnectMsg = new JSONObject();
                disconnectMsg.put("type", "user_disconnected");
                disconnectMsg.put("userEmail", userEmailAtDisconnect);
                disconnectMsg.put("boardId", boardIdAtDisconnect);
                this.server.broadcastMessage(disconnectMsg.toString(), boardIdAtDisconnect, this); // 'this' is okay as
                                                                                                   // sender context,
                                                                                                   // though client is
                                                                                                   // disconnecting
            }
            System.out.println("User '" + userEmailAtDisconnect + "' disconnected and removed from server lists.");
        }
        // Nullify fields after using them for notifications
        this.userEmail = null;
        this.boardId = null;

        try {
            if (in != null)
                in.close();
        } catch (IOException e) {
            /* Ignored */ }
        try {
            if (out != null)
                out.close();
        } catch (Exception e) {
            /* Ignored */ }
        try {
            if (clientSocket != null && !clientSocket.isClosed())
                clientSocket.close();
        } catch (IOException e) {
            /* Ignored */ }
        System.out.println("ClientHandler: Resources closed for ("
                + (userEmailAtDisconnect != null ? userEmailAtDisconnect : "UNKNOWN_DISCONNECTED_CLIENT") + ").");
    }

    public String getUserEmail() {
        return userEmail; // Might be null if called after disconnection
    }

    public String getBoardId() {
        return boardId; // Might be null
    }
}