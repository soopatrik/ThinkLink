package main.checklist;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ChecklistPanel extends JPanel {
    private List<Checklist> checklists = new ArrayList<>();
    private Checklist currentChecklist;
    private JComboBox<String> checklistSelector;
    private DefaultListModel<ChecklistItem> itemsModel;
    private JList<ChecklistItem> itemsList;
    private JTextField inputField;

    public ChecklistPanel() {
        setLayout(new BorderLayout());

        // Create the list model and list component
        itemsModel = new DefaultListModel<>();
        itemsList = new JList<>(itemsModel);
        itemsList.setCellRenderer(new ChecklistRenderer());

        // Create input field for new items
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        JButton addButton = new JButton("Add");

        inputPanel.add(new JLabel("New Goal: "), BorderLayout.WEST);
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(addButton, BorderLayout.EAST);

        // Action listeners
        addButton.addActionListener(e -> addGoal());
        inputField.addActionListener(e -> addGoal());

        itemsList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 1) {
                    int index = itemsList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        toggleGoalCompletion(index);
                    }
                }
            }
        });

        // Toolbar with actions
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);

        JButton clearCompletedButton = new JButton("Clear Completed");
        clearCompletedButton.addActionListener(e -> clearCompletedItems());

        JButton removeButton = new JButton("Remove Selected");
        removeButton.addActionListener(e -> removeSelectedItem());

        toolbar.add(clearCompletedButton);
        toolbar.add(removeButton);

        // Add components to panel
        add(toolbar, BorderLayout.NORTH);
        add(new JScrollPane(itemsList), BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);

        // Initialize a default checklist
        currentChecklist = new Checklist("My Goals");
        checklists.add(currentChecklist);

        // Add sample items
        addSampleItems();
    }

    private void addGoal() {
        String goalText = inputField.getText().trim();

        if (!goalText.isEmpty()) {
            // Create a new item
            ChecklistItem item = new ChecklistItem(goalText, false);

            // Add to model
            itemsModel.addElement(item);

            // Also add to the currentChecklist collection
            if (currentChecklist != null) {
                currentChecklist.addItem(item);
            }

            // Clear input field
            inputField.setText("");
        }
    }

    private void removeSelectedItem() {
        int selectedIndex = itemsList.getSelectedIndex();
        if (selectedIndex >= 0) {
            itemsModel.remove(selectedIndex);
        }
    }

    private void clearCompletedItems() {
        for (int i = itemsModel.getSize() - 1; i >= 0; i--) {
            if (itemsModel.getElementAt(i).isCompleted()) {
                itemsModel.remove(i);
            }
        }
    }

    private void addSampleItems() {
        // Create and add sample items
        ChecklistItem item1 = new ChecklistItem("Create project report", false);
        ChecklistItem item2 = new ChecklistItem("Design user interface", false);
        ChecklistItem item3 = new ChecklistItem("Implement core functionality", false);

        // Add to model
        itemsModel.addElement(item1);
        itemsModel.addElement(item2);
        itemsModel.addElement(item3);

        // Add to current checklist
        if (currentChecklist != null) {
            currentChecklist.addItem(item1);
            currentChecklist.addItem(item2);
            currentChecklist.addItem(item3);
        }
    }

    private void toggleGoalCompletion(int index) {
        if (index < 0 || index >= itemsModel.getSize())
            return;

        ChecklistItem item = itemsModel.getElementAt(index);
        item.setCompleted(!item.isCompleted());

        // This ensures the visual update happens
        itemsModel.set(index, item);

        // Also update in the currentChecklist if needed
        if (currentChecklist != null && index < currentChecklist.getItems().size()) {
            currentChecklist.getItems().get(index).setCompleted(item.isCompleted());
        }
    }

    // ChecklistItem class
    public static class ChecklistItem {
        private String text;
        private boolean completed;

        public ChecklistItem(String text, boolean completed) {
            this.text = text;
            this.completed = completed;
        }

        public String getText() {
            return text;
        }

        public boolean isCompleted() {
            return completed;
        }

        public void setCompleted(boolean completed) {
            this.completed = completed;
        }
    }

    // Custom renderer for checklist items
    private class ChecklistRenderer extends JCheckBox implements ListCellRenderer<ChecklistItem> {
        @Override
        public Component getListCellRendererComponent(
                JList<? extends ChecklistItem> list, ChecklistItem value,
                int index, boolean isSelected, boolean cellHasFocus) {

            setText(value.getText());
            setSelected(value.isCompleted());

            setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
            setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());

            return this;
        }
    }
}
