package com.naturalist.clades;

import com.naturalist.fieldnotes.Description;

import java.util.Optional;

public record Commelinids() implements Clade {

    private static final Description DESCRIPTION = new Description(
            """
            Grasses like the ones in a lawn belong to a group of monocots \
            called commelinids.""",
            """
            Commelinids are a group of monocots that includes the grasses, \
            sedges, palms, gingers, and bananas. Grasses — which feed much of \
            the world as wheat, rice, and corn — are the biggest part of this \
            group.""",
            """
            Commelinids are the clade of monocots whose cell walls hold \
            distinctive ferulic-acid compounds that glow under ultraviolet \
            light. The group unites the grasses and sedges (Poales), the palms \
            (Arecales), the gingers and bananas (Zingiberales), and \
            Commelinales. Poales, and the grasses in particular, is its \
            ecologically and economically dominant order.""",
            """
            Commelinidae (commelinids) — a monocot clade marked by UV-fluorescent \
            ferulic and coumaric acids cross-linking the primary cell wall, and \
            largely by starchy rather than oily endosperm. Comprises Poales, \
            Arecales, Zingiberales, Commelinales, and Dasypogonaceae. The Oak \
            Vista catalog places Poales here — Festuca, the tall fescue turf \
            grass. Its parent is Monocots.""");

    @Override
    public String slug() {
        return "commelinids";
    }

    @Override
    public String displayName() {
        return "Commelinids";
    }

    @Override
    public Description description() {
        return DESCRIPTION;
    }

    @Override
    public Optional<Clade> parent() {
        return Optional.of(new Monocots());
    }
}
