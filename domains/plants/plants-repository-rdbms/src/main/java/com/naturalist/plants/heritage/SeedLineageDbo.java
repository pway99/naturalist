package com.naturalist.plants.heritage;

import com.naturalist.fieldnotes.Description;
import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;
import com.naturalist.persistence.Fk;
import com.naturalist.plants.cultivar.CultivarName;

import java.util.function.Consumer;

/**
 * Flat persistence view of {@link SeedLineage} — a slug-identified {@code NamedEntity}. Two owned
 * value objects flatten onto columns here: the four-level Durrell {@link Description} and the
 * {@link Provenance} (four {@code provenance_*} columns). Its one reference, the cultivar FK,
 * follows the name-carrying rule of {@code docs/rdbms-key-management.md}: this DBO holds the
 * referenced {@code Cultivar} slug in {@link #cultivarName}, the mapper JOINs {@code cultivar} to
 * project it on read, and nested-selects the numeric {@code cultivar_id} from the slug on write.
 *
 * <p>Fields are camelCase; MyBatis translates the snake_case columns across on read
 * ({@code provenance_originator} → {@code provenanceOriginator}); the projected {@code cultivar_name}
 * alias maps to {@link #cultivarName}.
 */
@DboSchema(table = "seed_lineage", primaryKey = "id", unique = {"name"},
           foreignKeys = @Fk(columns = "cultivar_id", references = "cultivar(id)"),
           entity = SeedLineage.class)
final class SeedLineageDbo implements Dbo {
    Long id;              // numeric identity anchor (BIGINT IDENTITY); null before insert.
    String name;
    String cultivarName;  // referenced Cultivar slug: JOIN projection (read) / nested-select key (write)
    int adaptationStartYear;
    String selectionCriteria;   // nullable
    String notes;               // nullable
    String provenanceOriginator;
    String provenanceOriginLocation;
    int provenanceEstimatedGenerations;
    String provenanceSourceNotes;   // nullable
    String descriptionPreschool;
    String descriptionElementary;
    String descriptionSecondary;
    String descriptionUniversity;

    static SeedLineageDbo from(SeedLineage s) {
        SeedLineageDbo d = new SeedLineageDbo();
        d.name = s.name().value();
        d.cultivarName = s.cultivarName().value();
        d.adaptationStartYear = s.adaptationStartYear();
        d.selectionCriteria = s.selectionCriteria();
        d.notes = s.notes();
        Provenance p = s.provenance();
        d.provenanceOriginator = p.originator();
        d.provenanceOriginLocation = p.originLocation();
        d.provenanceEstimatedGenerations = p.estimatedGenerations();
        d.provenanceSourceNotes = p.sourceNotes();
        Description desc = s.description();
        d.descriptionPreschool = desc.preschool();
        d.descriptionElementary = desc.elementary();
        d.descriptionSecondary = desc.secondary();
        d.descriptionUniversity = desc.university();
        Observer.forClass(SeedLineageDbo.class)
                .arguments("from", i -> i.observable(d, "dbo"))
                .throwWhenInvalid();
        return d;
    }

    SeedLineage toEntity() {
        return new SeedLineage(
                SeedLineageName.of(name),
                CultivarName.of(cultivarName),
                new Provenance(
                        provenanceOriginator,
                        provenanceOriginLocation,
                        provenanceEstimatedGenerations,
                        provenanceSourceNotes),
                new Description(
                        descriptionPreschool,
                        descriptionElementary,
                        descriptionSecondary,
                        descriptionUniversity),
                adaptationStartYear,
                selectionCriteria,
                notes);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .notNull(name, "name").kebabFormat(name, "name").maxLength(name, 64, "name")
                .notNull(cultivarName, "cultivarName").kebabFormat(cultivarName, "cultivarName")
                .notBlank(provenanceOriginator, "provenanceOriginator")
                .maxLength(provenanceOriginator, 128, "provenanceOriginator")
                .notBlank(provenanceOriginLocation, "provenanceOriginLocation")
                .maxLength(provenanceOriginLocation, 128, "provenanceOriginLocation")
                .notBlank(descriptionPreschool, "descriptionPreschool")
                .notBlank(descriptionElementary, "descriptionElementary")
                .notBlank(descriptionSecondary, "descriptionSecondary")
                .notBlank(descriptionUniversity, "descriptionUniversity");
    }
}
