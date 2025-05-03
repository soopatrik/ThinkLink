package main.java.board;

import main.java.auth.User;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 
 * Can only be created and managed by Administrator users.
 */
public class SharedBoard {
    private String name;
    private Date creationDate;
    private List<LiveTask> tasks;
    private User creator;

    public SharedBoard(String name, User creator) {
        this.name = name;
        this.creationDate = new Date(); // Current date/time
        this.tasks = new ArrayList<>();
        this.creator = creator;

        // Validate creator is an administrator
        if (!creator.isAdministrator()) {
            throw new IllegalArgumentException("Only administrators can create shared boards");
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void addTask(LiveTask task) {
        tasks.add(task);
    }

    public void removeTask(LiveTask task) {
        tasks.remove(task);
    }

    public List<LiveTask> getTasks() {
        return new ArrayList<>(tasks); // Return a copy to prevent direct modification
    }

    public User getCreator() {
        return creator;
    }
}
