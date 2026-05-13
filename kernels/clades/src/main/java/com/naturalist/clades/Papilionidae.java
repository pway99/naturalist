package com.naturalist.clades;

import com.naturalist.fieldnotes.Description;

import java.util.Optional;

public record Papilionidae() implements Clade {

    private static final Description DESCRIPTION = new Description(
            """
            Swallowtails are big colourful butterflies with little tails \
            on their back wings, like a swallow bird. Many of them are \
            yellow and black, and you can see them in the garden in \
            summer.""",
            """
            Papilionidae is the family of butterflies called swallowtails. \
            They are usually large, brightly coloured, and most species \
            have a thin tail-like extension on each hind wing. Pipevine \
            Swallowtail, Western Tiger Swallowtail, and Anise Swallowtail \
            are all members of this family. Swallowtail caterpillars have \
            a strange defence: they can pop out a forked orange organ \
            called an osmeterium that gives off a smelly chemical to \
            scare off ants and birds.""",
            """
            Family Papilionidae (Papilionoidea) — approximately 570 \
            species worldwide, including the swallowtails, apollos, and \
            birdwings. Adults are typically large butterflies; many have \
            tailed hindwings, hence the common name. Caterpillars possess \
            an eversible osmeterium — a Y-shaped gland behind the head \
            that emits volatile terpenoids when disturbed — and several \
            lineages sequester or synthesise host-plant chemical defences \
            for use in all life stages. Larval host plants are \
            taxonomically narrow within tribes: Troidini specialise on \
            Aristolochiaceae, Papilionini on Rutaceae and Apiaceae, \
            Parnassiini on Crassulaceae and Papaveraceae.""",
            """
            Family Papilionidae — sister to Hedylidae within Papilionoidea; \
            fossil record from the Eocene (~50 Ma). Subfamily structure: \
            Baroniinae (monotypic, Mexican relict), Parnassiinae (apollos \
            and festoons, Holarctic), and Papilioninae (true swallowtails \
            and birdwings, cosmopolitan). The tribe Troidini contains the \
            Battus philenor validation case for this kernel — its larval \
            host-plant specialisation on Aristolochia (Aristolochiaceae) \
            underlies its aristolochic-acid-based chemical defence and the \
            Müllerian/Batesian mimicry rings that surround it (Spicebush \
            Swallowtail, dark-morph female Tiger Swallowtail, Red-spotted \
            Purple). The chemical defence is not a clade-level trait — it \
            is restricted to a subset of Troidini and a few related \
            Papilionidae — and is therefore left to the species-level \
            record rather than declared on the family clade.""");

    @Override
    public String slug() {
        return "papilionidae";
    }

    @Override
    public String displayName() {
        return "Papilionidae";
    }

    @Override
    public Description description() {
        return DESCRIPTION;
    }

    @Override
    public Optional<Clade> parent() {
        return Optional.of(new Lepidoptera());
    }
}
