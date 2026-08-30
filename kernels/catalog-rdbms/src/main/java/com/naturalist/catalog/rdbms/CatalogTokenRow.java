package com.naturalist.catalog.rdbms;

/** Query projection from catalog_search_token. Not a persisted Dbo — read-only search output. */
final class CatalogTokenRow {
    String slug;
    String domain;
    String entityType;   // entity_type
    String matchedToken; // matched_token (null for findBySlug/distinct)
    String kind;          // EXACT_SLUG | EXACT_TOKEN | PREFIX | FUZZY (null for findBySlug/distinct)
    Double sim;           // trigram similarity (null for findBySlug/distinct)
}
