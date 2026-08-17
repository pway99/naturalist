package com.naturalist.clades;

import com.naturalist.fieldnotes.Description;

import java.util.Optional;

public record Fabids() implements Clade {

    private static final Description DESCRIPTION = new Description(
            """
            Clover, peaches, figs, and passionflowers are all fabids.""",
            """
            Fabids are one of the two halves of the rosids. They include the \
            pea and bean family (like clover), the rose family (peaches, \
            apples, cherries), figs, and passionflowers. Many fabids can take \
            nitrogen from the air with the help of tiny bacteria in their \
            roots.""",
            """
            Fabids (eurosids I) are a rosid subclade containing the orders \
            Fabales, Rosales, Malpighiales, and their relatives. This clade \
            holds the "nitrogen-fixing clade," where partnerships with \
            nitrogen-fixing bacteria evolved most readily. Legumes (Fabales), \
            roses and figs (Rosales), and willows and passionflowers \
            (Malpighiales) are all fabids.""",
            """
            Fabidae (fabids / eurosids I) — the rosid subclade bearing the \
            nitrogen-fixing clade, comprising Fabales, Rosales, Cucurbitales, \
            and Fagales together with the COM clade (Celastrales, Oxalidales, \
            Malpighiales). In the Oak Vista catalog fabids places three orders: \
            Rosales (Prunus, Pyrus, Ficus), Fabales (Trifolium), and \
            Malpighiales (Passiflora edulis). Its parent is Rosids.""");

    @Override
    public String slug() {
        return "fabids";
    }

    @Override
    public String displayName() {
        return "Fabids";
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
