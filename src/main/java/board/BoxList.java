package main.java.board;

import java.awt.Graphics;
import java.awt.Point;
import org.json.JSONArray;
import org.json.JSONObject;

//////////////////////////////////////////
//Connects all the Box Objects in memory//
//////////////////////////////////////////
public class BoxList {
	protected Box top;
	private int size;
	// nodeCount was used for auto-incrementing legacy ID, server now handles unique
	// 'id'
	// private int nodeCount;

	public BoxList() {
		top = null;
		size = 0;
		// nodeCount = 0;
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
		if (top == null || lpr == top) {
			return;
		}
		Box current = top, next = lpr.getNext();
		Box prev = null;
		while (current != null && current != lpr) {
			prev = current;
			current = current.getNext();
		}
		if (current == null) {
			return;
		}
		if (prev != null) {
			prev.setNext(next);
		} else {
			// This should not be reached if lpr == top is handled.
		}
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
		if (lpr == null)
			return;

		if (top == null) {
			top = lpr;
			lpr.setNext(null);
		} else {
			Box current = top;
			while (current.getNext() != null) {
				current = current.getNext();
			}
			current.setNext(lpr);
			lpr.setNext(null);
		}
		size++;
	}

	public void deleteSelectedNode() {
		if (top == null) {
			return;
		}
		if (top.isSelected()) {
			top = top.getNext();
			size--;
			return;
		}
		Box current = top;
		while (current.getNext() != null && !current.getNext().isSelected()) {
			current = current.getNext();
		}
		if (current.getNext() != null) {
			current.setNext(current.getNext().getNext());
			size--;
		}
	}

	public void deleteNode(Box boxToDelete) {
		if (top == null || boxToDelete == null) {
			return;
		}
		if (top == boxToDelete) {
			top = top.getNext();
			size--;
			return;
		}
		Box current = top;
		while (current.getNext() != null && current.getNext() != boxToDelete) {
			current = current.getNext();
		}
		if (current.getNext() != null) {
			current.setNext(current.getNext().getNext());
			size--;
		}
	}

	public void deleteNodeById(int id) {
		if (top == null) {
			return;
		}
		if (top.getId() == id) {
			top = top.getNext();
			size--;
			return;
		}
		Box current = top;
		while (current.getNext() != null && current.getNext().getId() != id) {
			current = current.getNext();
		}
		if (current.getNext() != null) {
			current.setNext(current.getNext().getNext());
			size--;
		}
	}

	public Box getNode(String name) {
		Box current = top;
		while (current != null) {
			if (current.getTitle().equals(name)) {
				return current;
			}
			current = current.getNext();
		}
		return null;
	}

	public Box getBoxByLegacyID(int legacyId) {
		Box current = top;
		while (current != null) {
			if (current.getID() == legacyId) {
				return current;
			}
			current = current.getNext();
		}
		return null;
	}

	public Box getBoxById(int id) {
		Box current = top;
		while (current != null) {
			if (current.getId() == id) {
				return current;
			}
			current = current.getNext();
		}
		return null;
	}

	public int getSize() {
		return size;
	}

	public Box getFirstNode() {
		return top;
	}

	public void clearList() {
		top = null;
		size = 0;
	}

	public boolean containsBoxWithId(int id) {
		return getBoxById(id) != null;
	}

	public JSONArray toJSONArray() {
		JSONArray jsonArray = new JSONArray();
		Box current = top;
		while (current != null) {
			JSONObject boxJson = new JSONObject();
			boxJson.put("id", current.getId());
			boxJson.put("title", current.getTitle());
			boxJson.put("content", current.getContent());
			boxJson.put("x", current.getBoxX());
			boxJson.put("y", current.getBoxY());

			JSONArray connections = new JSONArray();
			if (current.getConnectedBoxIds() != null) {
				for (Integer connectedId : current.getConnectedBoxIds()) {
					connections.put(connectedId);
				}
			}
			boxJson.put("connections", connections);
			jsonArray.put(boxJson);
			current = current.getNext();
		}
		return jsonArray;
	}
}
