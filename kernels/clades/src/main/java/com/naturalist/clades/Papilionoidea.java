package com.naturalist.clades;

import com.naturalist.fieldnotes.Description;

import java.util.Optional;

public record Papilionoidea() implements Clade {

    private static final Description DESCRIPTION = new Description(
            """
            Butterflies are the insects with big colourful wings that fly \
            during the day. They visit flowers in the garden and park, and \
            their caterpillars munch on leaves. All the butterflies you see \
            — swallowtails, whites, blues, brush-foots — belong to one big \
            family group called Papilionoidea.""",
            """
            Papilionoidea is the superfamily that contains all true \
            butterflies — about 18,000 species worldwide. It includes the \
            swallowtails (Papilionidae), whites and sulphurs (Pieridae), \
            blues and coppers (Lycaenidae), brush-footed butterflies \
            (Nymphalidae), and skippers (Hesperiidae). Unlike most moths, \
            butterflies are active during the day, have club-tipped \
            antennae, and usually rest with their wings held together above \
            their backs.""",
            """
            Superfamily Papilionoidea — the monophyletic butterfly clade \
            within Obtectomera (Lepidoptera), containing approximately \
            18,000 described species across seven families: Papilionidae, \
            Hedylidae, Hesperiidae, Pieridae, Lycaenidae, Riodinidae, and \
            Nymphalidae. Molecular phylogenetics places Papilionidae + \
            Hedylidae as the sister clade to the rest. Adults are \
            characterised by knobbed antennae, diurnal activity (with few \
            exceptions), and a lack of a frenulum wing-coupling mechanism. \
            Larvae are overwhelmingly phytophagous, and host-plant \
            co-evolution is a dominant theme in the radiation.""",
            """
            Superfamily Papilionoidea — strongly supported monophyletic \
            within Obtectomera; crown-group diversification linked to the \
            mid-Cretaceous angiosperm radiation. Espeland et al. (2018) \
            recovered Papilionidae + Hedylidae as sister to a clade \
            comprising Hesperiidae + (Pieridae + (Lycaenidae + Riodinidae + \
            Nymphalidae)). The superfamily corresponds to the traditional \
            'Rhopalocera' in a restricted sense, excluding the formerly \
            associated Castniidae. This node is the natural home for any \
            trait shared by all butterflies but not moths — e.g. obligate \
            diurnality, knobbed antennae — once trait declarations land in \
            the consuming domain.""");

    @Override
    public String slug() {
        return "papilionoidea";
    }

    @Override
    public String displayName() {
        return "Papilionoidea";
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
