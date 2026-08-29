DROP TABLE IF EXISTS citation_association;
DROP TABLE IF EXISTS citation;
DROP TABLE IF EXISTS glossary_term;
DROP TABLE IF EXISTS concept;

CREATE TABLE concept (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name                    VARCHAR(64) NOT NULL UNIQUE,
    title                   TEXT        NOT NULL,
    description_preschool   TEXT        NOT NULL,
    description_elementary  TEXT        NOT NULL,
    description_secondary   TEXT        NOT NULL,
    description_university   TEXT       NOT NULL
);

CREATE TABLE glossary_term (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name       VARCHAR(64)  NOT NULL UNIQUE,
    term       VARCHAR(128) NOT NULL,
    definition TEXT         NOT NULL,
    example    TEXT
);

CREATE TABLE citation (
    id                            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name                          VARCHAR(200) NOT NULL UNIQUE,
    kind                          VARCHAR(32)  NOT NULL,
    title                         TEXT         NOT NULL,
    author                        TEXT,
    year                          INTEGER,
    last_modified                 TIMESTAMPTZ,
    authority_source_id           VARCHAR(64)  NOT NULL,
    authority_source_display_name VARCHAR(128) NOT NULL,
    authority_url                 TEXT         NOT NULL
);

CREATE TABLE citation_association (
    id             UUID PRIMARY KEY,
    citation_id    BIGINT       NOT NULL REFERENCES citation(id),
    subject_domain VARCHAR(64)  NOT NULL,
    subject_rank   VARCHAR(32)  NOT NULL,
    subject_name   VARCHAR(128) NOT NULL,
    note           TEXT
);
