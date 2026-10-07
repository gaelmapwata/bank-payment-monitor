CREATE TABLE payment_audits (
                                id BIGSERIAL PRIMARY KEY,

                                payment_reference VARCHAR(255) NOT NULL,

                                action VARCHAR(50) NOT NULL,

                                previous_status VARCHAR(50),

                                new_status VARCHAR(50),

                                occurred_at TIMESTAMP NOT NULL
);