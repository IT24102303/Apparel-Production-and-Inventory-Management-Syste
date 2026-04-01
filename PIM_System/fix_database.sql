-- SQL Script to fix the users table by removing the full_name column
-- Run this in phpMyAdmin or MySQL command line

USE pim_system;

-- Drop the full_name column if it exists
ALTER TABLE users DROP COLUMN IF EXISTS full_name;

-- If the above doesn't work (older MySQL versions), use this instead:
-- ALTER TABLE users DROP COLUMN full_name;



