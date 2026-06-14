package com.naturalist.clades;

import com.naturalist.fieldnotes.Description;

import java.util.Optional;

public record Anthophila() implements Clade {

    private static final Description DESCRIPTION = new Description(
            """
            Bees are fuzzy flying insects that visit flowers to collect \
            pollen and nectar. They carry pollen from flower to flower, \
            helping plants make seeds and fruit. Honeybees live in hives, \
            but most bees live alone in tiny holes in the ground or in \
            wood.""",
            """
            Anthophila is the clade that contains all bees — about 20,000 \
            species worldwide. Unlike their wasp ancestors, bees are \
            vegetarian: they collect pollen and nectar from flowers to feed \
            their young. Their bodies are covered in branched hairs that \
            trap pollen grains. Most bees are solitary — each female makes \
            her own nest — but honeybees and bumblebees live in social \
            colonies. Bees evolved from within the hunting wasps of \
            Apoidea and have no formal Linnaean rank of their own.""",
            """
            Clade Anthophila (Apoidea) — all bees, approximately 20,000 \
            described species across seven families: Andrenidae, Apidae, \
            Colletidae, Halictidae, Megachilidae, Melittidae, and \
            Stenotritidae. The clade is defined by the transition from \
            predatory provisioning (paralysed arthropod prey) to pollen \
            provisioning — a single evolutionary origin. Morphological \
            synapomorphies include branched (plumose) body hairs and \
            specialised pollen-transport structures (scopa or corbicula). \
            Anthophila corresponds to no formal Linnaean rank between \
            family and superfamily.""",
            """
            Clade Anthophila — monophyletic, nested within paraphyletic \
            apoid wasps. Single origin of pollen provisioning from a \
            crabronid-grade ancestor (likely Pemphredoninae or \
            Ammoplanina; Sann et al. 2018). Crown-group diversification \
            mid-Cretaceous (~100 Ma), tightly linked to angiosperm \
            diversification. The clade has no Linnaean rank: it is not a \
            family, superfamily, or infraorder — it is simply "all bees," \
            a monophyletic group without a rank assignment. This makes it \
            the strongest clade ≠ rank case in this kernel: a universally \
            recognised, ecologically dominant clade that the Linnaean \
            system cannot name at any level. Trait attachment point for \
            pollen provisioning, plumose setae, and floral constancy.""");

    @Override
    public String slug() {
        return "anthophila";
    }

    @Override
    public String displayName() {
        return "Anthophila";
    }

    @Override
    public Description description() {
        return DESCRIPTION;
    }

    @Override
    public Optional<Clade> parent() {
        return Optional.of(new Apoidea());
    }
}
