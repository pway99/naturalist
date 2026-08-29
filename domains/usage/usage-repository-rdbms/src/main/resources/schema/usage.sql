-- Usage domain schema. Three flat surrogate-UUID tables, no foreign keys: counter_name / naturalist /
-- counter are soft slug references (a counter_name is an activity slug, not a UsageCounter PK). Logical
-- keys are composite UNIQUEs. 'limit' is a reserved word so the column is limit_value.

DROP TABLE IF EXISTS usage_alert;
DROP TABLE IF EXISTS usage_event;
DROP TABLE IF EXISTS usage_counter;

CREATE TABLE usage_counter (
    id           UUID PRIMARY KEY,
    counter_name VARCHAR(64) NOT NULL,         -- activity slug, no FK
    scope        VARCHAR(24) NOT NULL,         -- UsageScope
    window_kind  VARCHAR(24) NOT NULL,         -- WindowKind
    since        TIMESTAMPTZ,                  -- nullable (required only for WindowKind.SINCE)
    limit_value  INTEGER     NOT NULL,
    active       BOOLEAN     NOT NULL,
    UNIQUE (counter_name, scope, window_kind)
);

CREATE TABLE usage_event (
    id           UUID PRIMARY KEY,
    counter_name VARCHAR(64) NOT NULL,         -- activity slug, no FK
    naturalist   VARCHAR(64) NOT NULL,         -- naturalist slug, no FK
    instant      TIMESTAMPTZ NOT NULL
);

CREATE TABLE usage_alert (
    id           UUID PRIMARY KEY,
    counter      VARCHAR(64) NOT NULL,         -- activity slug, no FK
    scope        VARCHAR(24) NOT NULL,         -- AlertScope
    kind         VARCHAR(24) NOT NULL,         -- AlertKind
    period       VARCHAR(64) NOT NULL,
    message      TEXT        NOT NULL,
    at           TIMESTAMPTZ NOT NULL,
    emailed      BOOLEAN     NOT NULL,
    acknowledged BOOLEAN     NOT NULL,
    UNIQUE (counter, scope, kind, period)
);
