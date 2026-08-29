package com.naturalist.library;

/**
 * A subject's three stored slugs, used only as a MyBatis parameter for the batched
 * {@code selectBySubjects} row-value {@code IN} lookup. Fields are package-private and read by
 * MyBatis via {@code #{k.subjectDomain}} etc. (the same field access the DBOs use).
 */
final class CitationSubjectKey {
    final String subjectDomain;
    final String subjectRank;
    final String subjectName;

    CitationSubjectKey(String subjectDomain, String subjectRank, String subjectName) {
        this.subjectDomain = subjectDomain;
        this.subjectRank = subjectRank;
        this.subjectName = subjectName;
    }
}
