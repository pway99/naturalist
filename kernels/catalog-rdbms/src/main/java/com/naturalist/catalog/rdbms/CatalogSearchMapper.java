package com.naturalist.catalog.rdbms;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
interface CatalogSearchMapper {

    @Select("""
        SELECT slug, domain, entity_type, token AS matched_token,
               CASE
                 WHEN lower(token) = lower(#{q}) AND is_slug THEN 'EXACT_SLUG'
                 WHEN lower(token) = lower(#{q})             THEN 'EXACT_TOKEN'
                 WHEN lower(token) LIKE lower(#{q}) || '%'   THEN 'PREFIX'
                 ELSE 'FUZZY'
               END AS kind,
               similarity(lower(token), lower(#{q})) AS sim
        FROM catalog_search_token
        WHERE token ILIKE '%' || #{q} || '%'
        """)
    List<CatalogTokenRow> search(@Param("q") String q);

    @Select("""
        SELECT slug, domain, entity_type
        FROM catalog_search_token
        WHERE is_slug AND lower(slug) = lower(#{slug})
        LIMIT 1
        """)
    CatalogTokenRow findBySlug(@Param("slug") String slug);

    @Select("SELECT DISTINCT domain, entity_type FROM catalog_search_token")
    List<CatalogTokenRow> distinctDomainTypes();
}
