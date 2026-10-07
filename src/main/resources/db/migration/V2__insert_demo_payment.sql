INSERT INTO payments (
    reference,
    source_system,
    source_payment_reference,
    branch_code,
    amount,
    currency,
    status,
    created_at,
    updated_at
)
VALUES (
           'PAY-2026-0001',
           'DEMO_SYSTEM',
           'DEMO-TXN-0001',
           'BR-001',
           150.00,
           'USD',
           'SUCCESS',
           CURRENT_TIMESTAMP,
           CURRENT_TIMESTAMP
       );