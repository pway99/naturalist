package com.naturalist.plants;

import com.naturalist.biogeography.Bioregion;
import com.naturalist.fieldnotes.CommonName;
import com.naturalist.fieldnotes.Description;
import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;
import com.naturalist.persistence.Fk;
import com.naturalist.taxonomy.TaxonomicSpecies;

import java.util.Set;
import java.util.function.Consumer;

/**
 * Parent row of the {@link PlantSpecies} rank record — the bottom of the plant Linnaean chain. The
 * upward reference to the parent {@link PlantGenus} is stored as {@code genus_id} (FK-enforced); the
 * DBO carries the genus's {@code name} in {@link #genusName} for the mapper's JOIN projection (read)
 * and nested-select (write). {@code growthHabit} and {@code lifeCycle} are enum names on
 * {@code varchar(32)} columns. The two multi-valued members become child tables:
 * {@link PlantSpeciesCommonNameDbo} and {@link PlantSpeciesNativeBioregionDbo}.
 */
@DboSchema(table = "plant_species", primaryKey = "id", unique = {"name"},
           foreignKeys = @Fk(columns = "genus_id", references = "plant_genus(id)"),
           entity = PlantSpecies.class)
final class PlantSpeciesDbo implements Dbo {
    Long id;              // numeric identity anchor (BIGINT IDENTITY); null before insert.
    String name;
    String genusName;     // JOIN projection (read) / nested-select key (write); stored as genus_id
    String epithet;
    String growthHabit;
    String lifeCycle;
    String descriptionPreschool;
    String descriptionElementary;
    String descriptionSecondary;
    String descriptionUniversity;

    static PlantSpeciesDbo from(PlantSpecies s) {
        PlantSpeciesDbo d = new PlantSpeciesDbo();
        d.name = s.name().value();
        d.genusName = s.genusName().value();
        d.epithet = s.epithet().value();
        d.growthHabit = s.growthHabit().name();
        d.lifeCycle = s.lifeCycle().name();
        Description desc = s.description();
        d.descriptionPreschool = desc.preschool();
        d.descriptionElementary = desc.elementary();
        d.descriptionSecondary = desc.secondary();
        d.descriptionUniversity = desc.university();
        Observer.forClass(PlantSpeciesDbo.class)
                .arguments("from", i -> i.observable(d, "dbo"))
                .throwWhenInvalid();
        return d;
    }

    PlantSpecies toEntity(Set<Bioregion> nativeBioregions, Set<CommonName> commonNames) {
        return new PlantSpecies(
                PlantSpeciesName.of(name),
                PlantGenusName.of(genusName),
                TaxonomicSpecies.of(epithet),
                new Description(
                        descriptionPreschool,
                        descriptionElementary,
                        descriptionSecondary,
                        descriptionUniversity),
                GrowthHabit.valueOf(growthHabit),
                LifeCycle.valueOf(lifeCycle),
                nativeBioregions,
                commonNames);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .notNull(name, "name").kebabFormat(name, "name").maxLength(name, 64, "name")
                .notNull(genusName, "genusName").kebabFormat(genusName, "genusName")
                .notBlank(epithet, "epithet").maxLength(epithet, 128, "epithet")
                .notBlank(growthHabit, "growthHabit").maxLength(growthHabit, 32, "growthHabit")
                .notBlank(lifeCycle, "lifeCycle").maxLength(lifeCycle, 32, "lifeCycle")
                .notBlank(descriptionPreschool, "descriptionPreschool")
                .notBlank(descriptionElementary, "descriptionElementary")
                .notBlank(descriptionSecondary, "descriptionSecondary")
                .notBlank(descriptionUniversity, "descriptionUniversity");
    }
}
