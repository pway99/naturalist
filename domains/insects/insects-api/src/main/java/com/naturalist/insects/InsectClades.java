package com.naturalist.insects;

import com.naturalist.clades.Clade;
import com.naturalist.clades.Holometabola;
import com.naturalist.clades.Trait;
import com.naturalist.insects.lifestage.Holometabolous;
import com.naturalist.insects.lifestage.MetabolyTrait;

import java.util.Set;

/**
 * Insect-domain trait declarations on clade-kernel nodes. The function
 * is pure — every declaration is a literal in the {@code switch} body
 * below, reviewed in the same PR that introduces the trait.
 *
 * <p>Pass {@link #traitsFor} as the third argument to
 * {@link com.naturalist.clades.CladeTraversal#findTrait} when resolving
 * a trait from any insect-domain consumer. The {@code default -> Set.of()}
 * arm is intentional — most clade permits in the kernel carry no
 * insect-domain trait, and forcing exhaustive {@code case} entries would
 * clutter the file without changing the result.
 *
 * <p>The kernel never imports this class; only insect-domain consumers
 * do. The clades kernel stays trait-agnostic.
 */
public final class InsectClades {

    private InsectClades() {
    }

    public static Set<Trait> traitsFor(Clade clade) {
        return switch (clade) {
            case Holometabola _ -> Set.of(new MetabolyTrait(new Holometabolous()));
            // Future declarations land here as the kernel adds the
            // corresponding clade permits — e.g.:
            //   case Hemiptera _  -> Set.of(new MetabolyTrait(new Hemimetabolous()));
            //   case Odonata _    -> Set.of(new MetabolyTrait(new Hemimetabolous()));
            //   case Zygentoma _  -> Set.of(new MetabolyTrait(new Ametabolous()));
            default -> Set.of();
        };
    }
}
