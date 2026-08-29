package com.naturalist.plants;

/**
 * A rank's stored (discriminator, slug) pair, used only as a MyBatis parameter for the batched
 * {@code selectByRankKeys} row-value {@code IN} lookup. Fields are package-private and read by
 * MyBatis via {@code #{k.rank}} / {@code #{k.rankName}} (the same field access the DBOs use).
 */
final class PlantFeatureAssignmentRankKey {
    final String rank;
    final String rankName;

    PlantFeatureAssignmentRankKey(String rank, String rankName) {
        this.rank = rank;
        this.rankName = rankName;
    }
}
