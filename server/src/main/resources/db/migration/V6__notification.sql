CREATE TABLE notification (
    id UUID PRIMARY KEY,
    recipient_type VARCHAR(16) NOT NULL,
    recipient_id VARCHAR(128) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    channel VARCHAR(16) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(16) NOT NULL,
    read_at TIMESTAMPTZ,
    sent_at TIMESTAMPTZ,
    error_message VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_notification_recipient ON notification (recipient_type, recipient_id, created_at);
CREATE INDEX idx_notification_created ON notification (created_at);
CREATE INDEX idx_notification_unread ON notification (recipient_type, read_at);