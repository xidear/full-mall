ALTER TABLE promotion_redemption ADD COLUMN released_at TIMESTAMPTZ;
CREATE INDEX promotion_redemption_release_idx ON promotion_redemption (order_id, released_at);
