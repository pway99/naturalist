package com.naturalist.clades;

import com.naturalist.fieldnotes.Description;

import java.util.Optional;

public record Animalia() implements Clade {

    private static final Description DESCRIPTION = new Description(
            """
            Animals are living things that have to eat other living things \
            to grow. They cannot make their own food like plants do. Dogs, \
            beetles, fish, and you are all animals.""",
            """
            Animalia is the part of the tree of life that includes every \
            animal — mammals, birds, reptiles, fish, insects, spiders, \
            worms, sponges, and many more. Animals are multicellular and \
            have to eat other organisms because they cannot make food from \
            sunlight. Most animals can move on their own, at least at some \
            point in their life.""",
            """
            Kingdom Animalia is the clade of multicellular, heterotrophic \
            eukaryotes that develop from a blastula and (with rare \
            exceptions) ingest food rather than absorb it. Animal cells \
            lack cell walls, which allows for the diversity of tissue types \
            and rapid movement. Major animal phyla include Porifera \
            (sponges), Cnidaria (jellyfish and corals), Mollusca, Annelida \
            (segmented worms), Arthropoda, Echinodermata, and Chordata \
            (vertebrates and their close relatives).""",
            """
            Kingdom Animalia (Metazoa) — monophyletic clade sister to \
            Choanoflagellata within Opisthokonta. Crown-group age \
            approximately 650 Ma. Defined by collagen-based extracellular \
            matrix, gap junctions, and the developmental signalling \
            pathways (Wnt, Notch, Hedgehog) that pattern multicellular body \
            plans. All animals are heterotrophs; the few endosymbiotic \
            exceptions (corals with zooxanthellae, Elysia slugs with \
            kleptoplasts) retain animal phylogeny while exploiting algal \
            photosynthesis. No clade-level traits are declared on Animalia \
            in the current kernel — animal-wide attributes (e.g., the Hox \
            cluster, neural crest in vertebrates) are introduced at the \
            sub-clades that originated them.""");

    @Override
    public String slug() {
        return "animalia";
    }

    @Override
    public String displayName() {
        return "Animalia";
    }

    @Override
    public Description description() {
        return DESCRIPTION;
    }

    @Override
    public Optional<Clade> parent() {
        return Optional.of(new Eukaryota());
    }
}
