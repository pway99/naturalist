package com.naturalist.biogeography;

import com.naturalist.fieldnotes.Description;

public record ModocPlateau() implements Bioregion {

    private static final Description DESCRIPTION = new Description(
            "The Modoc Plateau is a wide-open high country with sagebrush, juniper trees, and " +
                    "old volcanoes. Pronghorn antelope run across the flats. The lakes here are " +
                    "important resting places for ducks and geese flying south in autumn.",
            "The Modoc Plateau is a high, dry, lightly populated landscape in northeastern " +
                    "California. Sagebrush flats, scattered western juniper trees, and grasslands " +
                    "stretch across volcanic tablelands. Shallow alkaline lakes — Tule Lake, " +
                    "Lower Klamath, Goose Lake — are stops on the Pacific Flyway, drawing huge " +
                    "numbers of waterfowl in spring and autumn migration. Cold winters and warm " +
                    "summer days with cool nights define a high-desert climate.",
            "The Modoc Plateau is a high (1300–1800 m) volcanic tableland in northeastern " +
                    "California, bordering Oregon and Nevada. The landscape is shaped by " +
                    "Miocene–Pliocene basaltic flood volcanism producing extensive lava plains " +
                    "punctuated by cinder cones and shield volcanoes. Vegetation is dominated by " +
                    "Great Basin sagebrush steppe (Artemisia tridentata), juniper woodland " +
                    "(Juniperus occidentalis), and saltbush flats around the alkaline pluvial " +
                    "lake basins. The Pacific Flyway concentrates millions of migratory waterfowl " +
                    "at Tule Lake and Lower Klamath National Wildlife Refuges in spring and " +
                    "autumn — among the largest concentrations on the continent. Continental " +
                    "high-desert climate: cold dry winters, hot dry summers with substantial " +
                    "diurnal temperature range.",
            "The Modoc Plateau corresponds to EPA Level III Ecoregion 9 (Eastern Cascades Slopes " +
                    "and Foothills) in part and Ecoregion 80 (Northern Basin and Range) in part. " +
                    "Geologically, the plateau is built of Miocene to Pleistocene basaltic and " +
                    "andesitic flood volcanism associated with the Cascades arc and Basin and " +
                    "Range extension; the Medicine Lake Volcano and Lava Beds National Monument " +
                    "feature exceptional Holocene basalt flows, cinder cones, and lava tube " +
                    "systems. The pluvial lake basins (Goose Lake, Lower Klamath, Tule Lake, " +
                    "Surprise Valley) are remnants of much larger Pleistocene lake systems and " +
                    "remain alkaline-saline. Avifauna highlights: Anser caerulescens (snow " +
                    "goose), Branta canadensis, Anas acuta (northern pintail), and large " +
                    "wintering populations of Haliaeetus leucocephalus (bald eagle) feeding on " +
                    "the waterfowl concentrations. Antilocapra americana (pronghorn) and Bos " +
                    "bison (bison, extirpated, formerly seasonal visitor) are the historical " +
                    "ungulate fauna; Lepus californicus, Spermophilus beldingi, and Athene " +
                    "cunicularia (burrowing owl) characterise the small-mammal/raptor community. " +
                    "The plateau is the southernmost extent of the sage-grouse (Centrocercus " +
                    "urophasianus) range in California, with isolated lekking populations on " +
                    "the Oregon border."
    );

    @Override
    public String slug() {
        return "modoc-plateau";
    }

    @Override
    public String displayName() {
        return "Modoc Plateau";
    }

    @Override
    public Description description() {
        return DESCRIPTION;
    }
}
