ALTER TABLE payment_outbox
    ADD COLUMN claimed_by UUID NULL,
    ADD COLUMN claimed_until TIMESTAMP NULL;

CREATE INDEX idx_payment_outbox_claimable
    ON payment_outbox (created_at, id)
    WHERE published_at IS NULL;