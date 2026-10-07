ALTER TABLE payment_alerts
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'OPEN';

ALTER TABLE payment_alerts
    ADD COLUMN resolved_at TIMESTAMP NULL;