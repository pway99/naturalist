package com.naturalist.clades;

import com.naturalist.fieldnotes.Description;

import java.util.Optional;

public record Arthropoda() implements Clade {

    private static final Description DESCRIPTION = new Description(
            """
            Arthropods are animals with a hard outside, jointed legs, and \
            a body made of repeating parts. Beetles, butterflies, crabs, \
            and spiders are all arthropods.""",
            """
            Arthropoda is the part of the animal tree that includes \
            insects, spiders, scorpions, crabs, lobsters, centipedes, and \
            millipedes. Every arthropod has a hard outer covering called \
            an exoskeleton, a body built of repeating sections called \
            segments, and legs with bendable joints. Because the \
            exoskeleton cannot stretch, arthropods must shed it and grow \
            a new one as they get bigger — this is called moulting.""",
            """
            Phylum Arthropoda is the most species-rich phylum in Animalia, \
            defined by a chitinous exoskeleton, a segmented body, paired \
            jointed appendages, and growth by ecdysis (moulting). Major \
            sub-groups include Chelicerata (spiders, scorpions, horseshoe \
            crabs), Myriapoda (centipedes and millipedes), Crustacea \
            (crabs, lobsters, isopods), and Hexapoda (insects and \
            springtails). Arthropods occupy nearly every habitat — \
            terrestrial, freshwater, marine, and parasitic — and are \
            central to most ecological food webs.""",
            """
            Phylum Arthropoda — earliest crown-group fossils from the \
            Cambrian (Sirius Passet, Chengjiang faunas, ~520 Ma); Ediacaran \
            stem forms suggested but disputed. Synapomorphies include \
            chitinous, sclerotised exoskeleton; tagmosis (regional body \
            specialisation); jointed appendages with intrinsic musculature; \
            and ecdysis under control of ecdysone. The clade is the largest \
            in Animalia by described species count (~1.2 million, mostly \
            insects) and an order of magnitude larger by extant species \
            estimates. Pancrustacea (Crustacea + Hexapoda) is the prevailing \
            molecular topology, making insects derived crustaceans \
            phylogenetically. No traits attached at this node in the \
            current kernel; future trait declarations might include cuticle \
            ultrastructure or moulting hormone signalling once a consumer \
            needs them.""");

    @Override
    public String slug() {
        return "arthropoda";
    }

    @Override
    public String displayName() {
        return "Arthropoda";
    }

    @Override
    public Description description() {
        return DESCRIPTION;
    }

    @Override
    public Optional<Clade> parent() {
        return Optional.of(new Animalia());
    }
}
