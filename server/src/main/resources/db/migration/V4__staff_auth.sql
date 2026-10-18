CREATE TABLE staff_user (
    id UUID PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(64) NOT NULL,
    role VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE staff_session (
    token_hash VARCHAR(64) PRIMARY KEY,
    staff_id UUID NOT NULL REFERENCES staff_user(id),
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_staff_session_staff ON staff_session (staff_id);
CREATE INDEX idx_staff_session_expiry ON staff_session (expires_at);