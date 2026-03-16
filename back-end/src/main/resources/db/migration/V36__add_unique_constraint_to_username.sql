-- Add unique constraint to username column to prevent duplicate users
ALTER TABLE users
ADD CONSTRAINT uk_users_username UNIQUE (username);

