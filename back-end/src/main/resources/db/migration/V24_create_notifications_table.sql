CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,

    user_id VARCHAR(50) NOT NULL,

    type VARCHAR(30) NOT NULL,
    -- MATCH, MESSAGE, APPOINTMENT

    message TEXT NOT NULL,

    reference_id VARCHAR(50),
    -- matchId / appointmentId / chatId

    is_read BOOLEAN DEFAULT FALSE,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notifications_user_id
ON notifications(user_id);

CREATE INDEX idx_notifications_unread
ON notifications(user_id, is_read);
