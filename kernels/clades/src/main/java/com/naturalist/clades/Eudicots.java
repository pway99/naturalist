package com.naturalist.clades;

import com.naturalist.fieldnotes.Description;

import java.util.Optional;

public record Eudicots() implements Clade {

    private static final Description DESCRIPTION = new Description(
            """
            Most flowers you know — roses, sunflowers, beans — come from plants \
            called eudicots. Their sprouts open with two little seed-leaves.""",
            """
            Eudicots are the largest group of flowering plants. They usually \
            sprout with two seed-leaves, have leaves with a branching network \
            of veins, and flower parts in fours or fives. Roses, beans, oaks, \
            sunflowers, and tomatoes are all eudicots.""",
            """
            Eudicots, the "true dicots," are flowering plants united by pollen \
            grains with three furrows (tricolpate pollen). They typically show \
            net-veined leaves, floral parts in fours or fives, a taproot, and a \
            ring of vascular bundles with a cambium that allows woody growth. \
            About three-quarters of all flowering-plant species are eudicots.""",
            """
            Eudicotyledoneae (Tricolpatae) — a clade defined by the \
            synapomorphy of triaperturate (tricolpate or derived) pollen, \
            distinguishing it from the monosulcate condition of monocots and \
            magnoliids. APG coined the name precisely because the traditional \
            "dicots" is paraphyletic: it lumped the eudicots together with the \
            magnoliids and the ANA grade. This node is the parent of the two \
            core-eudicot subclades the catalog reaches — superrosids and \
            superasterids — and the teaching anchor for why "dicots" is not a \
            real group. Its parent is Angiosperms.""");

    @Override
    public String slug() {
        return "eudicots";
    }

    @Override
    public String displayName() {
        return "Eudicots";
    }

    @Override
    public Description description() {
        return DESCRIPTION;
    }

    @Override
    public Optional<Clade> parent() {
        return Optional.of(new Angiosperms());
    }
}
