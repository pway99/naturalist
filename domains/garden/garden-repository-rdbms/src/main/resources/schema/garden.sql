-- Garden domain schema. A single flat table with NO foreign keys: plantName is a polymorphic
-- rank ref (stays slug even though plants is on RDBMS), zone/subzone belong to the zone skeleton
-- (no rdbms yet), and cultivar is a cross-domain plants slug. All references are plain columns.

DROP TABLE IF EXISTS planting;

CREATE TABLE planting (
    id            UUID PRIMARY KEY,
    plant_rank    VARCHAR(16),                 -- nullable (ORDER|FAMILY|GENUS|SPECIES)
    plant_name    VARCHAR(64),                 -- nullable polymorphic plant rank slug, no FK
    cultivar_name VARCHAR(64),                 -- nullable cultivar slug, no FK
    zone_name     VARCHAR(64) NOT NULL,        -- zone slug, no FK
    sub_zone_name VARCHAR(64),                 -- nullable zone slug, no FK
    plant_count   INTEGER,                     -- nullable
    planted_date  DATE NOT NULL,
    removed_date  DATE,                         -- nullable
    notes         TEXT                          -- nullable
);
