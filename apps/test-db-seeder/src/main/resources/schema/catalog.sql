-- App-level cross-domain catalog search fan-in view. Unions every participating domain's own
-- <domain>_catalog_token view (see e.g. domains/insects/insects-repository-rdbms/src/main/resources/
-- schema/insects.sql). Applied last by CatalogSeeding, after every domain has been (re)seeded, so it
-- always reflects the current set of per-domain views. Add a new UNION ALL branch here as each
-- additional domain grows its own <domain>_catalog_token view.
CREATE EXTENSION IF NOT EXISTS pg_trgm;

DROP VIEW IF EXISTS catalog_search_token;
CREATE VIEW catalog_search_token AS
          SELECT token, slug, is_slug, domain, entity_type FROM insect_catalog_token
UNION ALL SELECT token, slug, is_slug, domain, entity_type FROM plant_catalog_token
UNION ALL SELECT token, slug, is_slug, domain, entity_type FROM chemistry_catalog_token;
