package com.naturalist.plants.phytochemistry;

import com.naturalist.chemistry.compound.CompoundName;
import com.naturalist.fieldnotes.Description;
import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;
import com.naturalist.plants.PlantRankName;
import com.naturalist.plants.phytochemistry.role.PhytochemicalRole;
import com.naturalist.taxonomy.LinealRank;

import java.util.Set;
import java.util.function.Consumer;

/**
 * Parent row of the {@link PhytochemicalConstituent} link entity. The 1:1 owned
 * {@link Description} flattens onto four NOT NULL columns; the two multi-valued members
 * ({@code Set<PhytochemicalRole>}, {@code Set<PlantTissue>}) become child tables
 * ({@link PhytochemicalConstituentRoleDbo}, {@link PhytochemicalConstituentTissueDbo}),
 * so the adapter loads the child rows and passes them to {@link #toEntity(Set, Set)}.
 *
 * <p>Both cross-reference slugs are plain columns with NO FK: {@code plant_name} is a
 * polymorphic rank reference (order/family/genus/species — four tables, so no single FK
 * target) split into {@code plant_rank} (the {@link LinealRank} rung) + {@code plant_name}
 * (the slug); {@code compound_name} is the chemistry {@code Compound} slug, cross-domain.
 * The {@code PhytochemicalConstituentName} slug encodes both sides of the link and is up to
 * 129 chars ({@code <plant-slug>-<compound-slug>}), wider than the usual 64.
 *
 * <p>Fields are camelCase; MyBatis translates the snake_case columns across on read
 * ({@code plant_rank} -> {@code plantRank}).
 */
@DboSchema(table = "phytochemical_constituent", primaryKey = "id", unique = {"name"},
           entity = PhytochemicalConstituent.class)
final class PhytochemicalConstituentDbo implements Dbo {
    Long id;              // numeric identity anchor (BIGINT IDENTITY); null before insert.
    String name;
    String plantRank;     // LinealRank name — ORDER|FAMILY|GENUS|SPECIES
    String plantName;     // polymorphic rank-reference slug (no FK)
    String compoundName;  // chemistry compound slug (no FK)
    String category;      // PhytochemicalCategory name
    String induction;     // InductionMode name
    String notes;         // nullable
    String descriptionPreschool;
    String descriptionElementary;
    String descriptionSecondary;
    String descriptionUniversity;

    static PhytochemicalConstituentDbo from(PhytochemicalConstituent c) {
        PhytochemicalConstituentDbo d = new PhytochemicalConstituentDbo();
        d.name = c.name().value();
        d.plantRank = c.plantName().rank().name();
        d.plantName = c.plantName().value();
        d.compoundName = c.compoundName().value();
        d.category = c.category().name();
        d.induction = c.induction().name();
        d.notes = c.notes();
        Description desc = c.description();
        d.descriptionPreschool = desc.preschool();
        d.descriptionElementary = desc.elementary();
        d.descriptionSecondary = desc.secondary();
        d.descriptionUniversity = desc.university();
        Observer.forClass(PhytochemicalConstituentDbo.class)
                .arguments("from", i -> i.observable(d, "dbo"))
                .throwWhenInvalid();
        return d;
    }

    /** Reassembles the entity from this parent row plus its two child-table member sets. */
    PhytochemicalConstituent toEntity(Set<PhytochemicalRole> roles, Set<PlantTissue> tissues) {
        return new PhytochemicalConstituent(
                PhytochemicalConstituentName.of(name),
                PlantRankName.of(plantName, LinealRank.valueOf(plantRank)),
                CompoundName.of(compoundName),
                new Description(
                        descriptionPreschool,
                        descriptionElementary,
                        descriptionSecondary,
                        descriptionUniversity),
                PhytochemicalCategory.valueOf(category),
                roles,
                tissues,
                InductionMode.valueOf(induction),
                notes);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .notNull(name, "name").kebabFormat(name, "name").maxLength(name, 129, "name")
                .notBlank(plantRank, "plantRank").maxLength(plantRank, 16, "plantRank")
                .notNull(plantName, "plantName").kebabFormat(plantName, "plantName").maxLength(plantName, 64, "plantName")
                .notNull(compoundName, "compoundName").kebabFormat(compoundName, "compoundName")
                .maxLength(compoundName, 96, "compoundName")
                .notBlank(category, "category").maxLength(category, 48, "category")
                .notBlank(induction, "induction").maxLength(induction, 32, "induction")
                .notBlank(descriptionPreschool, "descriptionPreschool")
                .notBlank(descriptionElementary, "descriptionElementary")
                .notBlank(descriptionSecondary, "descriptionSecondary")
                .notBlank(descriptionUniversity, "descriptionUniversity");
    }
}
