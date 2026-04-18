package com.naturalist.chemistry.reaction;

import com.naturalist.chemistry.compound.CompoundName;
import com.naturalist.ddd.CatalogEntity;
import com.naturalist.observability.Constraints;

import java.util.List;
import java.util.function.Consumer;

/**
 * A documented chemical reaction with its reactants, products,
 * conditions, and significance to the Oak Vista management context.
 * <p>
 * Reaction instances are loaded from reactions.json at startup.
 * This class defines the schema — the JSON defines specific reactions.
 * <p>
 * No specific reactions (gypsum dissolution, sulfur oxidation etc)
 * are hardcoded here.
 */
public record ReactionProfile(
        ReactionId id,
        ReactionName name,
        String title,
        String equation,
        List<CompoundName> reactants,
        List<CompoundName> products,
        ReactionType type,
        ReactionConditions conditions,
        String significance
) implements CatalogEntity<ReactionId, ReactionName> {

    @Override
    public ReactionProfile withId(ReactionId id) {
        return new ReactionProfile(id, name, title, equation, reactants, products, type, conditions, significance);
    }

    public boolean involves(CompoundName compoundName) {
        return reactants.contains(compoundName) || products.contains(compoundName);
    }

    public boolean isBiologicallyCatalyzed() {
        return conditions.requiresBiologicalCatalyst();
    }

    public boolean isTemperatureDependent() {
        return conditions.isTemperatureDependent();
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityId(id, "id")
                .entityName(name, "name")
                .notBlank(title, "title");
    }
}
