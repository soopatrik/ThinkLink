package main.java.notes;

/**
 * Data model for a personal note in ThinkLink
 */
public class Note {
    private String title;
    private String content;
    private String owner;

    public Note(String title, String content, String owner) {
        this.title = title;
        this.content = content;
        this.owner = owner;
    }

    // Getters and setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getOwner() {
        return owner;
    }

    @Override
    public String toString() {
        return title;
    }
}
