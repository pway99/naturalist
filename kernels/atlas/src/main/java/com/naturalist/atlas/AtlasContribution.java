package com.naturalist.atlas;

import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Forward-direction SPI — a participating domain's declaration of the surface
 * forms (substrings appearing in human prose, slugs, common names) under which
 * its own entities can be recognised by the atlas.
 * <p>
 * Each contribution names a single {@link DomainId} and emits a {@link Stream}
 * of {@link Alias} records pairing a literal surface form with the typed
 * {@link EntityRef} it should resolve to. The kernel's {@link DefaultAtlas}
 * collects every contribution at assembly time, indexes the aliases, and
 * answers {@link Atlas#resolveAlias(String)} from the resulting table.
 *
 * <h2>Derive, do not register</h2>
 * Per the plan's "Derive aliases, do not register" decision, the canonical
 * implementation is to compute aliases live from the domain's own entity data
 * — slug, scientific binomial, genus, abbreviated binomial — rather than to
 * maintain a parallel registration list. {@link #aliases()} returns a fresh
 * stream on every call so that a contribution backed by a mutable repository
 * can reflect newly added entities without needing to be re-registered.
 *
 * <h2>Placement</h2>
 * Implementations live in {@code <domain>-core} (where repository access is
 * available); the {@code -api} module never depends on atlas. The first real
 * producer ships in M4 ({@code plants-core}).
 *
 * <h2>Empty contributions</h2>
 * A contribution that returns an empty stream is legal — useful when a domain
 * wants to declare its presence to the atlas without (yet) having any entities
 * to surface. Tests should not assert non-emptiness as a general invariant.
 */
public interface AtlasContribution {

    /**
     * The contributing domain. Every alias emitted by this contribution is
     * implicitly attributed to this {@link DomainId}; callers may use it as
     * a tag for diagnostics or for grouping aliases when assembling per-app
     * catalogues.
     */
    DomainId domain();

    /**
     * The aliases this contribution wishes to register, computed live. Called
     * once per atlas assembly; not cached by the kernel. Consumers that need
     * to iterate twice should collect the stream themselves.
     */
    Stream<Alias> aliases();

    /**
     * A single (surface form, target) pair. The {@code surfaceForm} is the
     * literal text the atlas matches against (case-sensitive, no normalisation);
     * the {@code target} is the typed reference that text resolves to.
     * <p>
     * Both components are required. A blank surface form or null target is an
     * invariant violation, observable through the standard pipeline.
     *
     * @param surfaceForm the literal text to match
     * @param target      the typed reference the surface form resolves to
     */
    record Alias(String surfaceForm, EntityRef target) implements ValueObject {

        @Override
        public Consumer<? extends Constraints> invariants() {
            return i -> i
                    .notBlank(this, Alias::surfaceForm, "surfaceForm")
                    .valueObject(this, Alias::target, "target");
        }
    }
}
