CREATE TABLE payment_outbox (
                                id UUID PRIMARY KEY,

                                aggregate_id VARCHAR(100) NOT NULL,
                                event_type VARCHAR(50) NOT NULL,

                                payload JSONB NOT NULL,

                                created_at TIMESTAMP NOT NULL,
                                published_at TIMESTAMP NULL,

                                CONSTRAINT fk_outbox_payment_reference
                                    FOREIGN KEY (aggregate_id)
                                        REFERENCES payments(reference)
);

CREATE INDEX idx_payment_outbox_unpublished
    ON payment_outbox (created_at, id)
    WHERE published_at IS NULL;