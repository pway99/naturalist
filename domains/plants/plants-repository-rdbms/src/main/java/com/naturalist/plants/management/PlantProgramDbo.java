package com.naturalist.plants.management;

import com.naturalist.fieldnotes.Description;
import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;
import com.naturalist.plants.PlantRankName;
import com.naturalist.taxonomy.LinealRank;

import java.util.function.Consumer;

/**
 * Flat persistence view of {@link PlantProgram} — a slug-identified {@code NamedEntity} whose only
 * owned value object is the four-level Durrell {@link Description} (flattened onto four NOT NULL
 * columns). Its {@code plantName} is a <em>polymorphic cross-rank</em> reference: a
 * {@link PlantRankName} can name a taxon at any of the four plant ranks, so it is stored as two
 * plain columns — {@code plant_rank} (the {@link LinealRank}) and {@code plant_name} (the slug) —
 * with <b>no FK</b> (the target rank varies row to row). It is reconstructed via
 * {@code PlantRankName.of(slug, LinealRank.valueOf(rank))}, the same shape the fixtures and console
 * use, rather than joined.
 *
 * <p>The {@code constraint} component maps to column {@code program_constraint} — {@code constraint}
 * is a reserved SQL word — so this DBO carries it as {@link #programConstraint} and the mapper
 * reads/writes {@code entity.constraint()} across it.
 *
 * <p>Fields are camelCase; MyBatis translates the snake_case columns across on read
 * ({@code plant_rank} → {@code plantRank}, {@code program_constraint} → {@code programConstraint}).
 */
@DboSchema(table = "plant_program", primaryKey = "id", unique = {"name"}, entity = PlantProgram.class)
final class PlantProgramDbo implements Dbo {
    Long id;              // numeric identity anchor (BIGINT IDENTITY); null before insert.
    String name;
    String plantRank;     // LinealRank.name() of the referenced taxon's rank
    String plantName;     // the referenced taxon's slug, at that rank (no FK: rank varies)
    String programConstraint;   // nullable; column program_constraint ('constraint' is reserved)
    String notes;               // nullable
    String descriptionPreschool;
    String descriptionElementary;
    String descriptionSecondary;
    String descriptionUniversity;

    static PlantProgramDbo from(PlantProgram p) {
        PlantProgramDbo d = new PlantProgramDbo();
        d.name = p.name().value();
        d.plantRank = p.plantName().rank().name();
        d.plantName = p.plantName().value();
        d.programConstraint = p.constraint();
        d.notes = p.notes();
        Description desc = p.description();
        d.descriptionPreschool = desc.preschool();
        d.descriptionElementary = desc.elementary();
        d.descriptionSecondary = desc.secondary();
        d.descriptionUniversity = desc.university();
        Observer.forClass(PlantProgramDbo.class)
                .arguments("from", i -> i.observable(d, "dbo"))
                .throwWhenInvalid();
        return d;
    }

    PlantProgram toEntity() {
        return new PlantProgram(
                PlantProgramName.of(name),
                PlantRankName.of(plantName, LinealRank.valueOf(plantRank)),
                new Description(
                        descriptionPreschool,
                        descriptionElementary,
                        descriptionSecondary,
                        descriptionUniversity),
                programConstraint,
                notes);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .notNull(name, "name").kebabFormat(name, "name").maxLength(name, 64, "name")
                .notBlank(plantRank, "plantRank").maxLength(plantRank, 16, "plantRank")
                .notNull(plantName, "plantName").kebabFormat(plantName, "plantName").maxLength(plantName, 64, "plantName")
                .notBlank(descriptionPreschool, "descriptionPreschool")
                .notBlank(descriptionElementary, "descriptionElementary")
                .notBlank(descriptionSecondary, "descriptionSecondary")
                .notBlank(descriptionUniversity, "descriptionUniversity");
    }
}
