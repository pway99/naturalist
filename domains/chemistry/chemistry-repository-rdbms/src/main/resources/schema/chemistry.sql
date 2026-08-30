CREATE EXTENSION IF NOT EXISTS pg_trgm;

DROP TABLE IF EXISTS product_property;
DROP TABLE IF EXISTS product_compound;
DROP TABLE IF EXISTS product;
DROP TABLE IF EXISTS compound_depiction;
DROP TABLE IF EXISTS compound_property;
DROP TABLE IF EXISTS compound_constituent_element;
DROP TABLE IF EXISTS compound_functional_role;
DROP TABLE IF EXISTS compound CASCADE;
DROP TABLE IF EXISTS element CASCADE;

CREATE TABLE element (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name          VARCHAR(64) NOT NULL UNIQUE,
    symbol        VARCHAR(8)  NOT NULL,
    atomic_weight NUMERIC     NOT NULL,
    ionic_form    VARCHAR(64),
    ionic_charge  INTEGER     NOT NULL
);

CREATE TABLE compound (
    id                                       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name                                     VARCHAR(96)  NOT NULL UNIQUE,
    common_name                              VARCHAR(128) NOT NULL UNIQUE,
    omri_listed                              BOOLEAN      NOT NULL,
    cdfa_registered                          BOOLEAN      NOT NULL,
    formula                                  VARCHAR(64)  NOT NULL,
    molecular_weight                         NUMERIC,
    ph_character                             VARCHAR(32)  NOT NULL,
    chemical_nature                          VARCHAR(32)  NOT NULL,
    physical_form                            VARCHAR(32)  NOT NULL,
    structural_type                          VARCHAR(48)  NOT NULL,
    solubility_grams_per_liter_at_20c        NUMERIC      NOT NULL,
    solubility_category                      VARCHAR(32)  NOT NULL,
    solubility_ec_contribution_factor        NUMERIC      NOT NULL,
    solubility_notes                         TEXT         NOT NULL,
    bioavailability_primary_pathway          VARCHAR(32)  NOT NULL,
    bioavailability_relative_absorption_rate NUMERIC      NOT NULL,
    bioavailability_cuticular                BOOLEAN      NOT NULL,
    bioavailability_stomatal                 BOOLEAN      NOT NULL,
    bioavailability_chelate_enhanced         BOOLEAN      NOT NULL,
    bioavailability_mechanism                TEXT         NOT NULL,
    volatilization_min_effective_temp_f      NUMERIC,
    volatilization_max_safe_temp_f           NUMERIC,
    volatilization_optimal_temp_f            NUMERIC,
    volatilization_vapor_pressure_at_20c     NUMERIC,
    volatilization_efficacy_notes            TEXT,
    volatilization_safety_notes              TEXT,
    safety_hazard_level                      VARCHAR(32),
    safety_max_safe_concentration_ppm        NUMERIC,
    safety_min_application_temp_f            NUMERIC,
    safety_max_application_temp_f            NUMERIC,
    safety_requires_protective_equipment     BOOLEAN,
    safety_hazardous_to_bees_when_wet        BOOLEAN,
    safety_requires_evening_application      BOOLEAN,
    safety_application_constraints           TEXT
);

CREATE TABLE compound_functional_role (
    compound_id     BIGINT      NOT NULL REFERENCES compound(id),
    functional_role VARCHAR(48) NOT NULL,
    PRIMARY KEY (compound_id, functional_role)
);

CREATE TABLE compound_constituent_element (
    compound_id    BIGINT     NOT NULL REFERENCES compound(id),
    element_symbol VARCHAR(3) NOT NULL,
    PRIMARY KEY (compound_id, element_symbol)
);

CREATE TABLE compound_property (
    compound_id    BIGINT      NOT NULL REFERENCES compound(id),
    property_key   VARCHAR(64) NOT NULL,
    property_value TEXT        NOT NULL,
    PRIMARY KEY (compound_id, property_key)
);

CREATE TABLE compound_depiction (
    id          UUID PRIMARY KEY,
    compound_id BIGINT       NOT NULL REFERENCES compound(id),
    smiles      VARCHAR(512) NOT NULL,
    note        TEXT         NOT NULL
);

CREATE TABLE product (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name         VARCHAR(96)  NOT NULL UNIQUE,
    display_name VARCHAR(128) NOT NULL UNIQUE
);

CREATE TABLE product_compound (
    product_id  BIGINT NOT NULL REFERENCES product(id),
    compound_id BIGINT NOT NULL REFERENCES compound(id),
    PRIMARY KEY (product_id, compound_id)
);

CREATE TABLE product_property (
    product_id     BIGINT      NOT NULL REFERENCES product(id),
    property_key   VARCHAR(64) NOT NULL,
    property_value TEXT        NOT NULL,
    PRIMARY KEY (product_id, property_key)
);

-- Cross-domain catalog search fan-in contribution (see kernels/catalog-rdbms). Chemistry is
-- FLAT — no rank hierarchy, no common-name child tables — element and compound carry their
-- own tokens as direct columns, so this view needs no joins.
CREATE OR REPLACE VIEW chemistry_catalog_token AS
      SELECT name AS token, name AS slug, true AS is_slug, 'chemistry' AS domain, 'element' AS entity_type FROM element
UNION ALL SELECT symbol,      name, false, 'chemistry', 'element'  FROM element
UNION ALL SELECT name,        name, true,  'chemistry', 'compound' FROM compound
UNION ALL SELECT common_name, name, false, 'chemistry', 'compound' FROM compound;

CREATE INDEX IF NOT EXISTS element_name_trgm    ON element  USING gin (lower(name) public.gin_trgm_ops);
CREATE INDEX IF NOT EXISTS compound_name_trgm   ON compound USING gin (lower(name) public.gin_trgm_ops);
CREATE INDEX IF NOT EXISTS compound_common_trgm ON compound USING gin (lower(common_name) public.gin_trgm_ops);
