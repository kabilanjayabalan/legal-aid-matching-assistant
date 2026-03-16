CREATE TABLE chat_file_attachments (
    id BIGSERIAL PRIMARY KEY,

    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(100),
    file_size BIGINT,

    data BYTEA NOT NULL,

    chat_message_id BIGINT NOT NULL,

    CONSTRAINT fk_chat_file_attachments_chat_message
        FOREIGN KEY (chat_message_id)
        REFERENCES chat_messages(id)
        ON DELETE CASCADE
);
