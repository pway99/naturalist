package com.naturalist.clades;

import com.naturalist.fieldnotes.Description;

import java.util.Optional;

public record Superasterids() implements Clade {

    private static final Description DESCRIPTION = new Description(
            """
            Carnations and daisies belong to a big plant group called \
            superasterids.""",
            """
            Superasterids are a large branch of eudicots that holds the \
            asterids — like daisies and mints — plus a few other groups such \
            as the carnation family. It is the branch that explains why a \
            carnation is a eudicot but neither a rosid nor an asterid.""",
            """
            Superasterids are one of the two great divisions of the core \
            eudicots, sister to the superrosids. They contain the asterids \
            together with several orders — most notably Caryophyllales \
            (carnations, cacti, beets) — that sit outside the asterids proper. \
            This is why Dianthus, a carnation, is placed here rather than among \
            the asterids.""",
            """
            Superasteridae — core-eudicot clade comprising the asterids plus \
            Caryophyllales, Santalales, Berberidopsidales, and Gunnerales, \
            sister to the superrosids within Pentapetalae. In the Oak Vista \
            catalog superasterids is the placement for Caryophyllales \
            (Dianthus) — the "neither rosid nor asterid" case that makes this \
            node pedagogically load-bearing. Its parent is Eudicots.""");

    @Override
    public String slug() {
        return "superasterids";
    }

    @Override
    public String displayName() {
        return "Superasterids";
    }

    @Override
    public Description description() {
        return DESCRIPTION;
    }

    @Override
    public Optional<Clade> parent() {
        return Optional.of(new Eudicots());
    }
}
