package main.java.calendar;

import main.java.auth.User;
import java.util.Date;

/**
 * 
 * Can only be managed by administrators.
 */
public class Deadline {
    private String description;
    private Date dueDate;
    private User assignedTo;

    public Deadline(String description, Date dueDate, User assignedTo) {
        this.description = description;
        this.dueDate = dueDate;
        this.assignedTo = assignedTo;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public User getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(User assignedTo) {
        this.assignedTo = assignedTo;
    }
}
