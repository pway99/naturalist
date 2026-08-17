package com.naturalist.clades;

import com.naturalist.fieldnotes.Description;

import java.util.Optional;

public record Campanulids() implements Clade {

    private static final Description DESCRIPTION = new Description(
            """
            Dill and its cousins are campanulids.""",
            """
            Campanulids are the other half of the asterids. They include the \
            carrot and dill family, the daisy and sunflower family, hollies, \
            and bellflowers. Many carry their tiny flowers packed together in \
            clusters or flat-topped heads.""",
            """
            Campanulids (euasterids II) are an asterid subclade containing \
            Apiales (carrots, dill, celery), Asterales (daisies, sunflowers), \
            Dipsacales (honeysuckles), and Aquifoliales (hollies). Many produce \
            aromatic seeds and umbrella-shaped flower clusters called umbels, \
            or dense flower heads.""",
            """
            Campanulidae (campanulids / euasterids II) — core-asterid clade \
            comprising Aquifoliales, Escalloniales, Asterales, Bruniales, \
            Paracryphiales, Dipsacales, and Apiales. In the Oak Vista catalog \
            campanulids places Apiales — Anethum, dill. Its parent is \
            Asterids.""");

    @Override
    public String slug() {
        return "campanulids";
    }

    @Override
    public String displayName() {
        return "Campanulids";
    }

    @Override
    public Description description() {
        return DESCRIPTION;
    }

    @Override
    public Optional<Clade> parent() {
        return Optional.of(new Asterids());
    }
}
