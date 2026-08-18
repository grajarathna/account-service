CREATE TABLE account
(
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    account_number VARCHAR(20) NOT NULL,
    customer_name VARCHAR(100) NOT NULL,
    account_nickname VARCHAR(30),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_accounts_account_number
        UNIQUE (account_number),

    CONSTRAINT chk_account_nickname_length
        CHECK (
            account_nickname IS NULL
                OR char_length(account_nickname)
                BETWEEN 5 AND 30
            )
);

CREATE INDEX idx_account_customer_id
    ON account (customer_id);

--TODO
-- if filter by customer name is support is given, then we can use a gin index on customer_name