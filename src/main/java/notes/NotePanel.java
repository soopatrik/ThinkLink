package main.notes;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.event.ListSelectionListener;

import main.java.utils.SaveAndLoad;

/**
 * Panel for managing personal notes in ThinkLink
 */
public class NotePanel extends JPanel {
    private String username;
    private boolean isAdmin;

    // UI Components
    private JList<String> noteList;
    private DefaultListModel<String> listModel;
    private JTextField titleField;
    private JTextArea contentArea;
    private JButton newButton, saveButton, deleteButton;

    // Data
    private Map<String, Note> notes = new HashMap<>();
    private String currentNoteTitle = null;
    private SaveAndLoad saveAndLoad;

    /**
     * Creates a new NotePanel for the specified user
     * 
     * @param username The username of the current user
     * @param isAdmin  Whether the user is an administrator
     */
    public NotePanel(String username, boolean isAdmin) {
        this.username = username;
        this.isAdmin = isAdmin;
        this.saveAndLoad = new SaveAndLoad();

        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create the components
        createComponents();

        // Load existing notes
        loadNotes();
    }

    /**
     * Creates and arranges the UI components
     */
    private void createComponents() {
        // Create the list model and list component
        listModel = new DefaultListModel<>();
        noteList = new JList<>(listModel);
        noteList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        noteList.setCellRenderer(new NoteCellRenderer());
        noteList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectNote(noteList.getSelectedValue());
            }
        });

        // Create note editor components
        titleField = new JTextField();
        titleField.setFont(new Font("Arial", Font.BOLD, 14));

        contentArea = new JTextArea();
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);

        // Create toolbar with action buttons
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);

        newButton = new JButton("New Note");
        newButton.setIcon(UIManager.getIcon("FileView.fileIcon"));
        newButton.addActionListener(e -> createNewNote());

        saveButton = new JButton("Save");
        saveButton.setIcon(UIManager.getIcon("FileView.floppyDriveIcon"));
        saveButton.addActionListener(e -> saveCurrentNote());

        deleteButton = new JButton("Delete");
        deleteButton.setIcon(UIManager.getIcon("FileView.deleteIcon"));
        deleteButton.addActionListener(e -> deleteCurrentNote());

        toolbar.add(newButton);
        toolbar.add(new JSeparator(SwingConstants.VERTICAL));
        toolbar.add(saveButton);
        toolbar.add(deleteButton);

        // Add description label
        JLabel descLabel = new JLabel("Personal Notes - These notes are private to you");
        descLabel.setFont(new Font("Arial", Font.ITALIC, 12));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(toolbar, BorderLayout.NORTH);
        topPanel.add(descLabel, BorderLayout.SOUTH);

        // Create the note editor panel
        JPanel editorPanel = new JPanel(new BorderLayout(5, 5));
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.add(new JLabel("Title:"), BorderLayout.WEST);
        titlePanel.add(titleField, BorderLayout.CENTER);

        editorPanel.add(titlePanel, BorderLayout.NORTH);
        editorPanel.add(new JScrollPane(contentArea), BorderLayout.CENTER);

        // Create the split pane
        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(noteList),
                editorPanel);
        splitPane.setDividerLocation(200);

        // Add components to the panel
        add(topPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
    }

    /**
     * Custom cell renderer for the note list
     */
    private class NoteCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {

            JLabel label = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);

            // Add icon to indicate it's a note
            label.setIcon(UIManager.getIcon("FileView.fileIcon"));

            return label;
        }
    }

    /**
     * Loads all notes for the current user
     */
    private void loadNotes() {
        listModel.clear();
        notes.clear();

        // Get all note titles for this user
        List<String> noteTitles = saveAndLoad.getNoteList(username);

        // Load each note
        for (String title : noteTitles) {
            Map<String, String> noteData = saveAndLoad.loadNote(title, username);
            if (noteData != null) {
                String noteTitle = noteData.get("title");
                String content = noteData.get("content");

                Note note = new Note(noteTitle, content, username);
                notes.put(noteTitle, note);
                listModel.addElement(noteTitle);
            }
        }

        // Select the first note if available
        if (!listModel.isEmpty()) {
            noteList.setSelectedIndex(0);
        } else {
            // Create a sample note if none exist
            createSampleNote();
        }
    }

    /**
     * Creates a sample note for new users
     */
    private void createSampleNote() {
        String title = "Welcome to Notes";
        String content = "Welcome to ThinkLink's Note-Taking Space!\n\n" +
                "Use this space to jot down your personal thoughts, ideas, and reminders. " +
                "These notes are private to you and won't be shared with other users.\n\n" +
                "To get started:\n" +
                "1. Click 'New Note' to create a new note\n" +
                "2. Type a title and content\n" +
                "3. Click 'Save' to save your note\n\n" +
                "You can have multiple notes and switch between them using the list on the left.";

        Note note = new Note(title, content, username);
        notes.put(title, note);
        listModel.addElement(title);
        noteList.setSelectedValue(title, true);

        // Save the sample note
        saveAndLoad.saveNote(title, content, username);
    }

    /**
     * Creates a new blank note
     */
    private void createNewNote() {
        // Store all listeners
        ListSelectionListener[] listeners = noteList.getListSelectionListeners();

        // Remove all listeners to prevent side effects
        for (ListSelectionListener listener : listeners) {
            noteList.removeListSelectionListener(listener);
        }

        // First check if we need to save current changes
        if (hasUnsavedChanges()) {
            int response = JOptionPane.showConfirmDialog(this,
                    "Save changes to current note?",
                    "Unsaved Changes",
                    JOptionPane.YES_NO_CANCEL_OPTION);

            if (response == JOptionPane.YES_OPTION) {
                saveCurrentNote();
            } else if (response == JOptionPane.CANCEL_OPTION) {
                // Restore listeners and exit
                for (ListSelectionListener listener : listeners) {
                    noteList.addListSelectionListener(listener);
                }
                return;
            }
        }

        // Create unique title
        String title = "New Note";
        int counter = 1;
        while (notes.containsKey(title)) {
            title = "New Note " + counter++;
        }

        // Create the new note and update data model
        Note note = new Note(title, "", username);
        notes.put(title, note);

        // Also save to disk immediately to avoid duplication
        saveAndLoad.saveNote(title, "", username);

        // Update UI
        if (!listModel.contains(title)) {
            listModel.addElement(title);
        }

        // Update fields and current note tracking
        titleField.setText(title);
        contentArea.setText("");
        currentNoteTitle = title;

        // Select it in the list
        noteList.setSelectedValue(title, true);

        // Restore listeners
        for (ListSelectionListener listener : listeners) {
            noteList.addListSelectionListener(listener);
        }

        titleField.requestFocus();
    }

    /**
     * Saves the currently edited note
     */
    private void saveCurrentNote() {
        String title = titleField.getText().trim();
        String content = contentArea.getText();

        // Validate title
        if (title.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Note title cannot be empty.",
                    "Validation Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Get all selection listeners
        ListSelectionListener[] listeners = noteList.getListSelectionListeners();

        // Remove all listeners temporarily
        for (ListSelectionListener listener : listeners) {
            noteList.removeListSelectionListener(listener);
        }

        // Check if title has changed
        if (currentNoteTitle != null && !title.equals(currentNoteTitle)) {
            // Check if new title already exists
            if (notes.containsKey(title)) {
                int confirm = JOptionPane.showConfirmDialog(this,
                        "A note with this title already exists. Do you want to replace it?",
                        "Confirm Replace",
                        JOptionPane.YES_NO_OPTION);

                if (confirm != JOptionPane.YES_OPTION) {
                    // Restore listeners and exit
                    for (ListSelectionListener listener : listeners) {
                        noteList.addListSelectionListener(listener);
                    }
                    return;
                }

                // Remove the existing note with same title
                notes.remove(title);
                int indexToRemove = listModel.indexOf(title);
                if (indexToRemove >= 0) {
                    listModel.remove(indexToRemove);
                }
            }

            // Remove old note
            notes.remove(currentNoteTitle);

            // Update list model
            int index = listModel.indexOf(currentNoteTitle);
            if (index >= 0) {
                listModel.remove(index);
                listModel.add(index, title);
            }
        }

        // Create or update the note
        Note note = new Note(title, content, username);
        notes.put(title, note);

        // If this is a new note that's not in the list, add it
        if (!listModel.contains(title)) {
            listModel.addElement(title);
        }

        // Save to file
        boolean success = saveAndLoad.saveNote(title, content, username);

        if (success) {
            currentNoteTitle = title;
            JOptionPane.showMessageDialog(this,
                    "Note saved successfully!",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
        }

        // Restore listeners
        for (ListSelectionListener listener : listeners) {
            noteList.addListSelectionListener(listener);
        }
    }

    /**
     * Deletes the currently selected note
     */
    private void deleteCurrentNote() {
        String title = noteList.getSelectedValue();

        if (title != null) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to delete note \"" + title + "\"?",
                    "Confirm Deletion",
                    JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                // Temporarily remove selection listener
                ListSelectionListener[] listeners = noteList.getListSelectionListeners();
                for (ListSelectionListener listener : listeners) {
                    noteList.removeListSelectionListener(listener);
                }

                // Remove from model
                notes.remove(title);
                int selectedIndex = listModel.indexOf(title);
                listModel.removeElement(title);

                // Delete file
                saveAndLoad.deleteNote(title, username);

                // Clear editor if this was the current note
                if (title.equals(currentNoteTitle)) {
                    titleField.setText("");
                    contentArea.setText("");
                    currentNoteTitle = null;
                }

                // Select another note if available
                if (!listModel.isEmpty()) {
                    int newIndex = Math.min(selectedIndex, listModel.size() - 1);
                    noteList.setSelectedIndex(newIndex);

                    // Manually set the content
                    String newTitle = listModel.get(newIndex);
                    Note newNote = notes.get(newTitle);
                    if (newNote != null) {
                        titleField.setText(newNote.getTitle());
                        contentArea.setText(newNote.getContent());
                        currentNoteTitle = newTitle;
                    }
                }

                // Re-add listeners
                for (ListSelectionListener listener : listeners) {
                    noteList.addListSelectionListener(listener);
                }
            }
        }
    }

    /**
     * Selects a note from the list
     */
    private void selectNote(String title) {
        if (title == null) {
            titleField.setText("");
            contentArea.setText("");
            currentNoteTitle = null;
            return;
        }

        // If this is the same note that's already selected, don't do anything
        if (title.equals(currentNoteTitle)) {
            return;
        }

        Note note = notes.get(title);
        if (note != null) {
            // Update editor
            titleField.setText(note.getTitle());
            contentArea.setText(note.getContent());
            currentNoteTitle = title;
        }
    }

    /**
     * Checks if there are unsaved changes to the current note
     */
    private boolean hasUnsavedChanges() {
        if (currentNoteTitle == null) {
            return false;
        }

        Note note = notes.get(currentNoteTitle);
        if (note == null) {
            return false;
        }

        return !titleField.getText().equals(note.getTitle()) ||
                !contentArea.getText().equals(note.getContent());
    }

    /**
     * Updates the username when user changes
     */
    public void setUsername(String username) {
        this.username = username;
        loadNotes();
    }
}
