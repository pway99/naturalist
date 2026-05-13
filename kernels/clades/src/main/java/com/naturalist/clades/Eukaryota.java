package com.naturalist.clades;

import com.naturalist.fieldnotes.Description;

import java.util.Optional;

public record Eukaryota() implements Clade {

    private static final Description DESCRIPTION = new Description(
            """
            Every plant, animal, mushroom, and tiny pond creature you can see \
            is made of cells with a little control room inside called a \
            nucleus. All of those living things belong to one big family \
            called Eukaryota.""",
            """
            Eukaryota is the part of the tree of life that contains all \
            living things whose cells have a nucleus — the small sac inside \
            the cell that holds the DNA. Plants, animals, fungi, and most of \
            the wiggly creatures you can see in pond water are eukaryotes. \
            Bacteria are not eukaryotes — their cells are simpler and have \
            no nucleus.""",
            """
            Eukaryota is one of the three domains of life (alongside \
            Bacteria and Archaea), defined by membrane-bound organelles and \
            a true nucleus. Multicellular life — animals, land plants, fungi \
            — is exclusively eukaryotic, as are most familiar microorganisms \
            (amoebae, ciliates, diatoms). Eukaryotic cells originated through \
            endosymbiosis: a host archaeon engulfed an alphaproteobacterium \
            that became the mitochondrion, and in the plant lineage a \
            cyanobacterium became the chloroplast.""",
            """
            Domain Eukaryota — sister to the Asgard archaea in current rooted \
            tree reconstructions; crown-group age estimated at 1.8–2.1 Ga \
            with the Last Eukaryotic Common Ancestor (LECA) inferred to have \
            been mitochondriate, sexually reproducing, and equipped with a \
            full complement of cytoskeletal and membrane-trafficking \
            machinery. The clade is the root of every other clade in this \
            kernel; its parent reference is intentionally null. Holds no \
            declared traits at this node — life-history attributes attach \
            further down the tree (Metaboly on Holometabola, photosynthesis \
            on Archaeplastida, etc.).""");

    @Override
    public String slug() {
        return "eukaryota";
    }

    @Override
    public String displayName() {
        return "Eukaryota";
    }

    @Override
    public Description description() {
        return DESCRIPTION;
    }

    @Override
    public Optional<Clade> parent() {
        return Optional.empty();
    }
}
