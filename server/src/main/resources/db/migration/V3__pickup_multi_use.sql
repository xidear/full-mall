ALTER TABLE pickup
    ADD COLUMN total_uses INT NOT NULL DEFAULT 1,
    ADD COLUMN used_count INT NOT NULL DEFAULT 0;

ALTER TABLE pickup
    ADD CONSTRAINT pickup_total_uses_positive CHECK (total_uses > 0),
    ADD CONSTRAINT pickup_used_count_range CHECK (used_count >= 0 AND used_count <= total_uses);