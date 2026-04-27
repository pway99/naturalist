package com.naturalist.biogeography;

import com.naturalist.fieldnotes.Description;

public record SouthernCascades() implements Bioregion {

    private static final Description DESCRIPTION = new Description(
            "The Southern Cascades are mountains with snow on top, big pine forests, and " +
                    "volcanoes that once erupted long ago. Lassen Peak still has steaming hot " +
                    "springs and bubbling mud pots. Bears, deer, and woodpeckers all live here.",
            "The Southern Cascades are the southern end of a long mountain range that runs all " +
                    "the way up into Canada. The mountains here are volcanoes — Lassen Peak " +
                    "erupted just over a hundred years ago. Conifer forests of fir and pine cover " +
                    "the slopes, with mountain meadows full of wildflowers and lakes carved by " +
                    "glaciers. Winters bring deep snow; summers are warm and dry.",
            "The Southern Cascades are the southernmost extension of the Cascade volcanic arc, " +
                    "centred on Lassen Peak (a Pleistocene–Holocene plug-dome volcano that last " +
                    "erupted 1914–1917). Mixed conifer forest dominates the mid-elevation slopes " +
                    "(Pinus ponderosa, Pinus jeffreyi, Abies concolor, Calocedrus decurrens), " +
                    "transitioning to red fir (Abies magnifica) and mountain hemlock (Tsuga " +
                    "mertensiana) at higher elevations. Subalpine meadows, glacially carved " +
                    "lakes, and active geothermal features (fumaroles, mud pots, hot springs) " +
                    "characterise the high country. Mediterranean–montane climate with deep winter " +
                    "snowpack supplies the headwaters of the Pit, Feather, and McCloud rivers.",
            "The Southern Cascades correspond to the southern portion of EPA Level III Ecoregion " +
                    "4 (Cascades) and the western edge of Ecoregion 9 (Eastern Cascades Slopes " +
                    "and Foothills). Geologically, the region is dominated by Pliocene–Holocene " +
                    "andesitic and dacitic volcanism of the Cascade arc, with the Lassen volcanic " +
                    "centre representing the southernmost active vent. Forest community zonation " +
                    "follows the standard Sierra–Cascade elevational sequence: ponderosa pine " +
                    "(P. ponderosa) and white fir (Abies concolor) at 1200–1800 m; red fir (Abies " +
                    "magnifica) and Jeffrey pine (Pinus jeffreyi) at 1800–2400 m; mountain " +
                    "hemlock (Tsuga mertensiana), whitebark pine (Pinus albicaulis), and " +
                    "subalpine meadow above 2400 m. Vertebrate fauna includes Ursus americanus, " +
                    "Odocoileus hemionus columbianus, Martes caurina, and the disjunct Sierra " +
                    "Nevada red fox (Vulpes vulpes necator). Active hydrothermal systems support " +
                    "thermophilic microbial communities at Bumpass Hell and the Devils Kitchen."
    );

    @Override
    public String slug() {
        return "southern-cascades";
    }

    @Override
    public String displayName() {
        return "Southern Cascades";
    }

    @Override
    public Description description() {
        return DESCRIPTION;
    }
}
