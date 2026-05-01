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
 * The set of recognised domains is closed at the type level — {@code DomainId}
 * is a {@code sealed} interface and the permitted subtypes are the only
 * allowed instances. The compiler refuses construction of an unrecognised
 * domain; pattern-matched {@code switch} expressions over a {@code DomainId}
 * are exhaustive without a default branch. This makes the
 * {@code source_domain} metric tag's cardinality bounded as a property of
 * the type system rather than a convention.
 * <p>
 * The sealed shape also leaves room for each subtype to grow domain-specific
 * context — a human {@code displayName()} for the back-references panel, a
 * Durrell-style description for tooltip rendering, or any other cross-cutting
 * metadata — without a parallel sidecar map indexed by slug. Today subtypes
 * carry only the slug; new methods are added on the interface (with a default
 * where reasonable, abstract where the answer must vary per domain) when a
 * concrete consumer needs them. If subtype bodies grow large enough to
 * outgrow this file, extracting them to top-level files is a mechanical
 * refactor — a {@code permits} clause is reintroduced and call sites adjust.
 * <p>
 * Per the kernel's "no domain knowledge" rule the subtypes do not import
 * anything from the domain modules they name — they are slug-bearing markers
 * declared inside the catalog kernel itself. Adding a new domain is a
 * deliberate code change: a new nested {@code record} below. The friction is
 * appropriate to the work of standing up a real domain.
 *
 * <h2>Construction</h2>
 * Consumers construct subtypes directly: {@code new DomainId.Plants()},
 * {@code new DomainId.Chemistry()}, {@code new DomainId.Insects()} — or
 * import the nested type for a shorter call site. The {@link #of(String)}
 * factory parses a slug back into the corresponding subtype and throws
 * {@link IllegalArgumentException} on an unknown slug — the correct response
 * to a closed-enum deserialization miss.
 *
 * <h2>Invariants</h2>
 * Subtype values are hard-coded; {@link #invariants()} is a default no-op.
 * Validation happens at construction time, not via the observation pipeline.
 */
public sealed interface DomainId extends ValueObject {

    /**
     * The kebab-case slug identifying this domain.
     */
    String value();

    /**
     * Parse a slug back into its corresponding {@link DomainId} subtype.
     *
     * @throws IllegalArgumentException if the slug is not one of the
     *                                  permitted domain values
     */
    static DomainId of(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Unknown domain: null");
        }
        return switch (value) {
            case "plants"    -> new Plants();
            case "chemistry" -> new Chemistry();
            case "insects"   -> new Insects();
            default -> throw new IllegalArgumentException("Unknown domain: " + value);
        };
    }

    @Override
    default Consumer<? extends Constraints> invariants() {
        return i -> {};
    }

    /**
     * The plants domain — see {@code domains/plants/}. Hosts {@code Plant},
     * {@code Cultivar}, {@code SeedLineage}, {@code PhytochemicalConstituent},
     * and the management sub-context.
     */
    record Plants() implements DomainId {
        @Override public String value() { return "plants"; }
        @Override public String toString() { return value(); }
    }

    /**
     * The chemistry domain — see {@code domains/chemistry/}. Hosts
     * {@code Compound}, {@code Reaction}, {@code Element}, {@code Product},
     * and the depiction sub-context. Catalog back-references to compounds power
     * the "Found in:" panel on the {@code CompoundDetails} page (M8).
     */
    record Chemistry() implements DomainId {
        @Override public String value() { return "chemistry"; }
        @Override public String toString() { return value(); }
    }

    /**
     * The insects domain — see {@code domains/insects/}. Inverse references
     * to compounds (e.g. pipevine swallowtail sequestering aristolochic acid)
     * compose with the plants references on a chemistry detail page.
     */
    record Insects() implements DomainId {
        @Override public String value() { return "insects"; }
        @Override public String toString() { return value(); }
    }
}
