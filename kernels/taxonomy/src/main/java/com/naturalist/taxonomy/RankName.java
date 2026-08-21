package com.naturalist.taxonomy;

import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observable;

import java.util.function.Consumer;

/**
 * Organism-agnostic contract for a typed taxonomic-rank name: the slug it carries and the
 * {@link LinealRank} rung it occupies. Each organism domain's sealed rank-name interface
 * (e.g. {@code InsectRankName}, {@code PlantRankName}) extends this so shared types — the
 * observation kernel's {@code OrganismObservation.subject} — can reference any domain's
 * rank name without depending on that domain. Non-sealed by necessity: permits live in
 * domain packages a sealed type here could not enumerate.
 *
 * <p>Extends {@link Observable} so rank-name collections can be validated with the
 * general {@link Constraints#observableCollection}. Every concrete permit is, at
 * runtime, also an {@code EntityName} (the domain-side rank-name sealed interfaces
 * each extend {@code EntityName}), so the default {@link #invariants()} below
 * delegates to {@link Constraints#identifier}, which dispatches to
 * {@code EntityName.isValid()} at runtime.
 */
public interface RankName extends Observable {
    String value();
    LinealRank rank();

    @Override
    default Consumer<? extends Constraints> invariants() {
        return i -> i.identifier(this, "rankName");
    }
}
