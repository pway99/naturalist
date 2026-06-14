package com.naturalist.clades;

import com.naturalist.fieldnotes.Description;

import java.util.Optional;

public record Apoidea() implements Clade {

    private static final Description DESCRIPTION = new Description(
            """
            Bees and some kinds of wasps belong to one big group. The \
            wasps in this group are the ones that dig holes in the ground \
            or in wood to make nests for their babies. The bees evolved \
            from these wasps but switched from hunting insects to \
            collecting pollen from flowers.""",
            """
            Apoidea is the superfamily that includes all bees plus the \
            apoid wasps — sand wasps, digger wasps, and other solitary \
            hunting wasps that provision their nests with paralysed \
            insects. There are about 30,000 species altogether. Bees \
            evolved from within these wasps, switching from a carnivorous \
            to a vegetarian diet by collecting pollen instead of prey. \
            This means that "wasps" in this group are not a real \
            evolutionary group — bees are just pollen-collecting wasps.""",
            """
            Superfamily Apoidea (Hymenoptera: Aculeata) — apoid wasps and \
            bees, approximately 30,000 species. The traditional families \
            Crabronidae, Sphecidae, and related wasp lineages are \
            paraphyletic with respect to Anthophila (bees): bees arose \
            from within the spheciform wasps. Diagnostic features of the \
            superfamily include a slender petiole (propodeum), specialised \
            foretarsal grooming structures, and (in bees) branched body \
            hairs for pollen collection. EOL:676. No Linnaean rank above \
            family is formally recognised between Apoidea and Aculeata.""",
            """
            Superfamily Apoidea — paraphyletic wasp grade + monophyletic \
            bee clade (Anthophila). Molecular phylogenetics (Sann et al. \
            2018; Peters et al. 2017) place bees as sister to a subset of \
            Crabronidae (Pemphredoninae or Ammoplanina), rendering \
            Crabronidae and Sphecidae s.l. paraphyletic. The superfamily \
            has no Linnaean rank above family — it sits between family and \
            order in the hierarchy without a rankable name. Crown-group \
            origin in the mid-Cretaceous, coincident with the angiosperm \
            radiation that enabled the pollen-provisioning transition. \
            This node demonstrates that meaningful evolutionary groups \
            often have no corresponding rank in the Linnaean system.""");

    @Override
    public String slug() {
        return "apoidea";
    }

    @Override
    public String displayName() {
        return "Apoidea";
    }

    @Override
    public Description description() {
        return DESCRIPTION;
    }

    @Override
    public Optional<Clade> parent() {
        return Optional.of(new Holometabola());
    }
}
