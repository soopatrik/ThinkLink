///////////////////////////////////////////////////////
//Box: Hold the info particular to each graphical Box//
///////////////////////////////////////////////////////
package main.java.board;

import java.awt.Dimension;
import java.awt.Point;
import java.util.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.FontMetrics;

public class Box {
	//////////////////////////////////////////////
	///////// Identity of each box & Links////////
	//////////////////////////////////////////////

	// Memory address of Box it is connected to (in memory)
	private Box NextBox;
	// Holds the objects it connects to (graphically)
	private List<Integer> linkTo;
	private int ID, isConnectedBy;
	private boolean ifThisBoxIsTheMain0;
	private String Title, Body;
	private int id = -1; // Initialize to a sentinel value, will be set by server
	private boolean selected = false;
	private String text;
	private String content = "";
	//////////////////////////////////////////////

	//////////////////////////////////////////////
	////////////// Graphics Info//////////////////
	//////////////////////////////////////////////
	public int boxX, boxY, boxHeight, boxWidth;
	public boolean mouseDraggedBox;
	public Color boxColor = new Color(240, 240, 240);

	//////////////////////////////////////////////
	/////////// Identity Algorithms Below:////////
	//////////////////////////////////////////////
	public Box() {
		this.linkTo = new ArrayList<>();
		this.Title = "New Task";
		this.content = "";
		this.boxHeight = 100;
		this.boxWidth = 150;
		// Initialize other legacy fields if necessary, or mark as deprecated
		this.NextBox = null;
		this.ifThisBoxIsTheMain0 = false; // Assuming not main by default
		this.ID = -1; // Legacy list ID
		this.isConnectedBy = -1; // Legacy connection
		this.Body = "";
		this.mouseDraggedBox = false;
		recalculateSize();
	}

	// Constructor for server-side creation with all necessary fields
	public Box(int x, int y, String title, String content, int uniqueId) {
		this.boxX = x;
		this.boxY = y;
		this.Title = title;
		this.content = content;
		this.id = uniqueId; // This is the crucial server-generated ID

		this.linkTo = new ArrayList<>();
		this.boxHeight = 100; // Default or calculate based on content
		this.boxWidth = 150; // Default or calculate based on title/content
		this.boxColor = new Color(240, 240, 240);
		this.selected = false;
		this.NextBox = null;
		this.ifThisBoxIsTheMain0 = false;
		this.ID = -1; // Legacy, set if BoxList internally uses it
		this.isConnectedBy = -1;
		this.Body = ""; // If content is primary, Body might be redundant
		this.mouseDraggedBox = false;
		recalculateSize();
	}

	public Box(String m) {
		this(); // Call the default constructor to initialize common fields
		Title = m;
		// this.id = -1; // Or some other default if not provided by server immediately
		recalculateSize();
	}

	public void setIfThisBoxIsTheMain0(boolean decision) {
		ifThisBoxIsTheMain0 = decision;
	}

	public void setIsConnectedBy(int id) {
		isConnectedBy = id;
	}

	// Adds
	public void addLink(int iD, BoxList checkList) {
		// This method has complex legacy logic. For server-driven connections,
		// prefer using direct modification of 'linkTo' via addConnection(boxId) or
		// setConnectedBoxIds.
		// If this is still used for local-only UI interactions, it needs careful
		// review.
		// For now, let's assume server messages will handle connection state.
		// Simplified for now:
		if (iD != this.id && !this.linkTo.contains(iD)) {
			this.linkTo.add(iD);
			if (checkList != null) {
				Box targetBox = checkList.getBoxById(iD); // Use corrected method name
				if (targetBox != null) {
					// The concept of 'isConnectedBy' (single parent) is tricky with
					// multi-connections
					// targetBox.setIsConnectedBy(this.id); // This would overwrite if target has
					// other incoming
				}
			}
		}
	}

	public void EmptyTheListOfConnection() {
		if (linkTo != null)
			linkTo.clear();
	}

	// ADD LINK (INPUT/PARAMETER IS THE ID OF THE
	// OBJECT THAT CONNECTS TO THIS
	// OBJECT)
	public void addLinkCopyOnly(int i) {
		if (i >= 0 && i != this.id && (linkTo == null || !linkTo.contains(i))) {
			if (linkTo == null)
				linkTo = new ArrayList<>();
			linkTo.add(i);
		}
	}

	// Delete a graphical connection
	public void deleteLink(int i, BoxList list) {
		if (linkTo != null && linkTo.contains(Integer.valueOf(i))) {
			linkTo.remove(Integer.valueOf(i));
			if (list != null) {
				Box targetBox = list.getBoxById(i);
				if (targetBox != null && targetBox.getIsConnectedBy() == this.id) {
					targetBox.setIsConnectedBy(-1); // Clear if this was the 'main' incoming connection
				}
			}
		}
	}

	// Returns an array of the Box objects it is connecting to
	public int[] getList() {
		if (linkTo == null)
			return new int[0];
		return linkTo.stream().mapToInt(Integer::intValue).toArray();
	}

	// Returns the ID of the object to which it is connected
	// If it is not connected to anything, returns -1
	public int getIsConnectedBy() {
		return isConnectedBy;
	}

	// toString for general info. For debugging only
	public String toString() {
		String sentence = "";
		sentence += "Box ID: " + id + " (LegacyID: " + ID + "), Title: '" + Title + "', Content: '" + content
				+ "', Connections: ";
		if (linkTo != null) {
			for (Integer linkedId : linkTo) {
				sentence += linkedId + ", ";
			}
		} else {
			sentence += "None";
		}
		return sentence;
	}

	///////////////////////////////////////////
	///////// Getter and Setter Methods////////
	////// for Memory (not graphical) info/////
	///////////////////////////////////////////

	// Get the next object in the BoxList
	public Box getNext() {
		return NextBox;
	}

	// Sets the next object in the BoxList
	// (The object it connects to in memory)
	public void setNext(Box obj) {
		NextBox = obj;
	}

	// ID of a box is its position on the BoxList
	public int getID() {
		return ID;
	}

	public void setID(int i) {
		ID = i;
	}

	public String getTitle() {
		return Title;
	}

	public void setTitle(String newTitle) {
		this.Title = (newTitle == null) ? "" : newTitle;
		recalculateSize();
	}

	public String getBody() {
		return Body;
	}

	public void setBody(String Body) {
		this.Body = Body;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getText() {
		return text;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String newContent) {
		this.content = (newContent == null) ? "" : newContent;
		recalculateSize();
	}

	///////////////////////////////////////////
	///////////////////////////////////////////

	///////////////////////////////////////////
	///////// Getter and Setter Methods////////
	//////////// for Graphical info////////////
	///////////////////////////////////////////

	// Setter Methods
	// sets x-coordinate of box
	public void setBoxX(int boxX) {
		this.boxX = boxX;
	}

	// sets y-coordinate of box
	public void setBoxY(int boxY) {
		this.boxY = boxY;
	}

	//// sets x & y coordinates of box
	public void setBoxPosition(int boxX, int boxY) {
		this.boxX = boxX;
		this.boxY = boxY;
	}// SET BOX COORDINATE

	public void setBoxHeight(int height) {
		this.boxHeight = height;
	}

	public void setBoxWidth(int width) {
		this.boxWidth = width;
	}

	// Sets both height and width of box
	public void setBoxSize(int height, int width) {
		this.boxHeight = height;
		this.boxWidth = width;
	}

	// Sets coordinates and size of box
	public void setBox(int boxX, int boxY, int height, int width) {
		this.boxX = boxX;
		this.boxY = boxY;
		this.boxHeight = height;
		this.boxWidth = width;
	}

	// Sets condition of the relationship between the mouse and the box
	// Used to determine whether the mouse is hovering over box or not
	public void setMouseDragBoxCondition(boolean TF) {
		mouseDraggedBox = TF;
	}

	// Getter Methods
	public int getBoxX() {
		return boxX;
	}

	public int getBoxY() {
		return boxY;
	}

	public int getBoxWidth() {
		return boxWidth;
	}

	public int getBoxHeight() {
		return boxHeight;
	}

	public Point boxPoint() {
		return new Point(boxX, boxY);
	}

	public Dimension boxDimension() {
		return new Dimension(boxWidth, boxHeight);
	}

	public boolean getMouseDragBoxCondition() {
		return mouseDraggedBox;
	}

	public Color getBoxColor() {
		return boxColor;
	}

	public void setBoxColor(Color boxColor) {
		this.boxColor = boxColor;
	}

	public Point getCenter() {
		return new Point(boxX + boxWidth / 2, boxY + boxHeight / 2);
	}

	public boolean containsPoint(int x, int y) {
		return x >= boxX && x <= boxX + boxWidth &&
				y >= boxY && y <= boxY + boxHeight;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	public boolean isSelected() {
		return selected;
	}

	public List<Integer> getConnectedBoxIds() {
		if (this.linkTo == null) {
			this.linkTo = new ArrayList<>(); // Defensive initialization
		}
		return this.linkTo;
	}

	// Method to completely replace the set of connected box IDs
	public void setConnectedBoxIds(List<Integer> newConnectionIds) {
		if (newConnectionIds == null) {
			this.linkTo = new ArrayList<>();
		} else {
			this.linkTo = new ArrayList<>(newConnectionIds); // Create a new list to avoid external modification issues
		}
	}

	// Adds a single connection if it doesn't already exist
	public boolean addConnection(int boxId) {
		if (this.linkTo == null) {
			this.linkTo = new ArrayList<>();
		}
		if (!this.linkTo.contains(boxId) && boxId != this.id) { // Prevent self-connection
			this.linkTo.add(boxId);
			return true;
		}
		return false;
	}

	public void removeConnection(int boxId) {
		if (this.linkTo != null) {
			this.linkTo.remove(Integer.valueOf(boxId));
		}
	}

	////////////////////////////////////////////////
	////////////////////////////////////////////////

	public void draw(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		g2d.setColor(boxColor);
		g2d.fillRect(boxX, boxY, boxWidth, boxHeight);

		if (selected) {
			g2d.setColor(Color.RED);
			g2d.setStroke(new BasicStroke(2));
			g2d.drawRect(boxX - 1, boxY - 1, boxWidth + 2, boxHeight + 2);
		}
		g2d.setColor(Color.BLACK);
		g2d.setStroke(new BasicStroke(1));
		g2d.drawRect(boxX, boxY, boxWidth, boxHeight);

		Font titleFont = new Font("Arial", Font.BOLD, 12);
		g2d.setFont(titleFont);
		g2d.setColor(Color.BLACK);
		// Using a helper for centering text (from BoardPanel example, adapt if needed)
		FontMetrics titleMetrics = g2d.getFontMetrics(titleFont);
		int titleX = boxX + (boxWidth - titleMetrics.stringWidth(Title)) / 2;
		int titleY = boxY + titleMetrics.getAscent() + 5; // Small padding
		g2d.drawString(Title, titleX, titleY);

		Font contentFont = new Font("Arial", Font.PLAIN, 10);
		g2d.setFont(contentFont);
		// Using a helper for wrapped text (from BoardPanel example, adapt if needed)
		// For simplicity, draw content on one line for now or implement wrapping
		int contentY = titleY + titleMetrics.getHeight(); // Position below title
		g2d.drawString(content, boxX + 5, contentY);
	}

	public boolean contains(Point p) {
		return (p.x >= boxX && p.x < boxX + boxWidth && p.y >= boxY && p.y < boxY + boxHeight);
	}

	public Point getConnectionPoint(Point target) {
		Point center = getCenter();
		int dx = target.x - center.x;
		int dy = target.y - center.y;

		if (dx == 0 && dy == 0)
			return center; // Target is center, no specific edge

		double angle = Math.atan2(dy, dx);

		int halfWidth = boxWidth / 2;
		int halfHeight = boxHeight / 2;

		double tanAngle = Math.tan(angle);

		int edgeX, edgeY;

		if (Math.abs(dx) * halfHeight > Math.abs(dy) * halfWidth) { // Intersects vertical edges
			if (dx > 0) { // Right edge
				edgeX = halfWidth;
				edgeY = (int) (halfWidth * tanAngle);
			} else { // Left edge
				edgeX = -halfWidth;
				edgeY = (int) (-halfWidth * tanAngle);
			}
		} else { // Intersects horizontal edges
			if (dy > 0) { // Bottom edge
				edgeY = halfHeight;
				edgeX = (int) (halfHeight / tanAngle);
			} else { // Top edge
				edgeY = -halfHeight;
				edgeX = (int) (-halfHeight / tanAngle);
			}
		}
		return new Point(center.x + edgeX, center.y + edgeY);
	}

	public boolean isLinked(int iD) {
		if (linkTo == null)
			return false;
		return linkTo.contains(iD);
	}

	public void recalculateSize() {
		// Minimum dimensions
		boxWidth = Math.max(100, 20 + (this.Title != null ? this.Title.length() * 7 : 0));

		// Calculate height based on content
		if (this.content != null && !this.content.isEmpty()) {
			String[] lines = this.content.split("\n");
			int lineCount = lines.length;
			int maxLineWidth = 0;

			for (String line : lines) {
				maxLineWidth = Math.max(maxLineWidth, line.length());
			}

			// Adjust width based on longest line
			boxWidth = Math.max(boxWidth, 20 + maxLineWidth * 7);

			// Adjust height based on number of lines
			boxHeight = Math.max(50, 40 + lineCount * 15);
		} else {
			boxHeight = 50; // Default height
		}
	}

}
