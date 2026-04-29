package com.naturalist.atlas;

import java.util.Optional;

/**
 * Public-facing query interface for the atlas — the cross-domain navigation
 * surface the management console renders against.
 * <p>
 * The atlas is a routing table, not a knowledge graph: it does not ingest
 * {@code (source, target, kind)} triples at startup. Today it answers a single
 * question — "given this surface form in human prose, what entity does it
 * point at?" — and resolves it from a precomputed map of aliases contributed
 * by participating domains. The inverse direction
 * ({@code findReferencesTo}, {@code domainsReferencing}) lands in M3.
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
}
