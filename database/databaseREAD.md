# PostgreSQL Database Configuration for ThinkLink

db.url=jdbc:postgresql://localhost:5432/thinklink
db.username=thinklink_user
db.password=thinklink_pass

# Connection Pool Settings (if using connection pooling)

db.pool.minConnections=5
db.pool.maxConnections=20
db.pool.connectionTimeout=30000

# Database Schema Settings

db.schema.autoCreate=true
db.schema.autoUpdate=true

# Create database

psql -U postgres -c "CREATE DATABASE thinklink;"
psql -U postgres -c "CREATE USER thinklink_user WITH PASSWORD 'thinklink_pass';"
psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE thinklink TO thinklink_user;"

# Run the schema

psql -U thinklink_user -d thinklink -f database/schema.sql
