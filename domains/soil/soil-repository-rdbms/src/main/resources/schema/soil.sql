-- Soil domain schema. Cross-entity references are SOFT (the in-memory TestEntitySources declare no
-- foreign-key constraints, and the contracts insert observations against random/non-existent analyses):
-- soil_profile_name and lab_analysis_id are plain columns with NO FK. The logical grain of each
-- observation family is enforced by a composite UNIQUE. Measurements store as unconstrained NUMERIC so
-- a BigDecimal round-trips at its exact scale.

DROP TABLE IF EXISTS soil_physical_characteristics;
DROP TABLE IF EXISTS reported_recommendation;
DROP TABLE IF EXISTS reported_optimum;
DROP TABLE IF EXISTS nutrient_reading;
DROP TABLE IF EXISTS lab_analysis_info;
DROP TABLE IF EXISTS soil_profile_info;

CREATE TABLE soil_profile_info (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name          VARCHAR(64) NOT NULL UNIQUE,
    zone_name     VARCHAR(64) NOT NULL,        -- zone slug, no FK
    sub_zone_name VARCHAR(64)                  -- nullable zone slug, no FK
);

CREATE TABLE lab_analysis_info (
    id                          UUID PRIMARY KEY,
    soil_profile_name           VARCHAR(64) NOT NULL,   -- soft ref to soil_profile_info.name, no FK
    crop_type                   VARCHAR(48) NOT NULL,   -- garden CropTypeName slug, no FK
    sample_date                 DATE        NOT NULL,
    lab_id                      TEXT        NOT NULL,
    lab_sample_id               TEXT        NOT NULL,
    sample_depth_inches         NUMERIC,                -- nullable DepthInches
    notes                       TEXT,
    -- SamplingProtocol (nullable group: present iff sampling_tool non-null)
    sampling_subsample_count    INTEGER,
    sampling_tool               VARCHAR(16),
    sampling_compositing_method VARCHAR(16)
);

CREATE TABLE nutrient_reading (
    id              UUID PRIMARY KEY,
    nutrient_name   VARCHAR(48) NOT NULL,      -- NutrientName slug, no FK (no Nutrient entity)
    lab_analysis_id UUID        NOT NULL,      -- soft ref to lab_analysis_info.id, no FK
    value           NUMERIC     NOT NULL,
    unit            VARCHAR(24) NOT NULL,
    UNIQUE (nutrient_name, lab_analysis_id)
);

CREATE TABLE reported_optimum (
    id              UUID PRIMARY KEY,
    nutrient_name   VARCHAR(48) NOT NULL,
    lab_analysis_id UUID        NOT NULL,      -- soft ref, no FK
    -- OptimumRange (sealed, flattened): CLOSED|UPPER_BOUNDED|LOWER_BOUNDED|NOT_APPLICABLE
    range_shape     VARCHAR(16) NOT NULL,
    range_min       NUMERIC,                   -- set for CLOSED, LOWER_BOUNDED
    range_max       NUMERIC,                   -- set for CLOSED, UPPER_BOUNDED
    unit            VARCHAR(24) NOT NULL,
    UNIQUE (nutrient_name, lab_analysis_id)
);

CREATE TABLE reported_recommendation (
    id              UUID PRIMARY KEY,
    input_name      VARCHAR(48) NOT NULL,      -- RecommendedInputName slug, no FK
    lab_analysis_id UUID        NOT NULL,      -- soft ref, no FK
    -- RecommendedAmount (sealed, flattened): QUANTITY|NONE|BELOW_DETECTION_LIMIT
    amount_kind     VARCHAR(24) NOT NULL,
    amount_value    NUMERIC,                   -- Quantity.value (incl. 0) or BelowDetectionLimit.limit, null for NONE
    unit            VARCHAR(24) NOT NULL,
    route           VARCHAR(8),                -- nullable ApplicationRoute (SOIL|FOLIAR)
    UNIQUE (input_name, lab_analysis_id)
);

CREATE TABLE soil_physical_characteristics (
    id                                UUID PRIMARY KEY,
    lab_analysis_id                   UUID    NOT NULL UNIQUE,   -- soft ref, no FK (1:1 per analysis)
    cec_meq_per_100g                  NUMERIC NOT NULL,
    ph                                NUMERIC NOT NULL,
    ec_ds_per_meter                   NUMERIC NOT NULL,
    sar                               NUMERIC NOT NULL,
    limestone_pct                     NUMERIC NOT NULL,
    saturation_pct                    NUMERIC NOT NULL,
    -- CationBaseSaturation
    cbs_calcium_pct                   NUMERIC NOT NULL,
    cbs_magnesium_pct                 NUMERIC NOT NULL,
    cbs_potassium_pct                 NUMERIC NOT NULL,
    cbs_sodium_pct                    NUMERIC NOT NULL,
    cbs_hydrogen_pct                  NUMERIC NOT NULL,
    cbs_hydrogen_below_detection_limit BOOLEAN NOT NULL
);
