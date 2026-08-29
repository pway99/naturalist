package com.naturalist.plants;

/**
 * A subject's stored (rank, slug) pair, used only as a MyBatis parameter for the batched
 * {@code selectByNaturalistAndSubjects} row-value {@code IN} lookup. Fields are package-private and
 * read by MyBatis via {@code #{k.rank}} / {@code #{k.name}}.
 */
final class ObservationSubjectKey {
    final String rank;
    final String name;

    ObservationSubjectKey(String rank, String name) {
        this.rank = rank;
        this.name = name;
    }
}
