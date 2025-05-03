package main.java.board;

import java.util.Date;

/**
 * 
 * Represents a task on a shared board.
 */
public class LiveTask {
    private String title;
    private String notes;
    private String status;
    private Date dateCreated;

    public static final String STATUS_TODO = "To Do";
    public static final String STATUS_IN_PROGRESS = "In Progress";
    public static final String STATUS_COMPLETED = "Completed";

    public LiveTask(String title) {
        this.title = title;
        this.notes = "";
        this.status = STATUS_TODO;
        this.dateCreated = new Date(); // Current date/time
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        // Validate status
        if (status.equals(STATUS_TODO) ||
                status.equals(STATUS_IN_PROGRESS) ||
                status.equals(STATUS_COMPLETED)) {
            this.status = status;
        }
    }

    public Date getDateCreated() {
        return dateCreated;
    }
}
