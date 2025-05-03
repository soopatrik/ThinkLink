package main.java.board;

import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;

public class LineList {
	private List<Line> lines;

	public LineList() {
		lines = new ArrayList<>();
	}

	public void addLine(Box source, Box target) {
		// Check if line already exists
		for (Line line : lines) {
			if (line.connects(source, target)) {
				return;
			}
		}

		// Add connection to the boxes
		source.addLink(target.getId(), null);

		// Create new line
		Line line = new Line(source, target);
		lines.add(line);
	}

	public void draw(Graphics g) {
		for (Line line : lines) {
			line.updatePoints();
			line.draw(g);
		}
	}

	public Line findLine(Box source, Box target) {
		for (Line line : lines) {
			if (line.connects(source, target)) {
				return line;
			}
		}
		return null;
	}

	public void removeBoxLines(Box box) {
		List<Line> linesToRemove = new ArrayList<>();
		for (Line line : lines) {
			if (line.getSourceBox() == box || line.getTargetBox() == box) {
				linesToRemove.add(line);
			}
		}
		lines.removeAll(linesToRemove);
	}

	public void clearSelected() {
		for (Line line : lines) {
			line.setSelected(false);
		}
	}

	public List<Line> getLines() {
		return lines;
	}

	public void clear() {
		lines.clear();
	}
}