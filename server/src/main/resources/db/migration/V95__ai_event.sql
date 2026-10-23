-- AI 事件采集（K7）：加购/搜索/点击，供推荐与检索优化使用
CREATE TABLE ai_event (
    id           UUID PRIMARY KEY,
    event_type   VARCHAR(32) NOT NULL,
    member_id    UUID,
    anonymous_id VARCHAR(64),
    sku_id       UUID,
    keyword      VARCHAR(128),
    payload      JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ai_event_type_check
        CHECK (event_type IN ('VIEW', 'ADD_TO_CART', 'SEARCH', 'CLICK', 'PURCHASE'))
);

CREATE INDEX ai_event_type_time_idx ON ai_event (event_type, created_at DESC);
CREATE INDEX ai_event_sku_idx ON ai_event (sku_id);
CREATE INDEX ai_event_keyword_idx ON ai_event (keyword);
