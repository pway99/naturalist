package com.naturalist.atlas;

import com.naturalist.ddd.EntityName;

import java.util.stream.Stream;

/**
 * Inverse-direction SPI — a participating domain's declaration that it can
 * answer "which of my entities reference this foreign {@link EntityName}?"
 * <p>
 * The forward SPI ({@link AtlasContribution}) lets the atlas resolve a surface
 * form found in prose to the entity it points at; this SPI lets the atlas
 * fan out the inverse question to every domain that knows how to look it up.
 * It is what the chemistry detail page calls when rendering the
 * "Found in: california-pipevine (plants), pipevine-swallowtail (insects)"
 * panel for a compound (M8): the chemistry domain consumes, and each domain
 * holding outbound references contributes a provider.
 *
 * <h2>Routing, not graph</h2>
 * Per the plan's "Registry shape: routing, not graph" decision, the atlas does
 * not ingest reference triples at startup. It stores only that a given
 * {@code (domain, referenceType)} pair has a provider; the actual lookup runs
 * live against the domain's repository every time
 * {@link Atlas#findReferencesTo(EntityName)} is called. This keeps reference
 * data freshness automatic and removes any startup index to maintain.
 *
 * <h2>Placement</h2>
 * Implementations live in {@code <domain>-core} (where repository access is
 * available); the {@code -api} module never depends on atlas. The first real
 * provider ships in M5 ({@code plants-core} answering for
 * {@code CompoundName}).
 *
 * <h2>Multiple providers per domain</h2>
 * A domain may register more than one provider — for example, plants might
 * answer for both {@code CompoundName} and a future {@code SoilName}. Each
 * provider is keyed by its {@link #referenceType()}; one domain may also
 * register two providers for the same type if its sub-contexts each own a
 * slice of the answer.
 *
 * <h2>Exception policy</h2>
 * A provider that throws during {@link #referencesTo(EntityName)} is observed
 * (M9) and dropped from the fan-out result; peer providers' results are
 * returned regardless. Providers should not rely on exceptions to signal
 * "no references" — return an empty stream instead.
 *
 * @param <T> the {@link EntityName} subclass this provider answers for
 */
public interface EntityReferences<T extends EntityName> {

    /**
     * The contributing domain. Used as the grouping key in
     * {@link Atlas#findReferencesTo(EntityName)} and the {@code source_domain}
     * tag on observability metrics.
     */
    DomainId domain();

    /**
     * The {@link EntityName} subclass this provider can resolve back-references
     * for. The atlas indexes providers by this class at assembly so that
     * routing ({@link Atlas#domainsReferencing(Class)}) is a constant-time
     * lookup rather than a linear scan.
     */
    Class<T> referenceType();

    /**
     * Enumerate the references this domain holds to {@code target}. Called
     * live on every fan-out; the atlas does not cache results. Implementations
     * are responsible for any per-domain caching they consider appropriate
     * (typically none, given each query is a single repository read).
     * <p>
     * The returned stream may be empty when the target is unknown to this
     * domain — that is the normal case for most compounds on most pages, not
     * an error.
     */
    Stream<EntityRef> referencesTo(T target);
}
