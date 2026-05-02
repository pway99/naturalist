package com.naturalist.chemistry;

import com.naturalist.catalog.DomainId;

/**
 * The chemistry domain — see {@code domains/chemistry/}. Hosts
 * {@code Compound}, {@code Reaction}, {@code Element}, {@code Product}, and
 * the depiction sub-context. Catalog back-references to compounds power the
 * "Found in:" panel on the {@code CompoundDetails} page.
 * <p>
 * Owned by {@code chemistry-api} per the plan's "open {@link DomainId}"
 * decision: each domain ships its own slug-bearing subtype, the kernel knows
 * the names of no domains, and the catalog assembly enforces slug uniqueness
 * across registered subtypes at startup.
 */
public record ChemistryDomain() implements DomainId {

    @Override public String value() { return "chemistry"; }

    @Override public String toString() { return value(); }
}
