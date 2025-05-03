package main.java.board;
///////////////////////////////////////////////////

//Hold the info particular to each graphical Line//
///////////////////////////////////////////////////

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Graphics2D;

public class Line {

	private int id;
	private int sourceID;
	private int targetID;
	private Box sourceBox;
	private Box targetBox;
	private Point sourcePoint;
	private Point targetPoint;
	private Color color;
	private boolean selected;
	private Line nextLine;
	private float lineWidth = 1.5f;

	// Constructor for a line between two boxes
	public Line(Box source, Box target) {
		this.sourceBox = source;
		this.targetBox = target;
		this.sourceID = source.getId();
		this.targetID = target.getId();
		this.color = Color.BLACK;
		this.selected = false;
		this.nextLine = null;

		// Calculate connection points
		Point targetCenter = new Point(
				target.getBoxX() + target.getBoxWidth() / 2,
				target.getBoxY() + target.getBoxHeight() / 2);

		this.sourcePoint = source.getConnectionPoint(targetCenter);

		Point sourceCenter = new Point(
				source.getBoxX() + source.getBoxWidth() / 2,
				source.getBoxY() + source.getBoxHeight() / 2);

		this.targetPoint = target.getConnectionPoint(sourceCenter);
	}

	// Draw this line
	public void draw(Graphics g) {
		if (sourcePoint == null || targetPoint == null)
			return;

		// Convert to Graphics2D for better rendering
		Graphics2D g2d = (Graphics2D) g;

		// Draw line
		g2d.setColor(color);
		float lineWidth = selected ? 2.0f : 1.0f;
		g2d.setStroke(new BasicStroke(lineWidth));
		g2d.drawLine(sourcePoint.x, sourcePoint.y, targetPoint.x, targetPoint.y);

		// Draw arrowhead
		int dx = targetPoint.x - sourcePoint.x;
		int dy = targetPoint.y - sourcePoint.y;
		double angle = Math.atan2(dy, dx);

		int arrowSize = 10;
		int x1 = (int) (targetPoint.x - arrowSize * Math.cos(angle - Math.PI / 6));
		int y1 = (int) (targetPoint.y - arrowSize * Math.sin(angle - Math.PI / 6));
		int x2 = (int) (targetPoint.x - arrowSize * Math.cos(angle + Math.PI / 6));
		int y2 = (int) (targetPoint.y - arrowSize * Math.sin(angle + Math.PI / 6));

		Polygon arrowHead = new Polygon();
		arrowHead.addPoint(targetPoint.x, targetPoint.y);
		arrowHead.addPoint(x1, y1);
		arrowHead.addPoint(x2, y2);

		g2d.fillPolygon(arrowHead);

		// Draw selection indicator if selected
		if (selected) {
			g2d.setColor(Color.RED);
			g2d.drawLine(sourcePoint.x, sourcePoint.y, targetPoint.x, targetPoint.y);
		}
	}

	// Check if this line connects these boxes
	public boolean connects(Box source, Box target) {
		return (sourceBox == source && targetBox == target);
	}

	// Getters and setters
	public int getId() {
		return id;
	}

	public Box getSourceBox() {
		return sourceBox;
	}

	public void setSourceBox(Box sourceBox) {
		this.sourceBox = sourceBox;
	}

	public Box getTargetBox() {
		return targetBox;
	}

	public void setTargetBox(Box targetBox) {
		this.targetBox = targetBox;
	}

	public int getSourceID() {
		return sourceID;
	}

	public int getTargetID() {
		return targetID;
	}

	public void setColor(Color color) {
		this.color = color;
	}

	public Color getColor() {
		return color;
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	public Line getNext() {
		return nextLine;
	}

	public void setNext(Line nextLine) {
		this.nextLine = nextLine;
	}

	public float getLineWidth() {
		return lineWidth;
	}

	public void setLineWidth(float lineWidth) {
		this.lineWidth = lineWidth;
	}

	public void updatePoints() {
		if (sourceBox == null || targetBox == null) {
			return;
		}

		Point targetCenter = new Point(
				targetBox.getBoxX() + targetBox.getBoxWidth() / 2,
				targetBox.getBoxY() + targetBox.getBoxHeight() / 2);

		sourcePoint = sourceBox.getConnectionPoint(targetCenter);

		Point sourceCenter = new Point(
				sourceBox.getBoxX() + sourceBox.getBoxWidth() / 2,
				sourceBox.getBoxY() + sourceBox.getBoxHeight() / 2);

		targetPoint = targetBox.getConnectionPoint(sourceCenter);
	}
}
