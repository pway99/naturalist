package com.naturalist.plants;

import java.util.Locale;

/** Presentation helper: the human label for a rank ("Order", "Family"). Mirrors the insects RankLabel. */
public final class PlantRankLabel {
    private PlantRankLabel() {}

    public static String of(PlantRankName rankName) {
        String name = rankName.rank().name();               // e.g. "ORDER"
        return name.charAt(0) + name.substring(1).toLowerCase(Locale.ROOT);
    }
}
