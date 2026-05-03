package com.naturalist.catalog;

import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;

/**
 * Stable, low-cardinality identifier for a participating domain. Used as the
 * {@code domain} component of every {@link EntityRef}, as the
 * {@code source_domain} tag on catalog observability metrics, and as the
 * grouping key for fan-out queries on the inverse SPI.
 * <p>
 * The interface is open: each domain ships its own {@code DomainId} subtype in
 * its {@code *-api} module (e.g. {@code PlantsDomain} in
 * {@code domains/plants/plants-api}). The kernel knows the names of no domains.
 * Adding a domain is a deliberate code change in that domain's api module — a
 * new record implementing this interface and a {@code @DomainService} marker
 * (or an explicit composition-root registration) — and never a kernel edit.
 *
 * <h2>Bounded cardinality</h2>
 * The {@code source_domain} metric tag's cardinality is bounded by what each
 * composition root registers, not by the type system. Catalog assemblies
 * enforce slug uniqueness at startup: registering two distinct {@code DomainId}
 * instances that share a {@link #value()} fails fast with an
 * {@link IllegalArgumentException}. That preserves the bounded-cardinality
 * invariant the previous sealed shape gave for free, by a different mechanism.
 *
 * <h2>Construction</h2>
 * Domain api modules contribute concrete subtypes. Consumers construct them
 * directly: {@code new PlantsDomain()}, {@code new ChemistryDomain()},
 * {@code new InsectsDomain()}.
 *
 * <h2>Invariants</h2>
 * Subtype values are hard-coded slugs; {@link #invariants()} is a default
 * no-op. Validation happens at construction time, not via the observation
 * pipeline.
 */
public interface DomainId extends ValueObject {

    /**
     * The kebab-case slug identifying this domain. Must be unique across all
     * {@code DomainId} instances registered with a single catalog assembly.
     */
    String value();

    @Override
    default Consumer<? extends Constraints> invariants() {
        return i -> {
        };
    }
}
