-- Plants domain schema. The rank chain (order to family to genus to species) is a real numeric
-- FK at every rung. Cross-rank attachments (plant_rank + slug pairs) carry NO FK (polymorphic
-- over four rank tables). Cross-domain and cross-kernel refs (compound_name, observed_by,
-- bioregion, placed_in) are plain slug columns with NO FK.

CREATE EXTENSION IF NOT EXISTS pg_trgm;

DROP TABLE IF EXISTS phytochemical_constituent_tissue;
DROP TABLE IF EXISTS phytochemical_constituent_role;
DROP TABLE IF EXISTS phytochemical_constituent;
DROP TABLE IF EXISTS seed_lineage;
DROP TABLE IF EXISTS cultivar;
DROP TABLE IF EXISTS plant_program;
DROP TABLE IF EXISTS plant_image;
DROP TABLE IF EXISTS plant_observation_candidate;
DROP TABLE IF EXISTS plant_observation;
DROP TABLE IF EXISTS plant_feature_assignment;
DROP TABLE IF EXISTS plant_feature;
DROP TABLE IF EXISTS plant_ecological_role_role;
DROP TABLE IF EXISTS plant_ecological_role;
DROP TABLE IF EXISTS plant_species_native_bioregion;
DROP TABLE IF EXISTS plant_species_common_name CASCADE;
DROP TABLE IF EXISTS plant_species CASCADE;
DROP TABLE IF EXISTS plant_genus_common_name CASCADE;
DROP TABLE IF EXISTS plant_genus CASCADE;
DROP TABLE IF EXISTS plant_family_common_name CASCADE;
DROP TABLE IF EXISTS plant_family CASCADE;
DROP TABLE IF EXISTS plant_order_common_name CASCADE;
DROP TABLE IF EXISTS plant_order CASCADE;

-- ── Rank chain ────────────────────────────────────────────────────────────────────────────
CREATE TABLE plant_order (
    id                     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name                   VARCHAR(64)  NOT NULL UNIQUE,
    taxonomic_order        VARCHAR(128) NOT NULL,
    placed_in              VARCHAR(64),                       -- clade slug (kernels/clades), no FK
    description_preschool  TEXT NOT NULL,
    description_elementary TEXT NOT NULL,
    description_secondary  TEXT NOT NULL,
    description_university  TEXT NOT NULL
);
CREATE TABLE plant_order_common_name (
    order_id BIGINT       NOT NULL REFERENCES plant_order(id),
    label    VARCHAR(128) NOT NULL,
    locale   VARCHAR(35)  NOT NULL,
    PRIMARY KEY (order_id, label, locale)
);

CREATE TABLE plant_family (
    id                     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name                   VARCHAR(64)  NOT NULL UNIQUE,
    order_id               BIGINT       NOT NULL REFERENCES plant_order(id),
    taxonomic_family       VARCHAR(128) NOT NULL,
    description_preschool  TEXT NOT NULL,
    description_elementary TEXT NOT NULL,
    description_secondary  TEXT NOT NULL,
    description_university  TEXT NOT NULL
);
CREATE TABLE plant_family_common_name (
    family_id BIGINT       NOT NULL REFERENCES plant_family(id),
    label     VARCHAR(128) NOT NULL,
    locale    VARCHAR(35)  NOT NULL,
    PRIMARY KEY (family_id, label, locale)
);

CREATE TABLE plant_genus (
    id                     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name                   VARCHAR(64)  NOT NULL UNIQUE,
    family_id              BIGINT       NOT NULL REFERENCES plant_family(id),
    taxonomic_family       VARCHAR(128) NOT NULL,
    taxonomic_genus        VARCHAR(128) NOT NULL,
    description_preschool  TEXT NOT NULL,
    description_elementary TEXT NOT NULL,
    description_secondary  TEXT NOT NULL,
    description_university  TEXT NOT NULL
);
CREATE TABLE plant_genus_common_name (
    genus_id BIGINT       NOT NULL REFERENCES plant_genus(id),
    label    VARCHAR(128) NOT NULL,
    locale   VARCHAR(35)  NOT NULL,
    PRIMARY KEY (genus_id, label, locale)
);

CREATE TABLE plant_species (
    id                     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name                   VARCHAR(64)  NOT NULL UNIQUE,
    genus_id               BIGINT       NOT NULL REFERENCES plant_genus(id),
    epithet                VARCHAR(128) NOT NULL,
    growth_habit           VARCHAR(32)  NOT NULL,
    life_cycle             VARCHAR(32)  NOT NULL,
    description_preschool  TEXT NOT NULL,
    description_elementary TEXT NOT NULL,
    description_secondary  TEXT NOT NULL,
    description_university  TEXT NOT NULL
);
CREATE TABLE plant_species_common_name (
    species_id BIGINT       NOT NULL REFERENCES plant_species(id),
    label      VARCHAR(128) NOT NULL,
    locale     VARCHAR(35)  NOT NULL,
    PRIMARY KEY (species_id, label, locale)
);
CREATE TABLE plant_species_native_bioregion (
    species_id BIGINT      NOT NULL REFERENCES plant_species(id),
    bioregion  VARCHAR(64) NOT NULL,                          -- biogeography slug, no FK
    PRIMARY KEY (species_id, bioregion)
);

-- ── Cross-rank surrogate-UUID entities ──────────────────────────────────────────────────────
CREATE TABLE plant_ecological_role (
    id         UUID PRIMARY KEY,
    plant_rank VARCHAR(16) NOT NULL,                          -- ORDER|FAMILY|GENUS|SPECIES
    plant_name VARCHAR(64) NOT NULL,                          -- polymorphic rank ref, no FK
    UNIQUE (plant_rank, plant_name)
);
CREATE TABLE plant_ecological_role_role (
    role_id UUID        NOT NULL REFERENCES plant_ecological_role(id),
    role    VARCHAR(48) NOT NULL,
    PRIMARY KEY (role_id, role)
);

CREATE TABLE plant_feature (
    id    UUID PRIMARY KEY,
    value VARCHAR(255) NOT NULL UNIQUE
);
CREATE TABLE plant_feature_assignment (
    id         UUID PRIMARY KEY,
    feature_id UUID        NOT NULL REFERENCES plant_feature(id),
    rank       VARCHAR(16) NOT NULL,                          -- ORDER|FAMILY|GENUS|SPECIES
    rank_name  VARCHAR(64) NOT NULL,                          -- polymorphic rank ref, no FK
    ordinal    INTEGER     NOT NULL,
    UNIQUE (feature_id, rank_name)
);

CREATE TABLE plant_observation (
    id                       UUID PRIMARY KEY,
    observed_by              VARCHAR(128) NOT NULL,            -- naturalist slug, no FK
    subject_rank             VARCHAR(16)  NOT NULL,            -- ORDER|FAMILY|GENUS|SPECIES
    subject                  VARCHAR(64)  NOT NULL,            -- polymorphic rank ref, no FK
    observed_on              TIMESTAMPTZ  NOT NULL,
    notes                    TEXT,
    location                 TEXT,
    identification_confidence DOUBLE PRECISION,               -- null group: absent when no Identification
    identification_evidence  TEXT
);
CREATE TABLE plant_observation_candidate (
    observation_id  UUID             NOT NULL REFERENCES plant_observation(id),
    ordinal         INTEGER          NOT NULL,
    scientific_name TEXT             NOT NULL,
    common_name     TEXT,
    confidence      DOUBLE PRECISION NOT NULL,
    PRIMARY KEY (observation_id, ordinal)
);

CREATE TABLE plant_image (
    id             UUID PRIMARY KEY,
    parent_rank    VARCHAR(16)  NOT NULL,                     -- ORDER|FAMILY|GENUS|SPECIES
    parent_name    VARCHAR(64)  NOT NULL,                     -- polymorphic rank ref, no FK
    date_added     TIMESTAMPTZ  NOT NULL,
    resource_name  VARCHAR(255) NOT NULL,
    observation_id UUID REFERENCES plant_observation(id)      -- nullable
);

-- ── Sub-context NamedEntities ───────────────────────────────────────────────────────────────
CREATE TABLE cultivar (
    id                     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name                   VARCHAR(64)  NOT NULL UNIQUE,
    plant_species_id       BIGINT       NOT NULL REFERENCES plant_species(id),
    common_name            VARCHAR(128) NOT NULL,
    variety_type           VARCHAR(32)  NOT NULL,
    fruit_type             VARCHAR(32),                       -- nullable
    seed_saving_policy     VARCHAR(32)  NOT NULL,
    seed_source            TEXT,
    garden_notes           TEXT,
    description_preschool  TEXT NOT NULL,
    description_elementary TEXT NOT NULL,
    description_secondary  TEXT NOT NULL,
    description_university   TEXT NOT NULL
);

CREATE TABLE seed_lineage (
    id                               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name                             VARCHAR(64)  NOT NULL UNIQUE,
    cultivar_id                      BIGINT       NOT NULL REFERENCES cultivar(id),
    adaptation_start_year            INTEGER      NOT NULL,
    selection_criteria               TEXT,
    notes                            TEXT,
    provenance_originator            VARCHAR(128) NOT NULL,
    provenance_origin_location       VARCHAR(128) NOT NULL,
    provenance_estimated_generations INTEGER      NOT NULL,
    provenance_source_notes          TEXT,
    description_preschool            TEXT NOT NULL,
    description_elementary           TEXT NOT NULL,
    description_secondary            TEXT NOT NULL,
    description_university            TEXT NOT NULL
);

CREATE TABLE plant_program (
    id                     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name                   VARCHAR(64) NOT NULL UNIQUE,
    plant_rank             VARCHAR(16) NOT NULL,              -- ORDER|FAMILY|GENUS|SPECIES
    plant_name             VARCHAR(64) NOT NULL,              -- polymorphic rank ref, no FK
    program_constraint     TEXT,                              -- 'constraint' is a reserved word
    notes                  TEXT,
    description_preschool  TEXT NOT NULL,
    description_elementary TEXT NOT NULL,
    description_secondary  TEXT NOT NULL,
    description_university   TEXT NOT NULL
);

CREATE TABLE phytochemical_constituent (
    id                     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name                   VARCHAR(129) NOT NULL UNIQUE,      -- <plant-slug>-<compound-slug>
    plant_rank             VARCHAR(16)  NOT NULL,             -- ORDER|FAMILY|GENUS|SPECIES
    plant_name             VARCHAR(64)  NOT NULL,             -- polymorphic rank ref, no FK
    compound_name          VARCHAR(96)  NOT NULL,             -- chemistry compound slug, no FK
    category               VARCHAR(48)  NOT NULL,
    induction              VARCHAR(32)  NOT NULL,
    notes                  TEXT,
    description_preschool  TEXT NOT NULL,
    description_elementary TEXT NOT NULL,
    description_secondary  TEXT NOT NULL,
    description_university   TEXT NOT NULL
);
CREATE TABLE phytochemical_constituent_role (
    constituent_id BIGINT      NOT NULL REFERENCES phytochemical_constituent(id),
    role_kind      VARCHAR(48) NOT NULL,
    PRIMARY KEY (constituent_id, role_kind)
);
CREATE TABLE phytochemical_constituent_tissue (
    constituent_id BIGINT      NOT NULL REFERENCES phytochemical_constituent(id),
    tissue         VARCHAR(32) NOT NULL,
    PRIMARY KEY (constituent_id, tissue)
);

-- ── Cross-domain catalog search: token view + trigram indexes ──────────────────────────────
CREATE OR REPLACE VIEW plant_catalog_token AS
      SELECT o.name AS token, o.name AS slug, true  AS is_slug, 'plants' AS domain, 'plant-order'   AS entity_type FROM plant_order o
UNION ALL SELECT o.taxonomic_order, o.name, false, 'plants', 'plant-order'   FROM plant_order o
UNION ALL SELECT cn.label, o.name, false, 'plants', 'plant-order'   FROM plant_order_common_name  cn JOIN plant_order  o ON cn.order_id  = o.id
UNION ALL SELECT f.name, f.name, true,  'plants', 'plant-family'  FROM plant_family f
UNION ALL SELECT f.taxonomic_family, f.name, false, 'plants', 'plant-family'  FROM plant_family f
UNION ALL SELECT cn.label, f.name, false, 'plants', 'plant-family'  FROM plant_family_common_name cn JOIN plant_family f ON cn.family_id = f.id
UNION ALL SELECT g.name, g.name, true,  'plants', 'plant-genus'   FROM plant_genus g
UNION ALL SELECT g.taxonomic_genus, g.name, false, 'plants', 'plant-genus'   FROM plant_genus g
UNION ALL SELECT cn.label, g.name, false, 'plants', 'plant-genus'   FROM plant_genus_common_name  cn JOIN plant_genus  g ON cn.genus_id  = g.id
UNION ALL SELECT s.name, s.name, true,  'plants', 'plant-species' FROM plant_species s
UNION ALL SELECT s.epithet, s.name, false, 'plants', 'plant-species' FROM plant_species s
UNION ALL SELECT cn.label, s.name, false, 'plants', 'plant-species' FROM plant_species_common_name cn JOIN plant_species s ON cn.species_id = s.id;

-- Operator class is schema-qualified (public.gin_trgm_ops) rather than relying on search_path:
-- the schema-drift IT applies this DDL with search_path restricted to an isolated scratch
-- schema, and pg_trgm (installed once, database-wide) lives in "public".
CREATE INDEX IF NOT EXISTS plant_order_name_trgm   ON plant_order   USING gin (lower(name) public.gin_trgm_ops);
CREATE INDEX IF NOT EXISTS plant_family_name_trgm  ON plant_family  USING gin (lower(name) public.gin_trgm_ops);
CREATE INDEX IF NOT EXISTS plant_genus_name_trgm   ON plant_genus   USING gin (lower(name) public.gin_trgm_ops);
CREATE INDEX IF NOT EXISTS plant_species_name_trgm ON plant_species USING gin (lower(name) public.gin_trgm_ops);
CREATE INDEX IF NOT EXISTS plant_species_cn_trgm   ON plant_species_common_name USING gin (lower(label) public.gin_trgm_ops);
