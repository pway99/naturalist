package com.naturalist.clades;

import com.naturalist.fieldnotes.Description;

import java.util.Optional;

public record Holometabola() implements Clade {

    private static final Description DESCRIPTION = new Description(
            """
            Some insects start out looking very different from their \
            grown-up shape. A caterpillar turns into a butterfly; a grub \
            turns into a beetle. Insects that do this big change are \
            called holometabolous.""",
            """
            Holometabola is the part of the insect tree whose members go \
            through complete metamorphosis: egg → larva → pupa → adult. \
            The larva is the eating and growing stage (like a caterpillar), \
            the pupa is a resting stage where the body is rebuilt, and the \
            adult is the flying, mating stage. Butterflies, moths, beetles, \
            flies, bees, wasps, and ants are all holometabolous. \
            Grasshoppers and true bugs are not — their young already look \
            mostly like the adult.""",
            """
            Superorder Holometabola (sometimes called Endopterygota) is \
            the clade of insects that undergo complete metamorphosis: a \
            morphologically distinct larva that is highly specialised for \
            feeding, a non-feeding pupa during which adult tissues develop, \
            and a winged adult specialised for dispersal and reproduction. \
            The four-stage life cycle decouples larval and adult ecological \
            niches and is the strongest single predictor of insect \
            diversification — over 60% of all described insect species \
            belong to Holometabola.""",
            """
            Holometabola — strongly supported monophyletic clade containing \
            Neuropterida (lacewings and relatives), Coleoptera, \
            Strepsiptera, Hymenoptera, Mecoptera, Siphonaptera, Diptera, \
            Trichoptera, and Lepidoptera. Crown-group origin in the Late \
            Carboniferous to Early Permian; the four-stage life history is \
            hypothesised to derive from a heterochronic shift in pronymphal \
            expression in a hemimetabolous ancestor. This is the \
            originating node for complete metamorphosis — the natural home \
            for a Metaboly trait declaration once the Phase 2 trait type \
            lands. The kernel declares no traits itself; the insects domain \
            will declare Holometabola → MetabolyTrait(Holometabolous), and \
            every descendant (Lepidoptera, Coleoptera, Hymenoptera, …) \
            resolves the trait via clade traversal rather than duplicating \
            it on each rank-level record.""");

    @Override
    public String slug() {
        return "holometabola";
    }

    @Override
    public String displayName() {
        return "Holometabola";
    }

    @Override
    public Description description() {
        return DESCRIPTION;
    }

    @Override
    public Optional<Clade> parent() {
        return Optional.of(new Insecta());
    }
}
