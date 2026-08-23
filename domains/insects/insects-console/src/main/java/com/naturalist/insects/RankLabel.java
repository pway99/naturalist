package com.naturalist.insects;

import com.naturalist.insects.InsectRankName;

import java.util.Locale;

/** Presentation helper: the human label for a rank ("Order", "Family"). */
public final class RankLabel {
    private RankLabel() {}

    public static String of(InsectRankName rankName) {
        String name = rankName.rank().name();               // e.g. "ORDER"
        return name.charAt(0) + name.substring(1).toLowerCase(Locale.ROOT);
    }
}
