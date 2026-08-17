package com.naturalist.clades;

import com.naturalist.fieldnotes.Description;

import java.util.Optional;

public record Superrosids() implements Clade {

    private static final Description DESCRIPTION = new Description(
            """
            Roses, beans, and their many cousins belong to a big plant group \
            called superrosids.""",
            """
            Superrosids are a large branch of eudicots that contains the \
            rosids plus a few smaller groups. Most of the fruit and nut trees \
            people grow — apples, peaches, cherries — sit somewhere inside \
            this branch.""",
            """
            Superrosids are a clade of core eudicots made up of the large \
            rosid group together with the order Saxifragales. They form one of \
            the two great divisions of the core eudicots, the other being the \
            superasterids. The name gathers an enormous diversity of trees, \
            shrubs, and herbs.""",
            """
            Superrosidae — the rosid clade plus Saxifragales, recovered as \
            sister to the superasterids within the core eudicots (Pentapetalae). \
            Retained as an explicit node because it, with superasterids, frames \
            the core-eudicot bifurcation that a naturalist needs to read a \
            eudicot's placement; in the Oak Vista catalog it is a pass-through \
            parent above rosids. Intermediate grades (core eudicots, \
            Pentapetalae) are collapsed unless a later catalog needs them. Its \
            parent is Eudicots.""");

    @Override
    public String slug() {
        return "superrosids";
    }

    @Override
    public String displayName() {
        return "Superrosids";
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
