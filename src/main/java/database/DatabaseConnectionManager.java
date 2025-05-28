package main.java.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.io.IOException;
import java.io.InputStream;

/**
 * Manages PostgreSQL database connections for ThinkLink application
 */
public class DatabaseConnectionManager {
    private static final String DEFAULT_URL = "jdbc:postgresql://localhost:5432/thinklink";
    private static final String DEFAULT_USERNAME = "thinklink_user";
    private static final String DEFAULT_PASSWORD = "thinklink_pass";

    private static DatabaseConnectionManager instance;
    private String url;
    private String username;
    private String password;

    private DatabaseConnectionManager() {
        loadDatabaseConfig();
    }

    public static synchronized DatabaseConnectionManager getInstance() {
        if (instance == null) {
            instance = new DatabaseConnectionManager();
        }
        return instance;
    }

    private void loadDatabaseConfig() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("database.properties")) {
            if (input != null) {
                Properties props = new Properties();
                props.load(input);

                this.url = props.getProperty("db.url", DEFAULT_URL);
                this.username = props.getProperty("db.username", DEFAULT_USERNAME);
                this.password = props.getProperty("db.password", DEFAULT_PASSWORD);
            } else {
                // Use default values if properties file not found
                this.url = DEFAULT_URL;
                this.username = DEFAULT_USERNAME;
                this.password = DEFAULT_PASSWORD;
            }
        } catch (IOException e) {
            System.err.println("Error loading database configuration: " + e.getMessage());
            // Use default values
            this.url = DEFAULT_URL;
            this.username = DEFAULT_USERNAME;
            this.password = DEFAULT_PASSWORD;
        }
    }

    public Connection getConnection() throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
            return DriverManager.getConnection(url, username, password);
        } catch (ClassNotFoundException e) {
            throw new SQLException("PostgreSQL JDBC driver not found", e);
        }
    }

    public void testConnection() throws SQLException {
        try (Connection conn = getConnection()) {
            System.out.println("Database connection successful!");
        }
    }

    /**
     * Initialize database schema - creates tables if they don't exist
     */
    public void initializeDatabase() throws SQLException {
        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement()) {

            // Create users table
            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS users (
                            user_email VARCHAR(255) PRIMARY KEY,
                            role VARCHAR(50) NOT NULL DEFAULT 'Customary',
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            last_login TIMESTAMP,
                            is_active BOOLEAN DEFAULT TRUE
                        )
                    """);

            // Create boards table
            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS boards (
                            board_id VARCHAR(255) PRIMARY KEY,
                            board_name VARCHAR(255) NOT NULL,
                            creator_email VARCHAR(255) NOT NULL REFERENCES users(user_email),
                            is_shared BOOLEAN DEFAULT FALSE,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                    """);

            // Create boxes table
            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS boxes (
                            box_id SERIAL PRIMARY KEY,
                            board_id VARCHAR(255) NOT NULL REFERENCES boards(board_id) ON DELETE CASCADE,
                            title VARCHAR(500) NOT NULL DEFAULT 'New Task',
                            content TEXT DEFAULT '',
                            position_x INTEGER NOT NULL DEFAULT 0,
                            position_y INTEGER NOT NULL DEFAULT 0,
                            width INTEGER DEFAULT 150,
                            height INTEGER DEFAULT 100,
                            color VARCHAR(20) DEFAULT '#F0F0F0',
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                    """);

            // Create box_connections table
            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS box_connections (
                            connection_id SERIAL PRIMARY KEY,
                            source_box_id INTEGER NOT NULL REFERENCES boxes(box_id) ON DELETE CASCADE,
                            target_box_id INTEGER NOT NULL REFERENCES boxes(box_id) ON DELETE CASCADE,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            UNIQUE(source_box_id, target_box_id)
                        )
                    """);

            // Create notes table
            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS notes (
                            note_id SERIAL PRIMARY KEY,
                            user_email VARCHAR(255) NOT NULL REFERENCES users(user_email),
                            title VARCHAR(255) NOT NULL,
                            content TEXT,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            UNIQUE(user_email, title)
                        )
                    """);

            // Create checklists table
            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS checklists (
                            checklist_id SERIAL PRIMARY KEY,
                            user_email VARCHAR(255) NOT NULL REFERENCES users(user_email),
                            title VARCHAR(255) NOT NULL,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            UNIQUE(user_email, title)
                        )
                    """);

            // Create checklist_items table
            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS checklist_items (
                            item_id SERIAL PRIMARY KEY,
                            checklist_id INTEGER NOT NULL REFERENCES checklists(checklist_id) ON DELETE CASCADE,
                            text VARCHAR(500) NOT NULL,
                            is_completed BOOLEAN DEFAULT FALSE,
                            position_order INTEGER DEFAULT 0,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                    """);

            // Create deadlines table
            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS deadlines (
                            deadline_id SERIAL PRIMARY KEY,
                            description VARCHAR(500) NOT NULL,
                            due_date TIMESTAMP NOT NULL,
                            assigned_to VARCHAR(255) REFERENCES users(user_email),
                            created_by VARCHAR(255) NOT NULL REFERENCES users(user_email),
                            is_completed BOOLEAN DEFAULT FALSE,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                    """);

            // Create indexes for performance
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_boxes_board_id ON boxes(board_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_box_connections_source ON box_connections(source_box_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_box_connections_target ON box_connections(target_box_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_notes_user ON notes(user_email)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_checklists_user ON checklists(user_email)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_deadlines_assigned ON deadlines(assigned_to)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_deadlines_due_date ON deadlines(due_date)");

            System.out.println("Database schema initialized successfully!");

        } catch (SQLException e) {
            System.err.println("Error initializing database schema: " + e.getMessage());
            throw e;
        }
    }
}