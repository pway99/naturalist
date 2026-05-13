package com.naturalist.insects.lifestage;

import com.naturalist.clades.Trait;

/**
 * Trait wrapper attaching a {@link Metaboly} declaration to a clade.
 * Declared by the insects domain on Hexapoda sub-clades that originated
 * a particular developmental pattern — most notably
 * {@code new MetabolyTrait(new Holometabolous())} on
 * {@code com.naturalist.clades.Holometabola}.
 * <p>
 * The trait is resolved from any descendant species, genus, or family
 * by calling
 * {@code CladeTraversal.findTrait(placedIn, MetabolyTrait.class, InsectClades::traitsFor)}.
 * The kernel itself knows nothing about metaboly — the insects domain
 * supplies the declaration function.
 */
public record MetabolyTrait(Metaboly metaboly) implements Trait {
}
