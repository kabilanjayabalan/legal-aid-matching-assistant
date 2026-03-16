CREATE TABLE chats (
    id BIGSERIAL PRIMARY KEY,
    match_id INTEGER NOT NULL,
    sender_id INTEGER NOT NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_chat_match
        FOREIGN KEY (match_id) REFERENCES matches(id) ON DELETE CASCADE,

    CONSTRAINT fk_chat_sender
        FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_chats_match_id ON chats(match_id);
CREATE INDEX idx_chats_created_at ON chats(created_at);
