package com.naturalist.insects;

/**
 * A stored (rank, slug) pair, used only as a MyBatis parameter for the batched row-value {@code IN}
 * lookups ({@code selectByNaturalistAndSubjects}, {@code selectByParentNames}). Fields are
 * package-private and read by MyBatis via {@code #{k.rank}} / {@code #{k.name}}.
 */
final class InsectRankKey {
    final String rank;
    final String name;

    InsectRankKey(String rank, String name) {
        this.rank = rank;
        this.name = name;
    }
}
