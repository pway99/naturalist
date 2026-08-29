package com.naturalist.plants.cultivar;

import com.naturalist.fieldnotes.Description;
import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;
import com.naturalist.persistence.Fk;
import com.naturalist.plants.PlantSpeciesName;

import java.util.function.Consumer;

/**
 * Flat persistence view of {@link Cultivar} — a slug-identified {@code NamedEntity} whose only
 * owned value object is the four-level Durrell {@link Description} (flattened onto four NOT NULL
 * columns). Its one reference, the species FK, follows the name-carrying rule of
 * {@code docs/rdbms-key-management.md}: this DBO holds the referenced {@code PlantSpecies} slug in
 * {@link #plantName}, the mapper JOINs {@code plant_species} to project it on read, and
 * nested-selects the numeric {@code plant_species_id} from the slug on write. The three enums
 * store via {@code .name()} and reconstruct via {@code valueOf}; the nullable {@code fruit_type}
 * maps null↔null.
 *
 * <p>Fields are camelCase; MyBatis translates the snake_case columns across on read
 * ({@code common_name} → {@code commonName}); the projected {@code plant_name} alias maps to
 * {@link #plantName}.
 */
@DboSchema(table = "cultivar", primaryKey = "id", unique = {"name"},
           foreignKeys = @Fk(columns = "plant_species_id", references = "plant_species(id)"),
           entity = Cultivar.class)
final class CultivarDbo implements Dbo {
    Long id;              // numeric identity anchor (BIGINT IDENTITY); null before insert.
    String name;
    String plantName;     // referenced PlantSpecies slug: JOIN projection (read) / nested-select key (write)
    String commonName;
    String varietyType;
    String fruitType;     // nullable
    String seedSavingPolicy;
    String seedSource;    // nullable
    String gardenNotes;   // nullable
    String descriptionPreschool;
    String descriptionElementary;
    String descriptionSecondary;
    String descriptionUniversity;

    static CultivarDbo from(Cultivar c) {
        CultivarDbo d = new CultivarDbo();
        d.name = c.name().value();
        d.plantName = c.plantName().value();
        d.commonName = c.commonName();
        d.varietyType = c.varietyType().name();
        d.fruitType = c.fruitType() == null ? null : c.fruitType().name();
        d.seedSavingPolicy = c.seedSavingPolicy().name();
        d.seedSource = c.seedSource();
        d.gardenNotes = c.gardenNotes();
        Description desc = c.description();
        d.descriptionPreschool = desc.preschool();
        d.descriptionElementary = desc.elementary();
        d.descriptionSecondary = desc.secondary();
        d.descriptionUniversity = desc.university();
        Observer.forClass(CultivarDbo.class)
                .arguments("from", i -> i.observable(d, "dbo"))
                .throwWhenInvalid();
        return d;
    }

    Cultivar toEntity() {
        return new Cultivar(
                CultivarName.of(name),
                PlantSpeciesName.of(plantName),
                commonName,
                new Description(
                        descriptionPreschool,
                        descriptionElementary,
                        descriptionSecondary,
                        descriptionUniversity),
                VarietyType.valueOf(varietyType),
                fruitType == null ? null : FruitType.valueOf(fruitType),
                SeedSavingPolicy.valueOf(seedSavingPolicy),
                seedSource,
                gardenNotes);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .notNull(name, "name").kebabFormat(name, "name").maxLength(name, 64, "name")
                .notNull(plantName, "plantName").kebabFormat(plantName, "plantName")
                .notBlank(commonName, "commonName").maxLength(commonName, 128, "commonName")
                .notBlank(varietyType, "varietyType").maxLength(varietyType, 32, "varietyType")
                .notBlank(seedSavingPolicy, "seedSavingPolicy").maxLength(seedSavingPolicy, 32, "seedSavingPolicy")
                .notBlank(descriptionPreschool, "descriptionPreschool")
                .notBlank(descriptionElementary, "descriptionElementary")
                .notBlank(descriptionSecondary, "descriptionSecondary")
                .notBlank(descriptionUniversity, "descriptionUniversity");
    }
}
