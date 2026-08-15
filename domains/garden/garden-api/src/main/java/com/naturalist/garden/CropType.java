package com.naturalist.garden;

import com.naturalist.ddd.AggregateRoot;
import com.naturalist.ddd.NamedEntity;
import com.naturalist.observability.Constraints;
import com.naturalist.plants.PlantName;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * A crop <em>type</em> — the agronomic category at which requirements are published and planning
 * decisions are made: {@code tomato}, {@code lettuce}, {@code basil}. The root of the
 * {@link GardenPlan} aggregate.
 * <p>
 * <b>The inclusion test.</b> A type belongs in this catalog if a laboratory or an extension
 * service would publish a requirement table for it. Tomato yes; Amish Paste no;
 * <em>Solanum lycopersicum</em> not usually — they publish for "tomato". That test is what keeps
 * the catalog from drifting into taxonomy on one side or inventory on the other, and it is not
 * arbitrary: FGL's March 2026 reports are headed "TOMATO SOIL ANALYSIS" and carry a single tomato
 * optimum panel, which is exactly the granularity {@code LabAnalysisInfo.cropType} points at.
 * <p>
 * <b>A type, not an instance.</b> "The 2026 backyard tomato crop" is a season's growing — derive
 * it from the {@link Planting}s of this type in that zone over that period. It is deliberately not
 * modelled as an entity yet: it would be a grouping with nothing to carry until harvest and yield
 * exist to hang on it.
 * <p>
 * <b>Identity only.</b> No requirements live here. What a tomato needs is published by a source,
 * revised over time, and disagreed about between sources, so it belongs to {@code CropProfile}
 * keyed by {@code (source, type, revision)}. This mirrors soil's central rule — a measurement never
 * carries its target — applied in the other direction: a target never masquerades as identity.
 * <p>
 * <b>Not a taxon.</b> {@code plant} is an optional soft reference to the plants domain and nothing
 * more. The concepts are many-to-many — one type spans several species, one species appears as
 * several types — so a mandatory link would be false in both directions. Null is a normal answer:
 * a gardener knows they are growing lettuce without deciding which <em>Lactuca</em> it is.
 * <p>
 * <b>Varieties are not here either.</b> Amish Paste and Nick's Italian Pear are cultivars, and the
 * plants domain owns them with their breeding status, fruit type and seed-saving policy. A cultivar
 * sits on the horticultural axis and a crop type on the agronomic one; the two meet on a
 * {@link Planting}, which is where a real row of mixed varieties is recorded.
 */
@AggregateRoot
public record CropType(
        CropTypeName name,
        @Nullable PlantName plant
) implements NamedEntity<CropTypeName> {

    /** Whether this type has been tied to a botanical species. */
    public boolean isBotanicallyIdentified() {
        return plant != null;
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityName(name, "name")
                .entityNameOrNull(plant, "plant");
    }
}
