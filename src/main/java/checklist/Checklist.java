package main.checklist;

import java.util.ArrayList;
import java.util.List;

public class Checklist {
    private String title;
    private List<ChecklistPanel.ChecklistItem> items;

    public Checklist(String title) {
        this.title = title;
        this.items = new ArrayList<>();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<ChecklistPanel.ChecklistItem> getItems() {
        return items;
    }

    public void addItem(ChecklistPanel.ChecklistItem item) {
        items.add(item);
    }

    public void removeItem(ChecklistPanel.ChecklistItem item) {
        items.remove(item);
    }

    public void removeItem(int index) {
        if (index >= 0 && index < items.size()) {
            items.remove(index);
        }
    }

    @Override
    public String toString() {
        return title;
    }
}
