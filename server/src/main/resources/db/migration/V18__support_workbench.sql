ALTER TABLE support_conversation
    ADD COLUMN subject VARCHAR(255),
    ADD COLUMN customer_name VARCHAR(64),
    ADD COLUMN customer_phone VARCHAR(32),
    ADD COLUMN assigned_at TIMESTAMPTZ,
    ADD COLUMN last_message_at TIMESTAMPTZ;

ALTER TABLE support_message
    ADD COLUMN metadata_json JSONB;

CREATE TABLE support_transfer (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES support_conversation(id),
    from_agent_id UUID,
    to_agent_id UUID NOT NULL,
    reason VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_support_transfer_conversation ON support_transfer (conversation_id, created_at DESC);
CREATE INDEX idx_support_conversation_agent ON support_conversation (agent_id, status, last_message_at DESC);
