CREATE TABLE theme_config (
    id UUID PRIMARY KEY,
    channel VARCHAR(16) NOT NULL,
    market_code VARCHAR(8) NOT NULL,
    config_json JSONB NOT NULL,
    version INT NOT NULL DEFAULT 1,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (channel, market_code)
);

CREATE INDEX idx_theme_config_channel ON theme_config (channel, market_code);