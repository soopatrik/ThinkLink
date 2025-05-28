-- ThinkLink PostgreSQL Database Schema
-- Run this script to set up the database

-- Create database and user
CREATE DATABASE thinklink;
CREATE USER thinklink_user WITH PASSWORD 'thinklink_pass';
GRANT ALL PRIVILEGES ON DATABASE thinklink TO thinklink_user;

-- Connect to the thinklink database
\c thinklink;

-- Grant permissions to the user
GRANT ALL ON SCHEMA public TO thinklink_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO thinklink_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO thinklink_user;

-- Users table
CREATE TABLE users (
    user_email VARCHAR(255) PRIMARY KEY,
    role VARCHAR(50) NOT NULL DEFAULT 'Customary',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE
);

-- Boards table
CREATE TABLE boards (
    board_id VARCHAR(255) PRIMARY KEY,
    board_name VARCHAR(255) NOT NULL,
    creator_email VARCHAR(255) NOT NULL REFERENCES users(user_email),
    is_shared BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Boxes table
CREATE TABLE boxes (
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
);

-- Box connections table
CREATE TABLE box_connections (
    connection_id SERIAL PRIMARY KEY,
    source_box_id INTEGER NOT NULL REFERENCES boxes(box_id) ON DELETE CASCADE,
    target_box_id INTEGER NOT NULL REFERENCES boxes(box_id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(source_box_id, target_box_id)
);

-- Notes table
CREATE TABLE notes (
    note_id SERIAL PRIMARY KEY,
    user_email VARCHAR(255) NOT NULL REFERENCES users(user_email),
    title VARCHAR(255) NOT NULL,
    content TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_email, title)
);

-- Checklists table
CREATE TABLE checklists (
    checklist_id SERIAL PRIMARY KEY,
    user_email VARCHAR(255) NOT NULL REFERENCES users(user_email),
    title VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_email, title)
);

-- Checklist items table
CREATE TABLE checklist_items (
    item_id SERIAL PRIMARY KEY,
    checklist_id INTEGER NOT NULL REFERENCES checklists(checklist_id) ON DELETE CASCADE,
    text VARCHAR(500) NOT NULL,
    is_completed BOOLEAN DEFAULT FALSE,
    position_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Deadlines table
CREATE TABLE deadlines (
    deadline_id SERIAL PRIMARY KEY,
    description VARCHAR(500) NOT NULL,
    due_date TIMESTAMP NOT NULL,
    assigned_to VARCHAR(255) REFERENCES users(user_email),
    created_by VARCHAR(255) NOT NULL REFERENCES users(user_email),
    is_completed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for performance
CREATE INDEX idx_boxes_board_id ON boxes(board_id);
CREATE INDEX idx_box_connections_source ON box_connections(source_box_id);
CREATE INDEX idx_box_connections_target ON box_connections(target_box_id);
CREATE INDEX idx_notes_user ON notes(user_email);
CREATE INDEX idx_checklists_user ON checklists(user_email);
CREATE INDEX idx_deadlines_assigned ON deadlines(assigned_to);
CREATE INDEX idx_deadlines_due_date ON deadlines(due_date);

-- Insert default shared board
INSERT INTO users (user_email, role) VALUES ('system@thinklink.com', 'Administrator');
INSERT INTO boards (board_id, board_name, creator_email, is_shared) 
VALUES ('shared-global-board', 'Global Shared Board', 'system@thinklink.com', TRUE);

-- Grant final permissions
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO thinklink_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO thinklink_user;