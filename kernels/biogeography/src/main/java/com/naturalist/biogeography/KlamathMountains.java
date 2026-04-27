package com.naturalist.biogeography;

import com.naturalist.fieldnotes.Description;

public record KlamathMountains() implements Bioregion {

    private static final Description DESCRIPTION = new Description(
            "The Klamath Mountains are wild and rugged with rivers that twist through deep " +
                    "canyons. So many different kinds of trees grow here that scientists come " +
                    "from all over the world to look at them. Salmon swim up the rivers each year.",
            "The Klamath Mountains are some of the wildest, most botanically rich mountains in " +
                    "North America. The combination of old rocks, deep canyons, and a wet ocean " +
                    "climate creates pockets where rare conifer trees still grow that disappeared " +
                    "from most other places long ago. The Klamath, Trinity, and Salmon rivers cut " +
                    "through the range, carrying salmon and steelhead each year.",
            "The Klamath Mountains region is one of the most floristically diverse areas in " +
                    "temperate North America, hosting more conifer species than any other region " +
                    "of comparable size on Earth (≥17 species). The complex geology — accreted " +
                    "terranes of ultramafic, metamorphic, and sedimentary rocks — combined with " +
                    "high rainfall (1500–3000 mm annually) and refugial topography supports relict " +
                    "populations of Pleistocene-era species, including Brewer spruce (Picea " +
                    "breweriana), Port Orford cedar (Chamaecyparis lawsoniana), and weeping " +
                    "spruce. The Klamath, Trinity, and Salmon river systems support anadromous " +
                    "salmonids and are central to indigenous Karuk, Yurok, and Hupa cultural " +
                    "ecology.",
            "The Klamath Mountains correspond to EPA Level III Ecoregion 78. Geologically, the " +
                    "region is an accretionary tectonic collage of Paleozoic to Mesozoic terranes " +
                    "(Eastern Klamath, Central Metamorphic, Western Triassic and Paleozoic, " +
                    "Western Jurassic) including extensive ophiolitic ultramafic exposures " +
                    "(serpentinite, peridotite). Edaphic specialisation on serpentine soils drives " +
                    "exceptional plant endemism — California's serpentine flora includes many " +
                    "Klamath endemics (Darlingtonia californica, Lewisia cotyledon, Calochortus " +
                    "spp.). Conifer diversity is unmatched globally for the latitude: Picea " +
                    "breweriana (paleoendemic), Chamaecyparis lawsoniana (relict, restricted), " +
                    "Pinus monticola, P. lambertiana, P. jeffreyi, P. attenuata, P. balfouriana, " +
                    "Abies magnifica var. shastensis, A. concolor, A. procera, Pseudotsuga " +
                    "menziesii, Tsuga heterophylla, T. mertensiana, Calocedrus decurrens, " +
                    "Sequoia sempervirens (coastal margin), Taxus brevifolia, Torreya californica, " +
                    "and Juniperus occidentalis. Anadromous fish: Oncorhynchus tshawytscha (Spring " +
                    "and Fall Chinook), O. kisutch (Coho), O. mykiss (steelhead), Lampetra " +
                    "tridentata (Pacific lamprey)."
    );

    @Override
    public String slug() {
        return "klamath-mountains";
    }

    @Override
    public String displayName() {
        return "Klamath Mountains";
    }

    @Override
    public Description description() {
        return DESCRIPTION;
    }
}
