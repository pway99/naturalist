package com.naturalist.clades;

import com.naturalist.fieldnotes.Description;

import java.util.Optional;

public record Angiosperms() implements Clade {

    private static final Description DESCRIPTION = new Description(
            """
            Plants that grow flowers and make seeds tucked inside a fruit are \
            called flowering plants. Apples, roses, grass, and oak trees are \
            all flowering plants.""",
            """
            Angiosperms are the flowering plants — the most common plants \
            around us. They make flowers, and their seeds grow inside a \
            protective fruit, like an apple, a bean pod, or a tomato. Almost \
            every plant in a garden or a meadow is an angiosperm.""",
            """
            Angiosperms, the flowering plants, are seed plants whose seeds \
            develop enclosed inside a carpel that ripens into a fruit. Their \
            defining features are flowers, double fertilisation that produces \
            a nutrient tissue called endosperm, and efficient water-conducting \
            vessels. With roughly 300,000 species they dominate most land \
            plant communities on Earth.""",
            """
            Angiospermae (Magnoliophyta) — crown-group flowering plants, \
            sister to the extant gymnosperms within seed plants. \
            Synapomorphies include the carpel enclosing the ovules, double \
            fertilisation yielding triploid endosperm, sieve-tube/companion-cell \
            phloem, and (broadly) vessel elements. The APG IV backbone divides \
            the crown into the ANA grade, magnoliids, monocots, and eudicots; \
            this node is the parent of the three subclades the Oak Vista \
            catalog reaches — magnoliids, monocots, and eudicots. Its parent is \
            Plantae in the collapsed kernel tree.""");

    @Override
    public String slug() {
        return "angiosperms";
    }

    @Override
    public String displayName() {
        return "Angiosperms";
    }

    @Override
    public Description description() {
        return DESCRIPTION;
    }

    @Override
    public Optional<Clade> parent() {
        return Optional.of(new Plantae());
    }
}
