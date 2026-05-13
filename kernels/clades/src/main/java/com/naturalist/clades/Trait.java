package com.naturalist.clades;

/**
 * Marker interface for typed declarations attached to a {@link Clade}.
 * <p>
 * A trait is a fact about the clade and every descendant — for example,
 * "members of this clade undergo complete metamorphosis". Traits are
 * declared once at the originating clade and inherited by traversal
 * ({@link CladeTraversal#findTrait}); descendants do not redeclare them.
 * <p>
 * The clades kernel stores no traits of its own and interprets nothing.
 * Trait <em>types</em> live in the kernel or domain that owns the concept
 * (e.g. developmental biology owns {@code Metaboly}). Trait <em>declarations</em>
 * — the actual "clade X has trait Y" mappings — live in the consuming
 * domain that cares about those traits (insects-api decides that
 * Holometabola is holometabolous; plants-api would decide separately what
 * Archaeplastida photosynthesises). The kernel-provided traversal helper
 * takes a {@code Function<Clade, Set<Trait>>} from the caller so trait
 * lookup remains the consumer's responsibility.
 * <p>
 * Intentionally not sealed — the set of trait types is open across the
 * codebase. Implementations should be small immutable records (or
 * stateless markers) so they remain value-safe inside a {@code Set}.
 */
public interface Trait {
}
