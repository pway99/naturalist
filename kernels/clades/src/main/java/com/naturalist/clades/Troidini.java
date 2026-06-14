package com.naturalist.clades;

import com.naturalist.fieldnotes.Description;

import java.util.Optional;

public record Troidini() implements Clade {

    private static final Description DESCRIPTION = new Description(
            """
            Pipevine swallowtails are dark butterflies that eat a plant \
            called pipevine when they are caterpillars. The plant makes \
            them taste bad, so birds learn to leave them alone. Other \
            butterflies copy their dark colour to trick birds into thinking \
            they taste bad too.""",
            """
            Troidini is a tribe of about 135 swallowtail species often \
            called pipevine swallowtails or birdwings. Their caterpillars \
            feed on Aristolochiaceae (pipevine) plants and store the \
            plant's toxic aristolochic acids in their bodies, making them \
            poisonous to predators throughout their lives. Because they \
            are toxic, many other non-toxic butterflies have evolved to \
            look like them — a survival strategy called mimicry.""",
            """
            Tribe Troidini (Papilionidae: Papilioninae) — approximately \
            135 species of pharmacophagous swallowtails that sequester \
            aristolochic acids from their larval host plants in \
            Aristolochiaceae. The sequestered toxins provide chemical \
            defence against vertebrate predators across all life stages, \
            and Troidini species serve as models in both Müllerian and \
            Batesian mimicry complexes. Includes the genera Battus, \
            Atrophaneura, Pachliopta, Troides (birdwings), and \
            Ornithoptera (Queen Alexandra's birdwing — largest butterfly).""",
            """
            Tribe Troidini — pharmacophagous clade within Papilioninae; \
            monophyly well-supported by molecular data (Condamine et al. \
            2012). Larval specialisation on Aristolochiaceae and \
            sequestration of aristolochic acids is the defining \
            synapomorphy at the ecological level. Battus philenor (Pipevine \
            Swallowtail) is the validation case for this kernel — its \
            chemical defence underlies the Müllerian/Batesian mimicry ring \
            involving Spicebush Swallowtail (Papilio troilus), dark-morph \
            female Tiger Swallowtail (P. glaucus), and Red-spotted Purple \
            (Limenitis arthemis astyanax). The tribe-level placement is a \
            trait-bearing node for pharmacophagy, distinct from the \
            family-level osmeterium defence.""");

    @Override
    public String slug() {
        return "troidini";
    }

    @Override
    public String displayName() {
        return "Troidini";
    }

    @Override
    public Description description() {
        return DESCRIPTION;
    }

    @Override
    public Optional<Clade> parent() {
        return Optional.of(new Papilionidae());
    }
}
