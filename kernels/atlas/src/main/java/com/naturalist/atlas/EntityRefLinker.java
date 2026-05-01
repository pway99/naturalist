package com.naturalist.atlas;

/**
 * Per-domain console contribution for resolving an {@link EntityRef} to the
 * URL of its detail page. The third axis of the atlas's contribution model:
 * <ul>
 *   <li>{@link AtlasContribution} — search direction (forward).</li>
 *   <li>{@link EntityReferences} — inverse direction (back-refs).</li>
 *   <li>{@link EntityRefLinker} — rendering direction (this interface).</li>
 * </ul>
 * Each console module that owns a set of detail-page routes ships exactly one
 * implementation declaring which {@code EntityName} subclasses it can link
 * and how. Consumers (the chemistry detail page's "Found in" panel, the
 * search results page) ask a single composite linker and never reason about
 * which domain owns a given ref.
 *
 * <h2>Return contract</h2>
 * Implementations return {@code null} for any {@link EntityRef} whose
 * {@code name()} type is not theirs to handle. Returning a non-null URL is a
 * claim that the consumer can navigate there — implementations should not
 * synthesize URLs whose controller routes do not exist. The composite walks
 * registered linkers in registration order and returns the first non-null
 * answer; the consumer treats a {@code null} composite result as "no link"
 * (the same graceful-degradation path back-references already takes for
 * unrenderable refs).
 */
public interface EntityRefLinker {

    /**
     * Resolve {@code ref} to a console-relative URL for its detail page,
     * or {@code null} if this linker does not own the ref's name type.
     */
    String linkFor(EntityRef ref);
}
