-- ===================================
-- Database Setup for Anamnesebogen
-- ===================================

-- Create the database
CREATE DATABASE anamnesebogen
    WITH
    ENCODING = 'UTF8'
    LC_COLLATE = 'German_Germany.1252'
    LC_CTYPE = 'German_Germany.1252'
    TEMPLATE = template0;

-- Connect to the new database
\c anamnesebogen

-- Create the user
CREATE USER anamnesebogen_user WITH PASSWORD 'postgresanamnese';

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE anamnesebogen TO anamnesebogen_user;
GRANT ALL PRIVILEGES ON SCHEMA public TO anamnesebogen_user;

-- Grant future table privileges
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO anamnesebogen_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO anamnesebogen_user;

-- Confirm setup
SELECT 'Database anamnesebogen created successfully!' AS status;
