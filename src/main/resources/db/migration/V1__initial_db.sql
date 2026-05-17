CREATE TABLE IF NOT EXISTS accounts
(
    id             UUID PRIMARY KEY            DEFAULT gen_random_uuid(),
    account_number VARCHAR(20) UNIQUE NOT NULL,
    owner_name     VARCHAR(100)       NOT NULL,
    balance        NUMERIC(19, 4)     NOT NULL DEFAULT 0,
    currency       VARCHAR(3)         NOT NULL DEFAULT 'VND',
    status         VARCHAR(20)        NOT NULL DEFAULT 'ACTIVE',
    version        BIGINT             NOT NULL DEFAULT 0, -- Optimistic Locking
    created_at     TIMESTAMP          NOT NULL DEFAULT now(),
    updated_at     TIMESTAMP          NULL
);

CREATE TABLE IF NOT EXISTS transactions
(
    id              UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    idempotency_key VARCHAR(100) UNIQUE,     -- chống duplicate request
    from_account_id UUID REFERENCES accounts (id),
    to_account_id   UUID REFERENCES accounts (id),
    amount          NUMERIC(19, 4) NOT NULL,
    type            VARCHAR(20)    NOT NULL, -- TRANSFER, DEPOSIT, WITHDRAW
    status          VARCHAR(20)    NOT NULL DEFAULT 'COMPLETED',
    description     VARCHAR(255),
    created_at      TIMESTAMP      NOT NULL DEFAULT now()
);

-- Index để query nhanh
CREATE INDEX IF NOT EXISTS idx_transactions_from ON transactions (from_account_id);
CREATE INDEX IF NOT EXISTS idx_accounts_number ON accounts (account_number);

-- Seed data
INSERT INTO accounts (id, account_number, owner_name, balance, currency, status, version, created_at)
VALUES (gen_random_uuid(), 'ACC0001', 'Nguyen Van A', 10000000, 'VND', 'ACTIVE', 0, now()),
       (gen_random_uuid(), 'ACC0002', 'Tran Thi B', 5000000, 'VND', 'ACTIVE', 0, now())
ON CONFLICT
    DO NOTHING;