package main.java.utils;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import javax.swing.*;
import java.awt.event.*;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.RenderingHints;

////////////////////////////////////////////////
////Popping up panel for selecting the color////
////////////////////////////////////////////////

/**
 * A custom color selector component for the ThinkLink application.
 * This allows users to choose colors for boxes, lines, and other elements.
 */
public class ColorBox extends JPanel implements MouseListener {
	private int boxSize;
	private int strokeSize;
	private Color stroke;
	private Color fill;
	private Color selectedColor;
	private boolean clicked = false;
	private ColorChangeListener listener;

	/**
	 * Interface for handling color change events
	 */
	public interface ColorChangeListener {
		void colorChanged(Color newColor);
	}

	/**
	 * Creates a new ColorBox with the specified properties.
	 * 
	 * @param x          X position
	 * @param y          Y position
	 * @param strokeSize Width of the border
	 * @param stroke     Color of the border
	 * @param fill       Fill color of the box
	 */
	public ColorBox(int boxSize, int strokeSize, Color stroke, Color fill) {
		this.boxSize = boxSize;
		this.strokeSize = strokeSize;
		this.stroke = stroke;
		this.fill = fill;
		this.selectedColor = fill;

		setPreferredSize(new Dimension(boxSize, boxSize));
		setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		addMouseListener(this);
	}

	/**
	 * Sets a listener to be notified when the color changes
	 */
	public void setColorChangeListener(ColorChangeListener listener) {
		this.listener = listener;
	}

	/**
	 * Shows a color chooser dialog and updates the color
	 */
	private void showColorChooser() {
		Color newColor = JColorChooser.showDialog(
				this,
				"Select Color",
				selectedColor);

		if (newColor != null) {
			selectedColor = newColor;
			fill = newColor;
			repaint();

			// Notify listener if available
			if (listener != null) {
				listener.colorChanged(newColor);
			}
		}
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// Calculate centered position
		int x = (getWidth() - boxSize) / 2;
		int y = (getHeight() - boxSize) / 2;

		// Draw border
		g2d.setColor(stroke);
		g2d.fillRect(x, y, boxSize, boxSize);

		// Draw inner fill
		g2d.setColor(fill);
		g2d.fillRect(
				x + strokeSize,
				y + strokeSize,
				boxSize - (strokeSize * 2),
				boxSize - (strokeSize * 2));

		// Draw selection indicator if clicked
		if (clicked) {
			g2d.setColor(new Color(100, 100, 255, 100));
			g2d.setStroke(new BasicStroke(2));
			g2d.drawRect(x - 2, y - 2, boxSize + 4, boxSize + 4);
		}
	}

	public Color getSelectedColor() {
		return selectedColor;
	}

	public void setSelectedColor(Color color) {
		this.selectedColor = color;
		this.fill = color;
		repaint();
	}

	// Mouse event handlers
	@Override
	public void mouseClicked(MouseEvent e) {
		showColorChooser();
	}

	@Override
	public void mousePressed(MouseEvent e) {
		clicked = true;
		repaint();
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		clicked = false;
		repaint();
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
	}

	@Override
	public void mouseExited(MouseEvent e) {
		setCursor(Cursor.getDefaultCursor());
	}
}
