package com.naturalist.plants.cultivar;

import com.naturalist.ddd.NamedEntity;
import com.naturalist.fieldnotes.Description;
import com.naturalist.observability.Constraints;
import com.naturalist.plants.PlantSpeciesName;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * A named variety within a plant species, carrying the breeding status,
 * fruit morphology, and seed saving policy specific to this selection.
 * <p>
 * {@code Cultivar} operates below the species level: the {@code PlantSpecies} catalog
 * entry for {@code "tomato"} (Solanum lycopersicum) is the species; Amish Paste,
 * Italian Pear (Nick's), Sungold Cherry, and San Marzano are cultivars within
 * that species. Each cultivar has a stable slug name used for cross-domain
 * references in planting events and harvest records.
 * <p>
 * {@link VarietyType} determines genetic behaviour under seed saving.
 * {@link FruitType} determines culinary processing suitability for fruiting
 * cultivars and is nullable — non-fruiting cultivars (leaf herbs such as
 * basil and parsley) carry {@code null}. The enum itself is currently
 * tomato-specific (PASTE, CHERRY, SLICER, BEEFSTEAK) despite its generic
 * name; non-tomato fruit-bearing cultivars added to the catalog should
 * also use {@code null} until the enum is broadened.
 * {@link SeedSavingPolicy} encodes the management decision derived from
 * variety type and heritage significance.
 * <p>
 * The species cross-reference ({@code plantName}) is a soft FK into the
 * {@code PlantSpecies} catalog via {@link PlantSpeciesName} — no compile-time dependency
 * on any other domain module.
 * <p>
 * <b>Oak Vista 2026 cultivars:</b>
 * <ul>
 *   <li>{@code "amish-paste"} — UNKNOWN variety type (The PlantSpecies Barn, Chico).
 *       12 plants, paste type. Evaluating as primary sauce variety for 90-quart
 *       annual target. Seed provenance unconfirmed.</li>
 *   <li>{@code "italian-pear-nicks"} — OPEN_POLLINATED heirloom, 50+ year
 *       family lineage. SAVE_ANNUALLY without exception.
 *       Founding generation of Chico adaptation program.</li>
 *   <li>{@code "sungold-cherry"} — HYBRID_F1 commercial variety. 1 plant,
 *       cherry type, fresh eating. Do not save seed.</li>
 *   <li>{@code "san-marzano-f2"} — HYBRID_F2 from saved F1 seed. 3 remaining
 *       plants, struggling. Do not save seed.</li>
 * </ul>
 */
public record Cultivar(
        CultivarName name,
        PlantSpeciesName plantName,
        String commonName,
        Description description,
        VarietyType varietyType,
        @Nullable FruitType fruitType,
        SeedSavingPolicy seedSavingPolicy,
        @Nullable String seedSource,
        @Nullable String gardenNotes
) implements NamedEntity<CultivarName> {

    /**
     * Whether this cultivar is safe for seed saving based on its breeding status.
     * Open-pollinated varieties breed true; F1 hybrids and unknowns do not.
     */
    public boolean breedsTrueFromSeed() {
        return varietyType == VarietyType.OPEN_POLLINATED;
    }

    /**
     * Whether this cultivar requires mandatory annual seed saving — reserved
     * for irreplaceable heritage varieties with active lineage tracking.
     */
    public boolean requiresSeedSaving() {
        return seedSavingPolicy == SeedSavingPolicy.SAVE_ANNUALLY;
    }

    /**
     * Whether this cultivar is suitable for sauce production based on its
     * fruit morphology.
     */
    public boolean isSauceVariety() {
        return fruitType == FruitType.PASTE;
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityName(name, "name")
                .entityName(plantName, "plantName")
                .notBlank(commonName, "commonName")
                .valueObject(description, "description")
                .notNull(varietyType, "varietyType")
                .notNull(seedSavingPolicy, "seedSavingPolicy");
    }
}
