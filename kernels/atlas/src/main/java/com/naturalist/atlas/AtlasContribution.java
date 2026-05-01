package com.naturalist.atlas;

import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Search-direction SPI — a participating domain's declaration of the
 * entities it owns and the surface forms (tokens) under which each is
 * findable.
 * <p>
 * Each contribution names a single {@link DomainId} and emits a
 * {@link Stream} of {@link SearchableEntity} records pairing a typed
 * {@link EntityRef} with the tokens that should index it. The
 * configured {@link Atlas} adapter collects every contribution at
 * assembly time and answers {@link Atlas#search(String)} from the
 * resulting index — for the in-memory adapter shipped in
 * {@code kernels/atlas-inmem}, that index is a token map built once
 * at assembly.
 *
 * <h2>Derive, do not register</h2>
 * The canonical implementation computes tokens live from the domain's
 * own entity data — slug, scientific binomial, genus, abbreviated
 * binomial, common names — rather than authoring a parallel
 * registration list. {@link #searchableEntities()} returns a fresh
 * stream on every call so a contribution backed by a mutable repository
 * can reflect newly added entities without re-registration.
 *
 * <h2>Token collisions are normal</h2>
 * Multiple entities indexing the same token (two {@code Trifolium}
 * species under the genus token) is not an error — under search it is
 * the expected behaviour. The reader types the ambiguous term, the
 * results page shows both species, the reader picks. Contributions
 * should emit every derivable token unconditionally; ambiguity
 * filtering is the search index's concern, not the contribution's.
 *
 * <h2>Placement</h2>
 * Implementations live in {@code <domain>-core} (where repository
 * access is available); the {@code -api} module never depends on
 * atlas.
 *
 * <h2>Empty contributions</h2>
 * A contribution that returns an empty stream is legal — useful when a
 * domain wants to declare its presence to the atlas without (yet)
 * having any entities to surface.
 */
public interface AtlasContribution {

    /**
     * The contributing domain. Every entity emitted by this contribution
     * is implicitly attributed to this {@link DomainId}.
     */
    DomainId domain();

    /**
     * The entities this contribution wishes to make searchable, computed
     * live. Called once per atlas assembly; not cached by the kernel.
     * Consumers that need to iterate twice should collect the stream
     * themselves.
     */
    Stream<SearchableEntity> searchableEntities();

    /**
     * A single (entity, tokens) pair. {@code target} is the typed
     * reference a search hit will carry; {@code tokens} are the surface
     * forms under which that entity should be findable.
     * <p>
     * Tokens are not pre-normalised by the contribution — the kernel
     * lowercases and tokenises them at index time, applying the same
     * tokenisation to incoming queries for symmetric matching.
     * Contributions should emit human-readable forms ({@code "Aristolochia
     * californica"}, {@code "California Dutchman's pipe"}) rather than
     * pre-tokenised stems.
     *
     * @param target the typed reference the search hit will point at
     * @param tokens the surface forms under which {@code target} is findable
     */
    record SearchableEntity(EntityRef target, Stream<String> tokens) implements ValueObject {

        @Override
        public Consumer<? extends Constraints> invariants() {
            return i -> i
                    .valueObject(target, "target")
                    .notNull(tokens, "tokens");
        }
    }
}
