package com.naturalist.clades;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Static helpers for walking the clade parent chain.
 * <p>
 * Because clades are sealed records with their parent links hardcoded in
 * the kernel, traversal does not need a lookup function over the kernel's
 * own structure — it just follows {@link Clade#parent()} until it hits
 * the empty Optional at the root. Trait declarations, however, are owned
 * by the consuming domain (insects-api knows what's true of holometabolous
 * insects; the kernel does not), so {@link #findTrait} takes a
 * {@code Function<Clade, Set<Trait>>} that the caller provides.
 * <p>
 * The recommended shape for the caller's function is a pattern-matching
 * {@code switch} over the sealed {@link Clade} permits the domain cares
 * about, with {@code default -> Set.of()} for the rest — pure, stateless,
 * and reviewable. See the slice plan for an example.
 */
public final class CladeTraversal {

    private CladeTraversal() {
    }

    /**
     * Walks the parent chain starting at {@code start}, returning the first
     * trait declared at any ancestor that is an instance of {@code traitType}.
     * Stops at the root if no match is found.
     *
     * @param start      the clade to start the search from (inclusive)
     * @param traitType  the trait class to look for
     * @param traitsFor  caller-supplied function from a clade to the trait set
     *                   the caller's domain declares on that clade. Returning
     *                   an empty set is fine; the function must not return
     *                   {@code null}.
     * @return the nearest declared trait of the given type, or empty
     */
    public static <T extends Trait> Optional<T> findTrait(
            Clade start,
            Class<T> traitType,
            Function<Clade, Set<Trait>> traitsFor) {
        Optional<Clade> current = Optional.of(start);
        while (current.isPresent()) {
            for (Trait t : traitsFor.apply(current.get())) {
                if (traitType.isInstance(t)) {
                    return Optional.of(traitType.cast(t));
                }
            }
            current = current.get().parent();
        }
        return Optional.empty();
    }

    /**
     * Returns the chain from {@code start} to the root, inclusive. The first
     * element is {@code start}; the last element is the clade whose
     * {@link Clade#parent()} is empty (Eukaryota for the current kernel's
     * permits).
     */
    public static List<Clade> ancestry(Clade start) {
        List<Clade> chain = new ArrayList<>();
        Optional<Clade> current = Optional.of(start);
        while (current.isPresent()) {
            chain.add(current.get());
            current = current.get().parent();
        }
        return List.copyOf(chain);
    }
}
