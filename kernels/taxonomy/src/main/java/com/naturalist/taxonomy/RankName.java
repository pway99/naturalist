package com.naturalist.taxonomy;

/**
 * Organism-agnostic contract for a typed taxonomic-rank name: the slug it carries and the
 * {@link LinealRank} rung it occupies. Each organism domain's sealed rank-name interface
 * (e.g. {@code InsectRankName}, {@code PlantRankName}) extends this so shared types — the
 * observation kernel's {@code OrganismObservation.subject} — can reference any domain's
 * rank name without depending on that domain. Non-sealed by necessity: permits live in
 * domain packages a sealed type here could not enumerate.
 */
public interface RankName {
    String value();
    LinealRank rank();
}
