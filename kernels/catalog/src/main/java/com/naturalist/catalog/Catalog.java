package com.naturalist.catalog;

import com.naturalist.ddd.EntityName;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Public-facing query interface for the catalog — the cross-domain
 * navigation surface the management console renders against.
 * <p>
 * The catalog answers two complementary questions:
 * <ul>
 *   <li><b>Search</b> ({@link #search}) — "what does the catalog know
 *       about this term?" Many possible answers, including none. Empty is
 *       a first-class state and the catalog's growth signal.</li>
 *   <li><b>Inverse</b> ({@link #domainsReferencing}, {@link #findReferencesTo})
 *       — "which entities, in which domains, reference this entity?"
 *       Routed to live {@link EntityReferences} providers indexed by
 *       reference type; results are not cached at the catalog layer.</li>
 * </ul>
 *
 * <h2>Construction</h2>
 * Apps obtain a {@code Catalog} via an adapter-specific assembly factory in
 * their composition root, passing the {@link CatalogContribution}s of the
 * domains they include. The kernel ships an in-memory adapter in
 * {@code kernels/catalog-inmem} (the {@code CatalogAssembly} factory there);
 * apps with bespoke needs (e.g. a Lucene-backed registry) may implement
 * {@code Catalog} directly in a sibling adapter module without disturbing
 * contributing domains or consumers.
 *
 * <h2>Thread-safety</h2>
 * The in-memory adapter is immutable post-assembly and safe for concurrent
 * reads. Other adapters define their own thread-safety contracts.
 */
public interface Catalog {

    /**
     * Run a search against the assembled token index.
     * <p>
     * <b>Matching rules:</b>
     * <ul>
     *   <li>Case-insensitive throughout — {@code "ARISTOLOCHIA"},
     *       {@code "Aristolochia"}, and {@code "aristolochia"} are
     *       equivalent. Genus capitalisation is a presentation concern
     *       (the renderer italicises {@code Genus species} correctly);
     *       the search is liberal in what it accepts.</li>
     *   <li>The query is split on whitespace and ASCII punctuation; each
     *       token is looked up independently and the results are merged.
     *       {@code "A. californica"} therefore matches against both
     *       {@code "a"} and {@code "californica"}.</li>
     *   <li>An exact match against an entity's slug yields a hit with
     *       {@link MatchKind#EXACT_SLUG}; an exact match against a
     *       non-slug token yields {@link MatchKind#EXACT_TOKEN}; a
     *       prefix match against any token yields
     *       {@link MatchKind#PREFIX}.</li>
     *   <li>Multiple tokens may match the same entity; the result
     *       deduplicates per {@code (EntityRef, MatchKind)} so each
     *       entity surfaces at most once per kind.</li>
     *   <li>Hit ordering inside the returned {@link SearchResults} is by
     *       {@link MatchKind#ordinal()} then by the slug's natural
     *       ordering, for deterministic rendering.</li>
     *   <li>{@code null} or blank input returns an empty
     *       {@link SearchResults} without firing an
     *       {@link UnresolvedSearchObservation} — the empty input is
     *       not a search.</li>
     *   <li>A non-empty input that resolves to no hits returns an empty
     *       {@link SearchResults} <em>and</em> fires one
     *       {@link UnresolvedSearchObservation} so observers can record
     *       the catalog's growth signal.</li>
     * </ul>
     *
     * @param text the search input; may be {@code null}
     * @return the matching hits in the documented order, never null
     */
    SearchResults search(String text);

    /**
     * The set of domains that have registered an {@link EntityReferences}
     * provider for the given {@link EntityName} subclass — the coarse
     * routing answer used when the console wants to know, before doing any
     * fan-out, whether anyone holds back-references at all.
     * <p>
     * Matching is by exact class equality. {@code null} returns an empty
     * set rather than throwing.
     *
     * @param referenceType the target type to look up; may be {@code null}
     * @return the set of domains holding providers for this type, never null
     */
    Set<DomainId> domainsReferencing(Class<? extends EntityName> referenceType);

    /**
     * Fan out a back-reference query to every {@link EntityReferences}
     * provider whose {@code referenceType()} matches the runtime type of
     * {@code target}. Results are grouped by {@link DomainId} for the
     * console's "Found in:" panel rendering.
     * <p>
     * <b>Result shape:</b>
     * <ul>
     *   <li>Domains with no matching providers do not appear in the map.</li>
     *   <li>Domains whose providers all return empty streams do not appear
     *       in the map. The contract distinguishes "no references" (empty
     *       map, suppressed panel) from "no provider" — both yield no
     *       entries, which is the correct rendering.</li>
     *   <li>{@link EntityRef}s within each domain's list are returned in
     *       the order their provider yielded them; if a domain registers
     *       more than one provider, the lists are concatenated in
     *       registration order.</li>
     *   <li>A provider that throws is observed (M9) and excluded from the
     *       result; peer providers' results are returned regardless.</li>
     *   <li>{@code null} target returns an empty map.</li>
     * </ul>
     *
     * @param target the entity whose inbound references to find; may be
     *               {@code null}
     * @return back-references grouped by domain; never null, possibly empty
     */
    Map<DomainId, List<EntityRef>> findReferencesTo(EntityName target);
}
