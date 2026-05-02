package com.naturalist.plants;

import com.naturalist.catalog.DomainId;
import com.naturalist.infrastructure.DomainService;

/**
 * The plants domain — see {@code domains/plants/}. Hosts {@code Plant},
 * {@code Cultivar}, {@code SeedLineage}, {@code PhytochemicalConstituent}, and
 * the management sub-context.
 * <p>
 * Owned by {@code plants-api} per the plan's "open {@link DomainId}" decision:
 * each domain ships its own slug-bearing subtype, the kernel knows the names
 * of no domains, and the catalog assembly enforces slug uniqueness across
 * registered subtypes at startup.
 */
@DomainService
public record PlantsDomain() implements DomainId {

    @Override public String value() { return "plants"; }

    @Override public String toString() { return value(); }
}
