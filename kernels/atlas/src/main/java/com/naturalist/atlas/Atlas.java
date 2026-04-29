package com.naturalist.atlas;

import com.naturalist.ddd.EntityName;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Public-facing query interface for the atlas — the cross-domain navigation
 * surface the management console renders against.
 * <p>
 * The atlas is a routing table, not a knowledge graph: it does not ingest
 * {@code (source, target, kind)} triples at startup. It answers two
 * complementary questions:
 * <ul>
 *   <li><b>Forward</b> ({@link #resolveAlias}) — "given this surface form in
 *       human prose, what entity does it point at?" Resolved from a
 *       precomputed map of aliases registered by {@link AtlasContribution}s.</li>
 *   <li><b>Inverse</b> ({@link #domainsReferencing}, {@link #findReferencesTo})
 *       — "which entities, in which domains, reference this entity?"
 *       Routed to live {@link EntityReferences} providers indexed by
 *       reference type; results are not cached at the atlas layer.</li>
 * </ul>
 *
 * <h2>Construction</h2>
 * Apps obtain an {@code Atlas} via {@link AtlasAssembly#from} in their
 * composition root, passing the {@link AtlasContribution}s of the domains
 * they include. The kernel ships one default in-memory implementation
 * ({@link DefaultAtlas}); apps with bespoke needs (e.g. a remote registry)
 * may implement {@code Atlas} directly.
 *
 * <h2>Thread-safety</h2>
 * The default implementation is immutable post-assembly and safe for
 * concurrent reads. Custom implementations are expected to honour the same
 * contract — {@code resolveAlias} is a hot path on the description renderer
 * and must not block.
 */
public interface Atlas {

    /**
     * Resolve a surface form to the typed {@link EntityRef} a domain
     * registered for it.
     * <p>
     * <b>Matching rules:</b>
     * <ul>
     *   <li>Exact-string match against the registered alias. No prefix,
     *       suffix, or substring search; the renderer is responsible for
     *       carving prose into candidate spans before calling this method.</li>
     *   <li>Case-sensitive — {@code "Aristolochia"} (the genus) and
     *       {@code "aristolochia"} (a wrong-case form, or a stem inside
     *       {@code "aristolochic acid"}) are distinct surface forms.
     *       Genus capitalisation is meaningful in living taxonomy and the
     *       atlas preserves that.</li>
     *   <li>Whitespace and punctuation are not normalised. {@code "A. californica"}
     *       and {@code "A.californica"} are different surface forms; if both
     *       should resolve, the contribution must register both.</li>
     *   <li>{@code null} returns {@link Optional#empty()} rather than throwing,
     *       so callers can pass arbitrary regex match groups without a
     *       null-check.</li>
     * </ul>
     *
     * @param text the literal surface form to look up; may be {@code null}
     * @return the registered {@link EntityRef}, or {@link Optional#empty()}
     *         if no contribution registered this exact form
     */
    Optional<EntityRef> resolveAlias(String text);

    /**
     * The set of domains that have registered an {@link EntityReferences}
     * provider for the given {@link EntityName} subclass — the coarse routing
     * answer used when the console wants to know, before doing any fan-out,
     * whether anyone holds back-references at all.
     * <p>
     * Matching is by exact class equality; a provider declaring
     * {@code referenceType() == CompoundName.class} is included for
     * {@code domainsReferencing(CompoundName.class)} but not for any
     * supertype query. {@code null} returns an empty set rather than
     * throwing, mirroring {@link #resolveAlias}.
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
     *   <li>Domains whose providers all return empty streams do not appear in
     *       the map. The contract distinguishes "no references" (empty map,
     *       suppressed panel per M8) from "no provider" — both yield no
     *       entries, which is the correct rendering.</li>
     *   <li>{@link EntityRef}s within each domain's list are returned in the
     *       order their provider yielded them; if a domain registers more
     *       than one provider, the lists are concatenated in registration
     *       order.</li>
     *   <li>A provider that throws is observed (M9) and excluded from the
     *       result; peer providers' results are returned regardless. Page
     *       renders gracefully degrade rather than fail.</li>
     *   <li>{@code null} target returns an empty map.</li>
     * </ul>
     *
     * @param target the entity whose inbound references to find; may be
     *               {@code null}
     * @return back-references grouped by domain; never null, possibly empty
     */
    Map<DomainId, List<EntityRef>> findReferencesTo(EntityName target);
}
