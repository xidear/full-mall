ALTER TABLE staff_user
    ADD COLUMN pin_hash VARCHAR(255);

CREATE TABLE staff_store (
    staff_id UUID NOT NULL REFERENCES staff_user(id),
    store_id UUID NOT NULL REFERENCES store(id),
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (staff_id, store_id)
);

INSERT INTO staff_store (staff_id, store_id)
VALUES ('aaaaaaaa-0000-0000-0000-000000000003', '22222222-2222-2222-2222-222222222222')
ON CONFLICT DO NOTHING;
