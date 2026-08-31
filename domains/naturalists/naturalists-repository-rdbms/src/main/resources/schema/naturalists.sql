DROP TABLE IF EXISTS naturalist_credential;
DROP TABLE IF EXISTS naturalist;

CREATE TABLE naturalist (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name          VARCHAR(64)  NOT NULL UNIQUE,
    account_name  VARCHAR(64)  NOT NULL,
    public_handle VARCHAR(64)  NOT NULL UNIQUE,
    given_name    VARCHAR(100) NOT NULL,
    family_name   VARCHAR(100),
    role          VARCHAR(32)  NOT NULL,
    stage         VARCHAR(32)  NOT NULL,
    notes         TEXT
);
