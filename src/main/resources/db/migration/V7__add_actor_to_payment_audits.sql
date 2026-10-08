ALTER TABLE payment_audits
    ADD COLUMN performed_by VARCHAR(255),
    ADD COLUMN branch_code VARCHAR(50);