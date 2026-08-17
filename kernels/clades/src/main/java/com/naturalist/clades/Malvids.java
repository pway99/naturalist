package com.naturalist.clades;

import com.naturalist.fieldnotes.Description;

import java.util.Optional;

public record Malvids() implements Clade {

    private static final Description DESCRIPTION = new Description(
            """
            Scented geraniums, oranges, and pineapple guava are malvids.""",
            """
            Malvids are the other half of the rosids. They include the mallow \
            family, the citrus and orange group, mustards and cabbages, and \
            scented geraniums. Many malvids make sharp-smelling oils or spicy, \
            mustardy chemicals.""",
            """
            Malvids (eurosids II) are the rosid subclade containing Malvales \
            (mallows), Brassicales (mustards and cabbages), Sapindales (citrus \
            and maples), Myrtales, and Geraniales. Many produce glucosinolates \
            or aromatic terpene oils. Citrus, cabbage, and cotton are familiar \
            malvids.""",
            """
            Malvidae (malvids / eurosids II) — rosid subclade comprising \
            Geraniales, Myrtales, Crossosomatales, Picramniales, Sapindales, \
            Huerteales, Malvales, and Brassicales. Chemically notable for \
            glucosinolate biosynthesis in Brassicales and for diverse \
            terpenoid oils. The Oak Vista catalog places three orders here: \
            Geraniales (Pelargonium), Sapindales (Citrus), and Brassicales \
            (Lobularia). Its parent is Rosids.""");

    @Override
    public String slug() {
        return "malvids";
    }

    @Override
    public String displayName() {
        return "Malvids";
    }

    @Override
    public Description description() {
        return DESCRIPTION;
    }

    @Override
    public Optional<Clade> parent() {
        return Optional.of(new Rosids());
    }
}
