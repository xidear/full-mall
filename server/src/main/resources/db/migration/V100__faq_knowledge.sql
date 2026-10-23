-- FAQ 知识库与未命中记录（L5 自学习）
CREATE TABLE faq_entry (
    id         UUID PRIMARY KEY,
    question   VARCHAR(300) NOT NULL,
    answer     VARCHAR(2000) NOT NULL,
    keywords   TEXT NOT NULL DEFAULT '',
    status     VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT faq_entry_status_check CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    CONSTRAINT faq_entry_question_unique UNIQUE (question)
);

CREATE TABLE faq_miss (
    id         UUID PRIMARY KEY,
    question   VARCHAR(300) NOT NULL,
    member_id  UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX faq_miss_created_idx ON faq_miss (created_at DESC);
CREATE INDEX faq_entry_question_trgm_idx ON faq_entry (lower(question));
