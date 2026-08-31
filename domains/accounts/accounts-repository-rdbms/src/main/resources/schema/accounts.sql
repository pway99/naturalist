DROP TABLE IF EXISTS email_verification_token;
DROP TABLE IF EXISTS account;

CREATE TABLE account (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name           VARCHAR(64)  NOT NULL UNIQUE,
    email          VARCHAR(255) NOT NULL UNIQUE,
    password_hash  VARCHAR(80)  NOT NULL,
    email_verified BOOLEAN      NOT NULL,
    access         VARCHAR(32)  NOT NULL,
    status         VARCHAR(32)  NOT NULL
);

CREATE TABLE email_verification_token (
    id           UUID PRIMARY KEY,
    account_id   BIGINT       NOT NULL REFERENCES account(id),
    token_hash   VARCHAR(128) NOT NULL,
    purpose      VARCHAR(32)  NOT NULL,
    expires_at   TIMESTAMPTZ  NOT NULL,
    consumed_at  TIMESTAMPTZ
);
