package main.java.board;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import main.java.auth.User;
import main.java.network.ServerConnection;
import org.json.*;
import main.java.utils.SharedState;
import main.java.application.Dashboard;

public class BoardPanel extends JPanel implements MouseListener, MouseMotionListener {
	private User user;
	private BoxList boxList;
	private LineList lineList;
	private Box selectedBox;
	private Point dragStart;
	private boolean isDragging = false;
	private boolean connectingMode = false;
	private Box firstSelectedBox = null;
	private ServerConnection serverConnection;
	private String boardId;
	private Dashboard dashboard;

	public BoardPanel(User user, ServerConnection serverConnection, String boardId, Dashboard dashboard) {
		this.user = user;
		this.serverConnection = serverConnection;
		this.boardId = (boardId == null || boardId.trim().isEmpty()) ? "global-shared-board" : boardId;
		this.dashboard = dashboard;

		String userEmailForLog = (user != null ? user.getUserEmail() : "USER_NULL");
		System.out.println("BoardPanel CONSTRUCTOR (" + userEmailForLog + "): BoardId: " + this.boardId +
				", ServerConnection: " + (serverConnection != null) +
				", Dashboard: " + (dashboard != null));

		boxList = new BoxList();
		lineList = new LineList();

		loadBoardFromJSON(SharedState.loadSharedBoard(), userEmailForLog, true);

		setBackground(new Color(240, 240, 240));
		addMouseListener(this);
		addMouseMotionListener(this);

		if (this.serverConnection != null && this.boardId != null) {
			JSONObject joinMessage = new JSONObject();
			joinMessage.put("type", "join_board");
			joinMessage.put("boardId", this.boardId);
			joinMessage.put("userEmail", userEmailForLog);
			this.serverConnection.sendMessage(joinMessage);
			System.out.println(
					"BoardPanel (" + userEmailForLog + "): Sent 'join_board' message for boardId: " + this.boardId);
		}
	}

	public BoardPanel(User user) {
		this(user, null, "global-shared-board", null);
	}

	public BoardPanel(User user, ServerConnection serverConnection, String boardId) {
		this(user, serverConnection, boardId, null);
	}

	private void startConnectingBoxes() {
		connectingMode = true;
		firstSelectedBox = null;
		if (selectedBox != null) {
			selectedBox.setSelected(false);
			selectedBox = null;
		}
		setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
		JOptionPane.showMessageDialog(this, "Click on the first task, then the second task to connect them.",
				"Connect Tasks", JOptionPane.INFORMATION_MESSAGE);
	}

	private void sendBoxUpdate(Box boxToUpdate, String loggerUserEmail, String reason) {
		if (boxToUpdate == null || serverConnection == null || boardId == null)
			return;
		JSONObject message = new JSONObject();
		message.put("type", "update_box");
		message.put("boardId", boardId);
		message.put("userEmail", loggerUserEmail);
		message.put("boxId", boxToUpdate.getId());
		message.put("x", boxToUpdate.getBoxX());
		message.put("y", boxToUpdate.getBoxY());
		message.put("title", boxToUpdate.getTitle());
		message.put("content", boxToUpdate.getContent());
		JSONArray connectionsArray = new JSONArray();
		if (boxToUpdate.getConnectedBoxIds() != null) {
			for (Integer connId : boxToUpdate.getConnectedBoxIds()) {
				connectionsArray.put(connId);
			}
		}
		message.put("connections", connectionsArray);
		serverConnection.sendMessage(message);
		System.out.println(
				"BoardPanel (" + loggerUserEmail + ") SEND_BOX_UPDATE (" + reason + "): Box ID " + boxToUpdate.getId());
	}

	private void sendConnectionUpdate(int sourceBoxId, int targetBoxId, String loggerUserEmail, String reason) {
		try {
			// Attempt local update FIRST for immediate visual feedback
			Box sourceBox = boxList.getBoxById(sourceBoxId);
			Box targetBox = boxList.getBoxById(targetBoxId);

			if (sourceBox != null && targetBox != null) {
				boolean added = sourceBox.addConnection(targetBoxId);
				rebuildLinesFromBoxConnections(loggerUserEmail + "_LOCAL_AFTER_BOX_UPDATE");
				repaint();
			}

			// Send to server for synchronization with other clients
			if (serverConnection == null || boardId == null) {
				return;
			}

			JSONObject message = new JSONObject();
			message.put("type", "add_connection");
			message.put("boardId", boardId);
			message.put("userEmail", loggerUserEmail);
			message.put("sourceBoxId", sourceBoxId);
			message.put("targetBoxId", targetBoxId);

			serverConnection.sendMessage(message);
		} catch (Exception e) {
			System.err.println("Error in sendConnectionUpdate: " + e.getMessage());
		}
	}

	private void rebuildLinesFromBoxConnections(String loggerUserEmail) {
		System.out.println("BoardPanel (" + loggerUserEmail + ") REBUILD_LINES: Starting rebuild");

		if (lineList == null) {
			System.err.println("BoardPanel (" + loggerUserEmail + ") REBUILD_LINES: lineList is null!");
			return;
		}
		if (boxList == null) {
			System.err.println("BoardPanel (" + loggerUserEmail + ") REBUILD_LINES: boxList is null!");
			return;
		}

		// Clear existing lines using the correct method name
		try {
			lineList.clear();
			System.out.println("BoardPanel (" + loggerUserEmail + ") REBUILD_LINES: Cleared existing lines");
		} catch (Exception e) {
			System.err.println(
					"BoardPanel (" + loggerUserEmail + ") REBUILD_LINES: Error clearing lines: " + e.getMessage());
			return;
		}

		int linesAdded = 0;
		Box currentBox = boxList.getFirstNode();

		while (currentBox != null) {
			List<Integer> connectedIds = currentBox.getConnectedBoxIds();
			if (connectedIds != null && !connectedIds.isEmpty()) {
				System.out.println("BoardPanel (" + loggerUserEmail + ") REBUILD_LINES: Box " + currentBox.getId()
						+ " has " + connectedIds.size() + " connections: " + connectedIds);
				for (Integer targetId : connectedIds) {
					Box targetBox = boxList.getBoxById(targetId);
					if (targetBox != null) {
						try {
							lineList.addLine(currentBox, targetBox);
							linesAdded++;
							System.out.println(
									"BoardPanel (" + loggerUserEmail + ") REBUILD_LINES: Successfully added line "
											+ currentBox.getId() + " -> " + targetId);
						} catch (Exception e) {
							System.err.println("BoardPanel (" + loggerUserEmail + ") REBUILD_LINES: Error adding line "
									+ currentBox.getId() + " -> " + targetId + ": " + e.getMessage());
						}
					} else {
						System.err.println("BoardPanel (" + loggerUserEmail + ") REBUILD_LINES: Target box ID "
								+ targetId + " not found for source " + currentBox.getId());
					}
				}
			}
			currentBox = currentBox.getNext();
		}
		System.out.println(
				"BoardPanel (" + loggerUserEmail + ") REBUILD_LINES: Finished rebuild, added " + linesAdded + " lines");
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		if (lineList != null)
			lineList.draw(g2d);
		drawBoxes(g2d);
		if (connectingMode && firstSelectedBox != null) {
			Point p = getMousePosition();
			if (p != null) {
				g2d.setColor(Color.GRAY);
				g2d.setStroke(
						new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[] { 8 }, 0));
				Point start = firstSelectedBox.getCenter();
				g2d.drawLine(start.x, start.y, p.x, p.y);
			}
		}
	}

	private void drawBoxes(Graphics2D g2d) {
		if (boxList == null)
			return;
		for (Box current = boxList.getFirstNode(); current != null; current = current.getNext()) {
			drawBox(g2d, current);
		}
	}

	private void drawBox(Graphics2D g2d, Box box) {
		g2d.setColor(box.getBoxColor());
		g2d.fillRect(box.getBoxX(), box.getBoxY(), box.getBoxWidth(), box.getBoxHeight());
		if (box.isSelected()) {
			g2d.setColor(Color.RED);
			g2d.setStroke(new BasicStroke(2));
			g2d.drawRect(box.getBoxX() - 1, box.getBoxY() - 1, box.getBoxWidth() + 2, box.getBoxHeight() + 2);
		}
		g2d.setColor(Color.BLACK);
		g2d.setStroke(new BasicStroke(1));
		g2d.drawRect(box.getBoxX(), box.getBoxY(), box.getBoxWidth(), box.getBoxHeight());
		Font font = new Font("Arial", Font.BOLD, 12);
		g2d.setFont(font);
		g2d.setColor(Color.BLACK);
		drawCenteredString(g2d, box.getTitle(),
				new Rectangle(box.getBoxX(), box.getBoxY(), box.getBoxWidth(), box.getBoxHeight() / 3), font);
		Font contentFont = new Font("Arial", Font.PLAIN, 10);
		g2d.setFont(contentFont);
		drawWrappedText(g2d, box.getContent(), box.getBoxX() + 5, box.getBoxY() + box.getBoxHeight() / 3 + 5,
				box.getBoxWidth() - 10);
	}

	private void drawCenteredString(Graphics g, String text, Rectangle rect, Font font) {
		if (text == null)
			text = "";
		FontMetrics metrics = g.getFontMetrics(font);
		int x = rect.x + (rect.width - metrics.stringWidth(text)) / 2;
		int y = rect.y + ((rect.height - metrics.getHeight()) / 2) + metrics.getAscent();
		g.drawString(text, x, y);
	}

	private void drawWrappedText(Graphics2D g, String text, int x, int y, int width) {
		if (text == null || text.isEmpty() || width <= 0)
			return;
		FontMetrics fm = g.getFontMetrics();
		String[] lines = text.split("\n");
		for (String line : lines) {
			if (fm.stringWidth(line) <= width) {
				g.drawString(line, x, y);
				y += fm.getHeight();
			} else {
				String[] words = line.split(" ");
				if (words.length == 0)
					continue;
				StringBuilder currentLine = new StringBuilder(words[0]);
				for (int i = 1; i < words.length; i++) {
					if (fm.stringWidth(currentLine.toString() + " " + words[i]) < width) {
						currentLine.append(" ").append(words[i]);
					} else {
						g.drawString(currentLine.toString(), x, y);
						y += fm.getHeight();
						currentLine = new StringBuilder(words[i]);
					}
				}
				g.drawString(currentLine.toString(), x, y);
				y += fm.getHeight();
			}
		}
	}

	private Box findBoxAt(int x, int y) {
		if (boxList == null)
			return null;
		for (Box current = boxList.getFirstNode(); current != null; current = current.getNext()) {
			if (current.containsPoint(x, y)) {
				System.out.println("Found box " + current.getId() + " at (" + x + ", " + y + ")");
				return current;
			}
		}
		System.out.println("No box found at (" + x + ", " + y + ")");
		return null;
	}

	public Box findBoxById(int id) {
		if (boxList == null)
			return null;
		return boxList.getBoxById(id);
	}

	private void showBoxEditDialog(Box box) {
		String userEmailForLog = (user != null ? user.getUserEmail() : "USER_NULL");
		JTextField titleField = new JTextField(box.getTitle());
		JTextArea contentArea = new JTextArea(box.getContent(), 5, 20);
		contentArea.setLineWrap(true);
		contentArea.setWrapStyleWord(true);
		JScrollPane scrollPane = new JScrollPane(contentArea);
		scrollPane.setPreferredSize(new Dimension(250, 100));
		JPanel northPanel = new JPanel(new BorderLayout(5, 5));
		northPanel.add(new JLabel("Title:"), BorderLayout.WEST);
		northPanel.add(titleField, BorderLayout.CENTER);
		JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
		centerPanel.add(new JLabel("Content:"), BorderLayout.NORTH);
		centerPanel.add(scrollPane, BorderLayout.CENTER);
		JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
		mainPanel.add(northPanel, BorderLayout.NORTH);
		mainPanel.add(centerPanel, BorderLayout.CENTER);
		int result = JOptionPane.showConfirmDialog(this, mainPanel, "Edit Task (ID: " + box.getId() + ")",
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (result == JOptionPane.OK_OPTION) {
			String oldTitle = box.getTitle();
			String oldContent = box.getContent();
			String newTitle = titleField.getText();
			String newContent = contentArea.getText();
			boolean changed = false;
			if (!Objects.equals(newTitle, oldTitle)) {
				box.setTitle(newTitle);
				changed = true;
			}
			if (!Objects.equals(newContent, oldContent)) {
				box.setContent(newContent);
				changed = true;
			}
			if (changed) {
				repaint();
				sendBoxUpdate(box, userEmailForLog, "edit_dialog_changed");
			}
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		String userEmailForLog = (user != null ? user.getUserEmail() : "USER_NULL");

		if (SwingUtilities.isRightMouseButton(e)) {
			System.out.println(
					"BoardPanel (" + userEmailForLog + ") RIGHT_CLICK: at (" + e.getX() + ", " + e.getY() + ")");
			Box boxAtClick = findBoxAt(e.getX(), e.getY());
			System.out.println("BoardPanel (" + userEmailForLog + ") RIGHT_CLICK: boxAtClick = "
					+ (boxAtClick != null ? "Box " + boxAtClick.getId() : "null"));

			if (boxAtClick != null) {
				if (selectedBox != null && selectedBox != boxAtClick)
					selectedBox.setSelected(false);
				selectedBox = boxAtClick;
				selectedBox.setSelected(true);
				repaint();
			}

			// ALWAYS show context menu
			createContextMenu(e.getX(), e.getY(), boxAtClick);
			return;
		}

		if (connectingMode) {
			Box clickedBox = findBoxAt(e.getX(), e.getY());
			if (clickedBox != null) {
				if (firstSelectedBox == null) {
					firstSelectedBox = clickedBox;
					firstSelectedBox.setSelected(true);
					repaint();
				} else {
					if (firstSelectedBox.getId() != clickedBox.getId()) {
						sendConnectionUpdate(firstSelectedBox.getId(), clickedBox.getId(),
								(user != null ? user.getUserEmail() : "USER_NULL_CONN_MODE"),
								"connection_attempt");
						firstSelectedBox.setSelected(false);
					} else {
						firstSelectedBox.setSelected(false);
					}
					connectingMode = false;
					firstSelectedBox = null;
					setCursor(Cursor.getDefaultCursor());
					repaint();
				}
			} else {
				if (firstSelectedBox != null)
					firstSelectedBox.setSelected(false);
				connectingMode = false;
				firstSelectedBox = null;
				setCursor(Cursor.getDefaultCursor());
				repaint();
			}
		} else {
			Box clickedBox = findBoxAt(e.getX(), e.getY());
			if (clickedBox != null) {
				if (e.getClickCount() == 2) {
					if (selectedBox != null && selectedBox != clickedBox)
						selectedBox.setSelected(false);
					selectedBox = clickedBox;
					selectedBox.setSelected(true);
					showBoxEditDialog(clickedBox);
				} else {
					if (selectedBox != null && selectedBox != clickedBox)
						selectedBox.setSelected(false);
					selectedBox = clickedBox;
					selectedBox.setSelected(true);
				}
			} else {
				if (selectedBox != null) {
					selectedBox.setSelected(false);
					selectedBox = null;
				}
			}
			repaint();
		}
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (SwingUtilities.isRightMouseButton(e) || connectingMode)
			return;
		if (selectedBox != null)
			selectedBox.setSelected(false);
		selectedBox = null;
		Box pressedBox = findBoxAt(e.getX(), e.getY());
		if (pressedBox != null) {
			selectedBox = pressedBox;
			selectedBox.setSelected(true);
			dragStart = e.getPoint();
			isDragging = true;
		} else {
			isDragging = false;
		}
		repaint();
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (isDragging && selectedBox != null) {
			sendBoxUpdate(selectedBox, (user != null ? user.getUserEmail() : "USER_NULL"), "drag_release");
		}
		isDragging = false;
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if (isDragging && selectedBox != null && dragStart != null) {
			int dx = e.getX() - dragStart.x;
			int dy = e.getY() - dragStart.y;
			selectedBox.setBoxPosition(selectedBox.getBoxX() + dx, selectedBox.getBoxY() + dy);
			dragStart = e.getPoint();
			repaint();
		}
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		if (connectingMode && firstSelectedBox != null)
			repaint();
	}

	private void createContextMenu(int x, int y, Box clickedBox) {
		String userEmailForLog = (user != null ? user.getUserEmail() : "USER_NULL");
		System.out.println("BoardPanel (" + userEmailForLog + ") CREATE_CONTEXT_MENU: Clicked at (" + x + "," + y
				+ "),  Initial clickedBox=" + (clickedBox != null ? "Box " + clickedBox.getId() : "null"));

		JPopupMenu contextMenu = new JPopupMenu();

		// Make clickedBox effectively final for lambda usage
		final Box finalClickedBox;

		// Try to use selectedBox if clickedBox is null from right-click findBoxAt
		if (clickedBox == null && selectedBox != null) {
			System.out.println(
					"BoardPanel (" + userEmailForLog + ") CREATE_CONTEXT_MENU: clickedBox was null, but selectedBox is "
							+ selectedBox.getId() + ". Using selectedBox for menu.");
			finalClickedBox = selectedBox;
		} else {
			finalClickedBox = clickedBox;
		}

		if (finalClickedBox == null) {
			System.out.println("BoardPanel (" + userEmailForLog
					+ ") CREATE_CONTEXT_MENU: CLICKED BOX IS STILL NULL. Adding empty space menu items.");
			JMenuItem addTask = new JMenuItem("Add New Task");
			addTask.addActionListener(e -> {
				System.out.println("BoardPanel (" + (user != null ? user.getUserEmail() : "USER_NULL_CTX_ADD")
						+ ") CONTEXT_MENU: Add Task clicked");
				addBox(x, y, (user != null ? user.getUserEmail() : "USER_NULL_CTX_ADD"), boardId, serverConnection);
			});
			contextMenu.add(addTask);

			JMenuItem connectTasks = new JMenuItem("Connect Tasks");
			connectTasks.addActionListener(e -> {
				System.out.println("BoardPanel (" + userEmailForLog + ") CONTEXT_MENU: Connect Tasks clicked");
				startConnectingBoxes();
			});
			contextMenu.add(connectTasks);
		} else {
			System.out.println("BoardPanel (" + userEmailForLog + ") CREATE_CONTEXT_MENU: CLICKED BOX IS "
					+ finalClickedBox.getId() + ". Adding task-specific menu items.");

			JMenuItem editTask = new JMenuItem("Edit Task (ID: " + finalClickedBox.getId() + ")");
			editTask.addActionListener(e -> {
				System.out.println("BoardPanel (" + userEmailForLog + ") CONTEXT_MENU: Edit Task clicked for box "
						+ finalClickedBox.getId());
				showBoxEditDialog(finalClickedBox);
			});
			contextMenu.add(editTask);

			contextMenu.addSeparator();

			JMenuItem deleteTask = new JMenuItem("Delete Task (ID: " + finalClickedBox.getId() + ")");
			deleteTask.addActionListener(e -> {
				System.out.println("BoardPanel (" + userEmailForLog + ") CONTEXT_MENU: Delete Task clicked for box "
						+ finalClickedBox.getId());
				int confirm = JOptionPane.showConfirmDialog(this,
						"Are you sure you want to delete task '" + finalClickedBox.getTitle() + "'?",
						"Confirm Delete", JOptionPane.YES_NO_OPTION);
				if (confirm == JOptionPane.YES_OPTION) {
					sendDeleteBoxRequest(finalClickedBox.getId(), userEmailForLog);
				}
			});
			contextMenu.add(deleteTask);

			JMenuItem deleteConnections = new JMenuItem("Delete All Connections (ID: " + finalClickedBox.getId() + ")");
			deleteConnections.addActionListener(e -> {
				System.out.println("BoardPanel (" + userEmailForLog
						+ ") CONTEXT_MENU: Delete Connections clicked for box " + finalClickedBox.getId());
				int confirm = JOptionPane.showConfirmDialog(this,
						"Are you sure you want to delete all connections from '" + finalClickedBox.getTitle() + "'?",
						"Confirm Delete Connections", JOptionPane.YES_NO_OPTION);
				if (confirm == JOptionPane.YES_OPTION) {
					sendDeleteAllConnectionsRequest(finalClickedBox.getId(), userEmailForLog);
				}
			});
			contextMenu.add(deleteConnections);

			contextMenu.addSeparator();

			JMenuItem connectTasksFromBox = new JMenuItem(
					"Connect This Task (ID: " + finalClickedBox.getId() + ") to Another");
			connectTasksFromBox.addActionListener(e -> {
				System.out
						.println("BoardPanel (" + userEmailForLog + ") CONTEXT_MENU: Connect Tasks clicked (from box)");
				startConnectingBoxes();
			});
			contextMenu.add(connectTasksFromBox);
		}

		System.out.println("BoardPanel (" + userEmailForLog + ") CONTEXT_MENU: Showing menu with "
				+ contextMenu.getComponentCount() + " items at (" + x + "," + y + ")");
		contextMenu.show(this, x, y);
	}

	public BoxList getBoxList() {
		return boxList;
	}

	public LineList getLineList() {
		return lineList;
	}

	public void addBox(int x, int y, String loggerUserEmail, String actionBoardId,
			ServerConnection actionServerConnection) {
		String title = JOptionPane.showInputDialog(this, "Enter task title:", "New Task", JOptionPane.PLAIN_MESSAGE);
		if (title == null || title.trim().isEmpty())
			return;
		String content = JOptionPane.showInputDialog(this, "Enter task description (optional):", "Task Description",
				JOptionPane.PLAIN_MESSAGE);
		if (content == null)
			content = "";

		if (actionServerConnection != null && actionBoardId != null) {
			JSONObject request = new JSONObject();
			request.put("type", "client_request_add_box");
			request.put("boardId", actionBoardId);
			request.put("userEmail", loggerUserEmail);
			request.put("title", title);
			request.put("content", content);
			request.put("x", x);
			request.put("y", y);
			actionServerConnection.sendMessage(request);
			System.out.println(
					"BoardPanel (" + loggerUserEmail + ") ADD_BOX: Sent client_request_add_box for '" + title + "'");
		}
	}

	public void handleServerMessage(JSONObject message) {
		String type = message.optString("type");
		String userEmailForLog = (user != null ? user.getUserEmail() : "USER_NULL_IN_HSM");
		String receivedBoardId = message.optString("boardId", null);

		if (receivedBoardId == null || !receivedBoardId.equals(this.boardId)) {
			System.err.println("BoardPanel (" + userEmailForLog + ") MSG_RECV: Mismatched boardId. Expected '"
					+ this.boardId + "', got '" + receivedBoardId + "'. Ignoring type: " + type);
			return;
		}
		System.out.println("BoardPanel (" + userEmailForLog + ") MSG_RECV: Type '" + type + "' for board '"
				+ receivedBoardId + "'");

		boolean boardStructureChanged = false;

		switch (type) {
			case "add_box":
				handleRemoteAddBox(message);
				boardStructureChanged = true;
				break;
			case "update_box":
				handleRemoteUpdateBox(message);
				boardStructureChanged = true;
				break;
			case "delete_box":
				handleRemoteDeleteBox(message);
				boardStructureChanged = true;
				break;
			case "initial_board_state":
				handleInitialBoardState(message);
				boardStructureChanged = true;
				break;
			case "add_connection":
				handleRemoteAddConnection(message);
				boardStructureChanged = true;
				break;
			case "delete_connection":
				int sourceId = message.getInt("sourceBoxId");
				int targetId = message.getInt("targetBoxId");
				Box sourceBox = boxList.getBoxById(sourceId);
				if (sourceBox != null) {
					sourceBox.removeConnection(targetId);
					rebuildLinesFromBoxConnections(userEmailForLog);
					repaint();
				}
				boardStructureChanged = true;
				break;
			default:
				System.err.println("BoardPanel (" + userEmailForLog + ") MSG_RECV: Unknown type: " + type);
		}

		if (boardStructureChanged) {
			rebuildLinesFromBoxConnections(userEmailForLog + "/after_server_" + type);
		}
		repaint();
	}

	private void handleRemoteAddBox(JSONObject message) {
		int id = message.getInt("boxId");
		String title = message.getString("title");
		String content = message.optString("content", "");
		int x = message.getInt("x");
		int y = message.getInt("y");
		JSONArray connectionsJSON = message.optJSONArray("connections");
		List<Integer> connectedIds = new ArrayList<>();
		if (connectionsJSON != null) {
			for (int i = 0; i < connectionsJSON.length(); i++)
				connectedIds.add(connectionsJSON.getInt(i));
		}
		if (boxList.getBoxById(id) != null) {
			Box existingBox = boxList.getBoxById(id);
			existingBox.setBoxX(x);
			existingBox.setBoxY(y);
			existingBox.setTitle(title);
			existingBox.setContent(content);
			existingBox.setConnectedBoxIds(connectedIds);
		} else {
			Box newBox = new Box(x, y, title, content, id);
			newBox.setConnectedBoxIds(connectedIds);
			boxList.addNode(newBox);
		}
	}

	private void handleRemoteUpdateBox(JSONObject message) {
		int id = message.getInt("boxId");
		Box boxToUpdate = boxList.getBoxById(id);
		if (boxToUpdate != null) {
			boxToUpdate.setBoxX(message.getInt("x"));
			boxToUpdate.setBoxY(message.getInt("y"));
			boxToUpdate.setTitle(message.getString("title"));
			boxToUpdate.setContent(message.optString("content", boxToUpdate.getContent()));
			JSONArray connectionsArray = message.optJSONArray("connections");
			if (connectionsArray != null) {
				List<Integer> newConnectionIds = new ArrayList<>();
				for (int i = 0; i < connectionsArray.length(); i++)
					newConnectionIds.add(connectionsArray.getInt(i));
				boxToUpdate.setConnectedBoxIds(newConnectionIds);
			}
		} else {
			handleRemoteAddBox(message);
		}
	}

	private void handleRemoteDeleteBox(JSONObject message) {
		int boxIdToDelete = message.getInt("boxId");
		Box box = boxList.getBoxById(boxIdToDelete);
		if (box != null) {
			Box current = boxList.getFirstNode();
			while (current != null) {
				if (current.getId() != boxIdToDelete)
					current.getConnectedBoxIds().remove(Integer.valueOf(boxIdToDelete));
				current = current.getNext();
			}
			boxList.deleteNodeById(boxIdToDelete);
		}
	}

	private void handleInitialBoardState(JSONObject message) {
		String userEmailForLog = (user != null ? user.getUserEmail() : "USER_NULL_IN_HIBS");
		JSONObject boardState = message.optJSONObject("boardState");
		if (boardState != null) {
			loadBoardFromJSON(boardState, userEmailForLog, true);
		} else {
			if (boxList != null)
				boxList.clearList();
			if (lineList != null)
				lineList.clear();
		}
		rebuildLinesFromBoxConnections(userEmailForLog + "/after_initial_board_state");
		repaint();
	}

	private void handleRemoteAddConnection(JSONObject message) {
		int sourceBoxId = message.getInt("sourceBoxId");
		int targetBoxId = message.getInt("targetBoxId");
		Box sourceBox = boxList.getBoxById(sourceBoxId);
		Box targetBox = boxList.getBoxById(targetBoxId);
		if (sourceBox != null && targetBox != null) {
			sourceBox.addConnection(targetBoxId);
		}
	}

	public void loadBoardFromJSON(JSONObject sharedBoard, String loggerUserEmail, boolean initialLoad) {
		loggerUserEmail = (loggerUserEmail == null) ? (user != null ? user.getUserEmail() : "USER_NULL_IN_LBFJ")
				: loggerUserEmail;

		if (boxList == null)
			boxList = new BoxList();
		if (lineList == null)
			lineList = new LineList();

		if (initialLoad) {
			boxList.clearList();
			lineList.clear();
		}

		if (sharedBoard == null)
			return;

		JSONArray boxesArray = sharedBoard.optJSONArray("boxes");
		if (boxesArray != null) {
			for (int i = 0; i < boxesArray.length(); i++) {
				JSONObject boxJson = boxesArray.optJSONObject(i);
				if (boxJson != null) {
					int id = boxJson.getInt("id");
					Box existingBox = boxList.getBoxById(id);
					List<Integer> connectedIds = new ArrayList<>();
					JSONArray connectionsJson = boxJson.optJSONArray("connections");
					if (connectionsJson != null) {
						for (int j = 0; j < connectionsJson.length(); j++)
							connectedIds.add(connectionsJson.getInt(j));
					}

					if (existingBox != null) {
						if (initialLoad) {
							boxList.deleteNodeById(id);
							Box newBox = new Box(boxJson.getInt("x"), boxJson.getInt("y"), boxJson.getString("title"),
									boxJson.optString("content", ""), id);
							newBox.setConnectedBoxIds(connectedIds);
							boxList.addNode(newBox);
						} else {
							existingBox.setBoxX(boxJson.getInt("x"));
							existingBox.setBoxY(boxJson.getInt("y"));
							existingBox.setTitle(boxJson.getString("title"));
							existingBox.setContent(boxJson.optString("content", ""));
							existingBox.setConnectedBoxIds(connectedIds);
						}
					} else {
						Box newBox = new Box(boxJson.getInt("x"), boxJson.getInt("y"), boxJson.getString("title"),
								boxJson.optString("content", ""), id);
						newBox.setConnectedBoxIds(connectedIds);
						boxList.addNode(newBox);
					}
				}
			}
		}

		if (initialLoad) {
			rebuildLinesFromBoxConnections(loggerUserEmail + "/after_load_json_initial");
			repaint();
		}
	}

	public void saveCurrentState() {
		String userEmailForLog = (user != null ? user.getUserEmail() : "USER_NULL");
		if (this.boxList == null)
			return;
		SharedState.saveSharedBoard(this.boxList);
	}

	public String getBoardId() {
		return this.boardId;
	}

	private void sendDeleteBoxRequest(int boxId, String userEmailForLog) {
		if (serverConnection != null && boardId != null) {
			try {
				JSONObject deleteMessage = new JSONObject();
				deleteMessage.put("type", "delete_box");
				deleteMessage.put("boardId", boardId);
				deleteMessage.put("userEmail", userEmailForLog);
				deleteMessage.put("boxId", boxId);
				serverConnection.sendMessage(deleteMessage);
				System.out.println("BoardPanel (" + userEmailForLog + ") SEND_DELETE_BOX: Box ID " + boxId);
			} catch (Exception e) {
				System.err.println("Error sending delete box request: " + e.getMessage());
			}
		}
	}

	private void sendDeleteAllConnectionsRequest(int boxId, String userEmailForLog) {
		Box box = boxList.getBoxById(boxId);
		if (box != null && serverConnection != null && boardId != null) {
			try {
				List<Integer> connections = new ArrayList<>(box.getConnectedBoxIds());
				for (Integer targetId : connections) {
					JSONObject deleteConnMessage = new JSONObject();
					deleteConnMessage.put("type", "delete_connection");
					deleteConnMessage.put("boardId", boardId);
					deleteConnMessage.put("userEmail", userEmailForLog);
					deleteConnMessage.put("sourceBoxId", boxId);
					deleteConnMessage.put("targetBoxId", targetId);
					serverConnection.sendMessage(deleteConnMessage);
					System.out.println("BoardPanel (" + userEmailForLog + ") SEND_DELETE_CONNECTION: " + boxId + " -> "
							+ targetId);
				}
			} catch (Exception e) {
				System.err.println("Error sending delete connections request: " + e.getMessage());
			}
		}
	}
}