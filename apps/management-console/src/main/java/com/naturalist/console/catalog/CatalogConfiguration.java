package com.naturalist.console.catalog;

import com.naturalist.catalog.Catalog;
import com.naturalist.catalog.CatalogContribution;
import com.naturalist.catalog.DomainId;
import com.naturalist.catalog.EntityReferences;
import com.naturalist.catalog.inmem.CatalogAssembly;
import com.naturalist.resilience.Resilience;
import com.naturalist.spring.DomainServiceScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.List;

/**
 * Composition root for the assembled {@link Catalog} consumed by both the
 * cross-domain search surface ({@code SearchController}) and per-domain
 * back-reference panels ({@code ChemistryController} for M8).
 * <p>
 * The catalog is wired here — and not inside any per-domain controller —
 * because each domain's contribution and inverse provider crosses module
 * boundaries that no single domain can own. This {@code @Configuration}
 * is the single fan-in point, kept minimal so a future Lucene-backed
 * adapter swap is one factory call.
 *
 * <h2>How wiring works</h2>
 * {@link DomainServiceScan} discovers every {@code @DomainService}-annotated
 * class on the classpath under the pilot base packages
 * ({@code com.naturalist.catalog}, {@code com.naturalist.plants}) and
 * registers it as a Spring bean. Spring then collects every {@link DomainId},
 * {@link CatalogContribution}, and {@link EntityReferences} bean and feeds
 * them into the assembly factory below. Adding a new contribution becomes
 * a single annotation on a class — no edit to this file.
 */
@Configuration
@Import(DomainServiceScan.class)
public class CatalogConfiguration {

    @Bean
    Catalog catalog(List<DomainId> domains,
                    List<CatalogContribution> contributions,
                    List<EntityReferences<?>> providers,
                    Resilience resilience) {
        return CatalogAssembly.from(domains, contributions, providers, resilience);
    }
}
