# ThinkLink - Collaborative Mind Mapping & Project Management

A Java-based collaborative mind mapping application with real-time synchronization, integrated notes, checklists, and calendar functionality. Features PostgreSQL database integration for persistent data storage.

## Features

- **Interactive Mind Maps**: Create and edit connected boxes with drag-and-drop functionality
- **Real-time Collaboration**: Multiple users can work on the same board simultaneously
- **Personal Notes**: Create and manage personal notes with database storage
- **Goal Tracking**: Checklist functionality for task management
- **Calendar Integration**: Schedule and track deadlines
- **User Management**: Role-based access (Administrator/Regular User)
- **Database Integration**: PostgreSQL for persistent storage with file-based fallback

## Prerequisites

- **Java 11+** (JDK)
- **VS Code** with Java Extension Pack
- **PostgreSQL 13+**
- **DataGrip** (for database management)

## Database Setup

### Step 1: Install PostgreSQL

- Download from: https://www.postgresql.org/download/
- Follow installation instructions for your OS
- Remember the password you set for the `postgres` user

### Step 2: Set Up Database in DataGrip

1. **Open DataGrip**
2. **Create PostgreSQL connection:**

   - Click **"+" → "Data Source" → "PostgreSQL"**
   - **Host:** `localhost`
   - **Port:** `5432`
   - **Database:** `postgres`
   - **User:** `postgres`
   - **Password:** [your postgres password]
   - **Test Connection**

3. **Create ThinkLink database:**

   ```sql
   -- Run this in DataGrip query console
   CREATE DATABASE thinklink;
   CREATE USER thinklink_user WITH PASSWORD 'thinklink_pass';
   GRANT ALL PRIVILEGES ON DATABASE thinklink TO thinklink_user;
   ```

4. **Create connection to ThinkLink database:**

   - **Host:** `localhost`
   - **Port:** `5432`
   - **Database:** `thinklink`
   - **User:** `thinklink_user`
   - **Password:** `thinklink_pass`

5. **Initialize schema:**
   - Copy all contents from `database_schema.sql`
   - Paste and execute in the `thinklink` database connection
   - Verify tables are created: `users`, `boards`, `boxes`, `box_connections`, `notes`, `checklists`, `checklist_items`, `deadlines`

### Step 3: VS Code Configuration

Ensure `.vscode/settings.json` contains:

```json
{
  "java.project.referencedLibraries": ["lib/**/*.jar"],
  "java.configuration.updateBuildConfiguration": "automatic"
}
```

### Step 4: Database Configuration

Verify `src/main/resources/database.properties`:

```properties
db.url=jdbc:postgresql://localhost:5432/thinklink
db.username=thinklink_user
db.password=thinklink_pass
```

## Project Setup

### Step 1: Download Dependencies

Download the PostgreSQL JDBC driver:

- **File:** [postgresql-42.7.1.jar](https://repo1.maven.org/maven2/org/postgresql/postgresql/42.7.1/postgresql-42.7.1.jar)
- **Location:** Place in `lib/postgresql-42.7.1.jar`

### Step 2: Project Structure
