package com.naturalist.clades;

import com.naturalist.fieldnotes.Description;

import java.util.Optional;

public record Asterids() implements Clade {

    private static final Description DESCRIPTION = new Description(
            """
            Daisies, tomatoes, mint, and persimmons are asterids.""",
            """
            Asterids are a big group of flowering plants inside the \
            superasterids. They include daisies and sunflowers, tomatoes and \
            potatoes, mints and sages, and coffee. Their petals are usually \
            joined together into a tube or a cup.""",
            """
            Asterids are a clade of eudicots making up about a third of all \
            flowering-plant species. Most have petals fused into a tube and, \
            often, four or five stamens. The group divides into the lamiids \
            (mints, tomatoes, coffee) and the campanulids (dill, sunflowers, \
            hollies), with Ericales as an early-branching lineage.""",
            """
            Asteridae (asterids) — a superasterid clade broadly synapomorphic \
            for sympetaly (a fused corolla), epipetalous stamens equal in \
            number to the corolla lobes, and iridoid chemistry. It comprises a \
            basal grade (Cornales, Ericales) plus the core asterids, which \
            split into the lamiids (euasterids I) and campanulids \
            (euasterids II). The Oak Vista catalog reaches asterids through \
            both core subclades; asterids is retained as their shared parent. \
            Its parent is Superasterids.""");

    @Override
    public String slug() {
        return "asterids";
    }

    @Override
    public String displayName() {
        return "Asterids";
    }

    @Override
    public Description description() {
        return DESCRIPTION;
    }

    @Override
    public Optional<Clade> parent() {
        return Optional.of(new Superasterids());
    }
}
