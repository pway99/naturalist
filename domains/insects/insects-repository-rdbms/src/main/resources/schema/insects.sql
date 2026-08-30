-- Insects domain schema. The rank chain (order to family to genus to species) is a real numeric FK
-- at every rung, and every rank carries a nullable clade placed_in slug (no FK). Cross-rank
-- attachments (rank + slug pairs, up to 96 chars to hold a subspecies slug) carry no FK. Cross-domain
-- and cross-kernel refs (observed_by naturalist, host/nectar plant slugs) are plain slug columns, no FK.

CREATE EXTENSION IF NOT EXISTS pg_trgm;

DROP TABLE IF EXISTS insect_life_stage_nectar_source;
DROP TABLE IF EXISTS insect_life_stage_parasitoid_host;
DROP TABLE IF EXISTS insect_life_stage_host_plant;
DROP TABLE IF EXISTS insect_life_stage_habitat_layer;
DROP TABLE IF EXISTS insect_life_stage_habitat_zone;
DROP TABLE IF EXISTS insect_life_stage_window;
DROP TABLE IF EXISTS insect_life_stage;
DROP TABLE IF EXISTS insect_image;
DROP TABLE IF EXISTS insect_observation_candidate;
DROP TABLE IF EXISTS insect_observation;
DROP TABLE IF EXISTS insect_functional_role_guild;
DROP TABLE IF EXISTS insect_functional_role;
DROP TABLE IF EXISTS insect_feature_assignment;
DROP TABLE IF EXISTS insect_feature;
DROP TABLE IF EXISTS insect_species_supporting_plant;
DROP TABLE IF EXISTS insect_species_habitat_layer;
DROP TABLE IF EXISTS insect_species_habitat_zone;
DROP TABLE IF EXISTS insect_species_protected_stage;
DROP TABLE IF EXISTS insect_species_common_name CASCADE;
DROP TABLE IF EXISTS insect_species CASCADE;
DROP TABLE IF EXISTS insect_genus_common_name CASCADE;
DROP TABLE IF EXISTS insect_genus CASCADE;
DROP TABLE IF EXISTS insect_family_common_name CASCADE;
DROP TABLE IF EXISTS insect_family CASCADE;
DROP TABLE IF EXISTS insect_order_common_name CASCADE;
DROP TABLE IF EXISTS insect_order CASCADE;

-- ── Rank chain ────────────────────────────────────────────────────────────────────────────
CREATE TABLE insect_order (
    id                     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name                   VARCHAR(64)  NOT NULL UNIQUE,
    taxonomic_order        VARCHAR(128) NOT NULL,
    placed_in              VARCHAR(64),
    description_preschool  TEXT NOT NULL,
    description_elementary TEXT NOT NULL,
    description_secondary  TEXT NOT NULL,
    description_university  TEXT NOT NULL
);
CREATE TABLE insect_order_common_name (
    order_id BIGINT       NOT NULL REFERENCES insect_order(id),
    label    VARCHAR(128) NOT NULL,
    locale   VARCHAR(35)  NOT NULL,
    PRIMARY KEY (order_id, label, locale)
);

CREATE TABLE insect_family (
    id                     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name                   VARCHAR(64)  NOT NULL UNIQUE,
    order_id               BIGINT       NOT NULL REFERENCES insect_order(id),
    taxonomic_family       VARCHAR(128) NOT NULL,
    placed_in              VARCHAR(64),
    description_preschool  TEXT NOT NULL,
    description_elementary TEXT NOT NULL,
    description_secondary  TEXT NOT NULL,
    description_university  TEXT NOT NULL
);
CREATE TABLE insect_family_common_name (
    family_id BIGINT       NOT NULL REFERENCES insect_family(id),
    label     VARCHAR(128) NOT NULL,
    locale    VARCHAR(35)  NOT NULL,
    PRIMARY KEY (family_id, label, locale)
);

CREATE TABLE insect_genus (
    id                     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name                   VARCHAR(64)  NOT NULL UNIQUE,
    family_id              BIGINT       NOT NULL REFERENCES insect_family(id),
    taxonomic_genus        VARCHAR(128) NOT NULL,
    placed_in              VARCHAR(64),
    description_preschool  TEXT NOT NULL,
    description_elementary TEXT NOT NULL,
    description_secondary  TEXT NOT NULL,
    description_university  TEXT NOT NULL
);
CREATE TABLE insect_genus_common_name (
    genus_id BIGINT       NOT NULL REFERENCES insect_genus(id),
    label    VARCHAR(128) NOT NULL,
    locale   VARCHAR(35)  NOT NULL,
    PRIMARY KEY (genus_id, label, locale)
);

CREATE TABLE insect_species (
    id                     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name                   VARCHAR(64)  NOT NULL UNIQUE,
    genus_id               BIGINT       NOT NULL REFERENCES insect_genus(id),
    epithet                VARCHAR(128) NOT NULL,
    placed_in              VARCHAR(64),
    sighting_notes         TEXT,
    -- ChemicalDefense (null group: present iff chem_mechanism non-null)
    chem_mechanism           TEXT,
    chem_source_compounds    TEXT,
    chem_aposematic_signal   TEXT,
    -- Voltinism (null group: present iff voltinism_pattern non-null)
    voltinism_pattern        VARCHAR(24),
    voltinism_notes          TEXT,
    -- HabitatProfile (null group: present iff a habitat_zone child row exists)
    habitat_moisture         VARCHAR(24),
    habitat_light            VARCHAR(24),
    -- HabitatRequirements (null group: present iff any req_* non-null)
    req_nectar_sources       TEXT,
    req_shelter              TEXT,
    req_prey_availability    TEXT,
    req_lighting             TEXT,
    req_elevation_range      TEXT,
    -- GardenConnections (null group: present iff a supporting_plant child row exists OR either text non-null)
    garden_relationship_to_other_beneficials TEXT,
    garden_natural_enemies   TEXT,
    -- BeneficialProfile (null group: present iff beneficial_significance non-null)
    beneficial_significance         TEXT,
    beneficial_pest_management_value TEXT,
    beneficial_ipm_notes            TEXT,
    -- EcologicalSignificance (null group: present iff any eco_* non-null)
    eco_indicator_value      TEXT,
    eco_food_web_position    TEXT,
    eco_regional_context     TEXT,
    description_preschool  TEXT NOT NULL,
    description_elementary TEXT NOT NULL,
    description_secondary  TEXT NOT NULL,
    description_university  TEXT NOT NULL
);
CREATE TABLE insect_species_common_name (
    species_id BIGINT       NOT NULL REFERENCES insect_species(id),
    label      VARCHAR(128) NOT NULL,
    locale     VARCHAR(35)  NOT NULL,
    PRIMARY KEY (species_id, label, locale)
);
CREATE TABLE insect_species_protected_stage (
    species_id BIGINT      NOT NULL REFERENCES insect_species(id),
    stage_kind VARCHAR(16) NOT NULL,
    PRIMARY KEY (species_id, stage_kind)
);
CREATE TABLE insect_species_habitat_zone (
    species_id BIGINT      NOT NULL REFERENCES insect_species(id),
    zone       VARCHAR(24) NOT NULL,
    PRIMARY KEY (species_id, zone)
);
CREATE TABLE insect_species_habitat_layer (
    species_id BIGINT      NOT NULL REFERENCES insect_species(id),
    layer      VARCHAR(24) NOT NULL,
    PRIMARY KEY (species_id, layer)
);
CREATE TABLE insect_species_supporting_plant (
    species_id BIGINT  NOT NULL REFERENCES insect_species(id),
    ordinal    INTEGER NOT NULL,
    plant      TEXT    NOT NULL,
    PRIMARY KEY (species_id, ordinal)
);

-- ── Cross-rank surrogate-UUID entities ──────────────────────────────────────────────────────
CREATE TABLE insect_feature (
    id    UUID PRIMARY KEY,
    value VARCHAR(255) NOT NULL UNIQUE
);
CREATE TABLE insect_feature_assignment (
    id         UUID PRIMARY KEY,
    feature_id UUID        NOT NULL REFERENCES insect_feature(id),
    rank       VARCHAR(16) NOT NULL,
    rank_name  VARCHAR(96) NOT NULL,
    ordinal    INTEGER     NOT NULL,
    UNIQUE (feature_id, rank_name)
);

CREATE TABLE insect_functional_role (
    id          UUID PRIMARY KEY,
    parent_rank VARCHAR(16) NOT NULL,
    parent_name VARCHAR(96) NOT NULL UNIQUE,
    beneficial  BOOLEAN     NOT NULL
);
CREATE TABLE insect_functional_role_guild (
    role_id UUID        NOT NULL REFERENCES insect_functional_role(id),
    guild   VARCHAR(24) NOT NULL,
    PRIMARY KEY (role_id, guild)
);

CREATE TABLE insect_observation (
    id                       UUID PRIMARY KEY,
    observed_by              VARCHAR(128) NOT NULL,
    subject_rank             VARCHAR(16)  NOT NULL,
    subject                  VARCHAR(96)  NOT NULL,
    observed_on              TIMESTAMPTZ  NOT NULL,
    notes                    TEXT,
    location                 TEXT,
    identification_confidence DOUBLE PRECISION,
    identification_evidence  TEXT
);
CREATE TABLE insect_observation_candidate (
    observation_id  UUID             NOT NULL REFERENCES insect_observation(id),
    ordinal         INTEGER          NOT NULL,
    scientific_name TEXT             NOT NULL,
    common_name     TEXT,
    confidence      DOUBLE PRECISION NOT NULL,
    PRIMARY KEY (observation_id, ordinal)
);

CREATE TABLE insect_image (
    id             UUID PRIMARY KEY,
    parent_rank    VARCHAR(16)  NOT NULL,
    parent_name    VARCHAR(96)  NOT NULL,
    date_added     TIMESTAMPTZ  NOT NULL,
    resource_name  VARCHAR(255) NOT NULL,
    observation_id UUID REFERENCES insect_observation(id)
);

-- ── Life stages (sealed NamedEntity, single-table inheritance over egg/larva/pupa/adult) ─────
CREATE TABLE insect_life_stage (
    id                     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name                   VARCHAR(80) NOT NULL UNIQUE,
    stage_kind             VARCHAR(16) NOT NULL,        -- EGG|LARVA|PUPA|ADULT
    parent_rank            VARCHAR(16) NOT NULL,
    parent_name            VARCHAR(96) NOT NULL,        -- cross-rank InsectRankName, no FK
    -- StagePhenology (non-null, windows in child table)
    phenology_notes        TEXT,
    -- StageHabitat (non-null, zones/layers in child tables)
    habitat_moisture       VARCHAR(24),
    habitat_light          VARCHAR(24),
    habitat_substrate      TEXT,
    habitat_microclimate   TEXT,
    habitat_spatial_notes  TEXT,
    -- StageChemistryRole (nullable)
    chemistry_role         VARCHAR(24),
    chemistry_notes        TEXT,
    -- EggStage
    egg_color_progression      TEXT,
    egg_laying_pattern         TEXT,
    egg_adaptive_significance  TEXT,
    -- LarvaStage (hostPlants/parasitoidHosts in child tables)
    larva_feeding_strategy     VARCHAR(16),
    larva_remarkable_behavior  TEXT,
    larva_instar_progression   TEXT,
    -- PupaStage (DiapauseRegulation flattened: kind + two generic permit fields)
    pupa_appearance            TEXT,
    pupa_adaptive_significance TEXT,
    diapause_kind              VARCHAR(24),
    diapause_field_a           TEXT,
    diapause_field_b           TEXT,
    -- AdultStage (nectarSources in child table)
    adult_feeding_habit        VARCHAR(16),
    adult_ecological_role      TEXT,
    adult_lifespan             TEXT,
    description_preschool  TEXT NOT NULL,
    description_elementary TEXT NOT NULL,
    description_secondary  TEXT NOT NULL,
    description_university  TEXT NOT NULL
);
CREATE TABLE insect_life_stage_window (
    life_stage_id BIGINT      NOT NULL REFERENCES insect_life_stage(id),
    ordinal       INTEGER     NOT NULL,
    onset         VARCHAR(10) NOT NULL,                 -- MonthDay --MM-DD
    peak          VARCHAR(10),                          -- nullable
    tail          VARCHAR(10) NOT NULL,
    cohort_label  TEXT,
    PRIMARY KEY (life_stage_id, ordinal)
);
CREATE TABLE insect_life_stage_habitat_zone (
    life_stage_id BIGINT      NOT NULL REFERENCES insect_life_stage(id),
    zone          VARCHAR(24) NOT NULL,
    PRIMARY KEY (life_stage_id, zone)
);
CREATE TABLE insect_life_stage_habitat_layer (
    life_stage_id BIGINT      NOT NULL REFERENCES insect_life_stage(id),
    layer         VARCHAR(24) NOT NULL,
    PRIMARY KEY (life_stage_id, layer)
);
CREATE TABLE insect_life_stage_host_plant (
    life_stage_id BIGINT      NOT NULL REFERENCES insect_life_stage(id),
    ordinal       INTEGER     NOT NULL,
    plant_name    VARCHAR(96) NOT NULL,                 -- cross-domain plants slug, no FK
    PRIMARY KEY (life_stage_id, ordinal)
);
CREATE TABLE insect_life_stage_parasitoid_host (
    life_stage_id BIGINT      NOT NULL REFERENCES insect_life_stage(id),
    ordinal       INTEGER     NOT NULL,
    species_name  VARCHAR(96) NOT NULL,                 -- insect species slug, no FK (parity)
    PRIMARY KEY (life_stage_id, ordinal)
);
CREATE TABLE insect_life_stage_nectar_source (
    life_stage_id BIGINT      NOT NULL REFERENCES insect_life_stage(id),
    ordinal       INTEGER     NOT NULL,
    plant_name    VARCHAR(96) NOT NULL,                 -- cross-domain plants slug, no FK
    PRIMARY KEY (life_stage_id, ordinal)
);

-- ── Cross-domain catalog search: token view + trigram indexes ──────────────────────────────
CREATE OR REPLACE VIEW insect_catalog_token AS
      SELECT o.name AS token, o.name AS slug, true  AS is_slug, 'insects' AS domain, 'insect-order'   AS entity_type FROM insect_order o
UNION ALL SELECT o.taxonomic_order, o.name, false, 'insects', 'insect-order'   FROM insect_order o
UNION ALL SELECT cn.label, o.name, false, 'insects', 'insect-order'   FROM insect_order_common_name  cn JOIN insect_order  o ON cn.order_id  = o.id
UNION ALL SELECT f.name, f.name, true,  'insects', 'insect-family'  FROM insect_family f
UNION ALL SELECT f.taxonomic_family, f.name, false, 'insects', 'insect-family'  FROM insect_family f
UNION ALL SELECT cn.label, f.name, false, 'insects', 'insect-family'  FROM insect_family_common_name cn JOIN insect_family f ON cn.family_id = f.id
UNION ALL SELECT g.name, g.name, true,  'insects', 'insect-genus'   FROM insect_genus g
UNION ALL SELECT g.taxonomic_genus, g.name, false, 'insects', 'insect-genus'   FROM insect_genus g
UNION ALL SELECT cn.label, g.name, false, 'insects', 'insect-genus'   FROM insect_genus_common_name  cn JOIN insect_genus  g ON cn.genus_id  = g.id
UNION ALL SELECT s.name, s.name, true,  'insects', 'insect-species' FROM insect_species s
UNION ALL SELECT s.epithet, s.name, false, 'insects', 'insect-species' FROM insect_species s
UNION ALL SELECT cn.label, s.name, false, 'insects', 'insect-species' FROM insect_species_common_name cn JOIN insect_species s ON cn.species_id = s.id;

-- Operator class is schema-qualified (public.gin_trgm_ops) rather than relying on search_path:
-- InsectsSchemaDriftIT applies this DDL with search_path restricted to an isolated scratch
-- schema, and pg_trgm (installed once, database-wide) lives in "public".
CREATE INDEX IF NOT EXISTS insect_order_name_trgm   ON insect_order   USING gin (lower(name) public.gin_trgm_ops);
CREATE INDEX IF NOT EXISTS insect_family_name_trgm  ON insect_family  USING gin (lower(name) public.gin_trgm_ops);
CREATE INDEX IF NOT EXISTS insect_genus_name_trgm   ON insect_genus   USING gin (lower(name) public.gin_trgm_ops);
CREATE INDEX IF NOT EXISTS insect_species_name_trgm ON insect_species USING gin (lower(name) public.gin_trgm_ops);
CREATE INDEX IF NOT EXISTS insect_species_cn_trgm   ON insect_species_common_name USING gin (lower(label) public.gin_trgm_ops);
