ALTER TABLE notifications
ALTER COLUMN user_id TYPE INT
USING user_id::INT;

ALTER TABLE notifications
ADD CONSTRAINT fk_notifications_user
FOREIGN KEY (user_id)
REFERENCES users(id)
ON DELETE CASCADE;

