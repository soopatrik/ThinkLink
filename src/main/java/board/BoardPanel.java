// BoardPanel.java (converted from RenderPanel.java)
package main.java.board;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import main.java.auth.User;

public class BoardPanel extends JPanel implements MouseListener, MouseMotionListener {
	private User user;
	private BoxList boxList;
	private LineList lineList;
	private Box selectedBox;
	private Point dragStart;
	private boolean isDragging = false;
	private boolean connectingMode = false;
	private Box firstSelectedBox = null;

	public BoardPanel(User user) {
		this.user = user;
		boxList = new BoxList();
		lineList = new LineList();

		// Create a sample box to start
		Box initialBox = new Box("Welcome");
		initialBox.setBoxPosition(200, 200);
		boxList.addNode(initialBox);

		setBackground(new Color(240, 240, 240));
		addMouseListener(this);
		addMouseMotionListener(this);

		// Create popup menu
		createPopupMenu();
	}

	private void createPopupMenu() {
		JPopupMenu popup = new JPopupMenu();

		// Add New Box - available to all users
		JMenuItem addItem = new JMenuItem("Add New Task");
		addItem.addActionListener(e -> {
			Point p = getMousePosition();
			if (p != null) {
				Box newBox = new Box();
				newBox.setBoxPosition(p.x, p.y);
				newBox.setTitle("New Task");
				boxList.addNode(newBox);
				repaint();
			}
		});
		popup.add(addItem);

		// Connect Boxes - available to all users
		JMenuItem connectItem = new JMenuItem("Connect Tasks");
		connectItem.addActionListener(e -> {
			startConnectingBoxes();
		});
		popup.add(connectItem);

		// Delete Box - admin only
		if (user.isAdministrator()) {
			popup.addSeparator();
			JMenuItem deleteItem = new JMenuItem("Delete Task");
			deleteItem.addActionListener(e -> {
				Box boxToDelete = findSelectedBox();
				if (boxToDelete != null) {
					int confirm = JOptionPane.showConfirmDialog(
							this,
							"Are you sure you want to delete this task?",
							"Confirm Deletion",
							JOptionPane.YES_NO_OPTION);

					if (confirm == JOptionPane.YES_OPTION) {
						deleteBox(boxToDelete);
					}
				}
			});
			popup.add(deleteItem);
		}

		setComponentPopupMenu(popup);
	}

	private void startConnectingBoxes() {
		connectingMode = true;
		firstSelectedBox = null;
		setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
		JOptionPane.showMessageDialog(
				this,
				"Please click on the first task, then the second task to connect them.",
				"Connect Tasks",
				JOptionPane.INFORMATION_MESSAGE);
	}

	private void deleteBox(Box box) {
		if (box != null) {
			// Delete all lines connected to this box
			for (Box current = boxList.getFirstNode(); current != null; current = current.getNext()) {
				current.removeConnection(box.getId());
			}

			// Remove the box from the list
			boxList.deleteNode(box);
			repaint();
		}
	}

	private Box findSelectedBox() {
		for (Box current = boxList.getFirstNode(); current != null; current = current.getNext()) {
			if (current.isSelected()) {
				return current;
			}
		}
		return null;
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2d = (Graphics2D) g;

		// Enable anti-aliasing
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);

		// Draw connections between boxes
		drawConnections(g2d);

		// Draw all boxes
		drawBoxes(g2d);

		// Draw connection guide line in connecting mode
		if (connectingMode && firstSelectedBox != null) {
			Point p = getMousePosition();
			if (p != null) {
				g2d.setColor(Color.GRAY);
				g2d.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0,
						new float[] { 8 }, 0));

				Point start = firstSelectedBox.getCenter();
				g2d.drawLine(start.x, start.y, p.x, p.y);
			}
		}
	}

	private void drawConnections(Graphics2D g2d) {
		g2d.setColor(Color.DARK_GRAY);
		g2d.setStroke(new BasicStroke(1.5f));

		for (Box source = boxList.getFirstNode(); source != null; source = source.getNext()) {
			for (Integer targetId : source.getConnectedBoxIds()) {
				Box target = findBoxById(targetId);
				if (target != null) {
					Point sourceCenter = source.getCenter();
					Point targetCenter = target.getCenter();

					// Draw the line
					g2d.drawLine(sourceCenter.x, sourceCenter.y, targetCenter.x, targetCenter.y);

					// Draw arrow at the target end
					drawArrow(g2d, sourceCenter, targetCenter);
				}
			}
		}
	}

	private void drawArrow(Graphics2D g2d, Point from, Point to) {
		int arrowSize = 10;
		double dx = to.x - from.x;
		double dy = to.y - from.y;
		double angle = Math.atan2(dy, dx);

		// Calculate position near the edge of the target box
		int x1 = to.x - (int) (arrowSize * Math.cos(angle));
		int y1 = to.y - (int) (arrowSize * Math.sin(angle));

		// Draw the arrowhead
		int[] xPoints = { to.x,
				x1 - (int) (arrowSize * Math.cos(angle - Math.PI / 6)),
				x1 - (int) (arrowSize * Math.cos(angle + Math.PI / 6)) };
		int[] yPoints = { to.y,
				y1 - (int) (arrowSize * Math.sin(angle - Math.PI / 6)),
				y1 - (int) (arrowSize * Math.sin(angle + Math.PI / 6)) };

		g2d.fillPolygon(xPoints, yPoints, 3);
	}

	private void drawBoxes(Graphics2D g2d) {
		Box current = boxList.getFirstNode();
		while (current != null) {
			drawBox(g2d, current);
			current = current.getNext();
		}
	}

	private void drawBox(Graphics2D g2d, Box box) {
		int x = box.getBoxX();
		int y = box.getBoxY();
		int width = box.getBoxWidth();
		int height = box.getBoxHeight();

		// Draw box background
		g2d.setColor(box.getBoxColor());
		g2d.fillRoundRect(x, y, width, height, 10, 10);

		// Draw box border (highlighted if selected)
		if (box.isSelected()) {
			g2d.setColor(Color.BLUE);
			g2d.setStroke(new BasicStroke(2.0f));
		} else {
			g2d.setColor(Color.DARK_GRAY);
			g2d.setStroke(new BasicStroke(1.0f));
		}
		g2d.drawRoundRect(x, y, width, height, 10, 10);

		// Draw box title
		g2d.setColor(Color.BLACK);
		g2d.setFont(new Font("Arial", Font.BOLD, 14));
		drawCenteredString(g2d, box.getTitle(), new Rectangle(x, y, width, 30), g2d.getFont());

		// If box has content, draw it
		if (box.getBody() != null && !box.getBody().isEmpty()) {
			g2d.setFont(new Font("Arial", Font.PLAIN, 12));
			g2d.setColor(Color.DARK_GRAY);

			// Draw content with simple wrapping
			drawWrappedText(g2d, box.getBody(), x + 5, y + 35, width - 10);
		}
	}

	private void drawCenteredString(Graphics g, String text, Rectangle rect, Font font) {
		FontMetrics metrics = g.getFontMetrics(font);
		int x = rect.x + (rect.width - metrics.stringWidth(text)) / 2;
		int y = rect.y + ((rect.height - metrics.getHeight()) / 2) + metrics.getAscent();
		g.drawString(text, x, y);
	}

	private void drawWrappedText(Graphics2D g, String text, int x, int y, int width) {
		FontMetrics fm = g.getFontMetrics();
		int lineHeight = fm.getHeight();

		String[] words = text.split("\\s+");
		StringBuilder currentLine = new StringBuilder();
		int currentY = y;

		for (String word : words) {
			if (fm.stringWidth(currentLine + word) < width) {
				currentLine.append(word).append(" ");
			} else {
				g.drawString(currentLine.toString(), x, currentY);
				currentY += lineHeight;
				currentLine = new StringBuilder(word).append(" ");
			}
		}

		// Draw the last line
		g.drawString(currentLine.toString(), x, currentY);
	}

	private Box findBoxAt(int x, int y) {
		Box current = boxList.getFirstNode();
		while (current != null) {
			if (current.containsPoint(x, y)) {
				return current;
			}
			current = current.getNext();
		}
		return null;
	}

	private Box findBoxById(int id) {
		Box current = boxList.getFirstNode();
		while (current != null) {
			if (current.getId() == id) {
				return current;
			}
			current = current.getNext();
		}
		return null;
	}

	private void showBoxEditDialog(Box box) {
		JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
				"Edit Task", true);
		dialog.setLayout(new BorderLayout());

		JPanel panel = new JPanel(new GridLayout(3, 1, 5, 5));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		JTextField titleField = new JTextField(box.getTitle());

		JTextArea contentArea = new JTextArea(box.getBody());
		contentArea.setLineWrap(true);
		contentArea.setWrapStyleWord(true);
		contentArea.setRows(5);

		panel.add(new JLabel("Title:"));
		panel.add(titleField);
		panel.add(new JLabel("Content:"));
		panel.add(new JScrollPane(contentArea));

		JPanel buttonPanel = new JPanel();
		JButton saveButton = new JButton("Save");
		saveButton.addActionListener(e -> {
			String title = titleField.getText().trim();
			if (title.isEmpty()) {
				JOptionPane.showMessageDialog(dialog,
						"Task title cannot be empty.",
						"Validation Error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}

			box.setTitle(title);
			box.setBody(contentArea.getText());
			dialog.dispose();
			repaint();
		});

		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(e -> dialog.dispose());

		buttonPanel.add(saveButton);
		buttonPanel.add(cancelButton);

		dialog.add(panel, BorderLayout.CENTER);
		dialog.add(buttonPanel, BorderLayout.SOUTH);
		dialog.pack();
		dialog.setLocationRelativeTo(this);
		dialog.setVisible(true);
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		Box clickedBox = findBoxAt(e.getX(), e.getY());

		// Handle box connection mode
		if (connectingMode) {
			if (clickedBox != null) {
				if (firstSelectedBox == null) {
					firstSelectedBox = clickedBox;
					firstSelectedBox.setSelected(true);
					repaint();
				} else if (clickedBox != firstSelectedBox) {
					// Create connection between boxes
					firstSelectedBox.addConnection(clickedBox.getId());

					// Reset connection mode
					firstSelectedBox.setSelected(false);
					firstSelectedBox = null;
					connectingMode = false;
					setCursor(Cursor.getDefaultCursor());
					repaint();
				}
			}
		}
		// Handle double-click to edit box
		else if (e.getClickCount() == 2 && clickedBox != null) {
			showBoxEditDialog(clickedBox);
		}
		// Handle single click to select a box
		else if (clickedBox != null) {
			// Deselect any other selected box
			for (Box box = boxList.getFirstNode(); box != null; box = box.getNext()) {
				box.setSelected(false);
			}

			clickedBox.setSelected(true);
			repaint();
		} else {
			// Clicked on empty space - deselect all
			for (Box box = boxList.getFirstNode(); box != null; box = box.getNext()) {
				box.setSelected(false);
			}
			repaint();
		}
	}

	@Override
	public void mousePressed(MouseEvent e) {
		Box clicked = findBoxAt(e.getX(), e.getY());
		if (clicked != null && !connectingMode) {
			selectedBox = clicked;
			dragStart = e.getPoint();
			isDragging = true;
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		isDragging = false;
		selectedBox = null;
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if (isDragging && selectedBox != null) {
			int dx = e.getX() - dragStart.x;
			int dy = e.getY() - dragStart.y;

			selectedBox.setBoxPosition(
					selectedBox.getBoxX() + dx,
					selectedBox.getBoxY() + dy);

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
	}

	public BoxList getBoxList() {
		return boxList;
	}

	public LineList getLineList() {
		return lineList;
	}
}