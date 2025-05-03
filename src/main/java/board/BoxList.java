package main.java.board;

import java.awt.Graphics;
import java.awt.Point;

//////////////////////////////////////////
//Connects all the Box Objects in memory//
//////////////////////////////////////////
public class BoxList {
	protected Box top;
	private int size;
	private int nodeCount;

	public BoxList() {
		top = null;
		size = 0;
		nodeCount = 0;
	}

	public void draw(Graphics g) {
		Box current = top;
		while (current != null) {
			current.draw(g);
			current = current.getNext();
		}
	}

	public Box findNodeAt(Point point) {
		Box current = top;
		while (current != null) {
			if (current.contains(point)) {
				return current;
			}
			current = current.getNext();
		}
		return null;
	}

	public void moveNodeToTheFront(Box lpr) {
		// If list is empty or node is already at the front
		if (top == null || lpr == top) {
			return;
		}

		// Find the node and its predecessor
		Box current = top, next = lpr.getNext();
		Box prev = null;

		while (current != null && current != lpr) {
			prev = current;
			current = current.getNext();
		}

		// If node not found
		if (current == null) {
			return;
		}

		// Remove it from current position
		prev.setNext(next);

		// Insert at front
		lpr.setNext(top);
		top = lpr;
	}

	public void unselectAll() {
		Box current = top;
		while (current != null) {
			current.setSelected(false);
			current = current.getNext();
		}
	}

	public void addNode(Box lpr) {
		size++;

		if (top == null) {
			top = lpr;
			lpr.setNext(null);
			lpr.setId(nodeCount++);
		} else {
			Box current = top;
			while (current.getNext() != null) {
				current = current.getNext();
			}
			current.setNext(lpr);
			lpr.setNext(null);
			lpr.setId(nodeCount++);
		}
	}

	public void deleteSelectedNode() {
		if (top == null) {
			return;
		}

		Box current;

		// If head is selected
		if (top.isSelected()) {
			current = top;
			top = top.getNext();
			size--;
			return;
		}

		// Find selected node
		current = top;
		while (current.getNext() != null && !current.getNext().isSelected()) {
			current = current.getNext();
		}

		// If found, delete it
		if (current.getNext() != null) {
			current.setNext(current.getNext().getNext());
			size--;
		}
	}

	public Box getNode(String name) {
		Box current = top;
		while (current != null) {
			if (current.getText().equals(name)) {
				return current;
			}
			current = current.getNext();
		}
		return null;
	}

	public Box getNode(int ID) {
		Box current = top;
		while (current != null) {
			if (current.getId() == ID) {
				return current;
			}
			current = current.getNext();
		}
		return null;
	}

	public void deleteNode(Box lpr) {
		if (top == null) {
			return;
		}

		Box current, previous;

		// If head node is the one to delete
		if (top == lpr) {
			top = top.getNext();
			size--;
			return;
		}

		// Find node in list
		previous = top;
		current = top.getNext();

		while (current != null && current != lpr) {
			previous = current;
			current = current.getNext();
		}

		// If found, delete it
		if (current != null) {
			previous.setNext(current.getNext());
			size--;
		}
	}

	public int getSize() {
		return size;
	}

	public Box getFirstNode() {
		return top;
	}

	public int getNodeCount() {
		return nodeCount;
	}

	public void clearList() {
		top = null;
		size = 0;
		nodeCount = 0;
	}

	public int getLength() {
		return size;
	}
}
