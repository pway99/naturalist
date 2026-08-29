package com.naturalist.library;

import com.naturalist.fieldnotes.Description;
import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;

import java.util.function.Consumer;

/**
 * Flat persistence view of {@link Concept} — a slug-identified {@code NamedEntity} whose only
 * owned value object is the four-level Durrell {@link Description}. That VO flattens onto four
 * NOT NULL columns ({@code description_preschool} … {@code description_university}); everything
 * else is a scalar, so the whole aggregate maps one-to-one onto a single {@code concept} row.
 * Fields are camelCase; MyBatis translates the snake_case columns across on read.
 */
@DboSchema(table = "concept", primaryKey = "id", unique = {"name"}, entity = Concept.class)
final class ConceptDbo implements Dbo {
    Long id;              // numeric identity anchor (BIGINT IDENTITY); null before insert.
    String name;
    String title;
    String descriptionPreschool;
    String descriptionElementary;
    String descriptionSecondary;
    String descriptionUniversity;

    static ConceptDbo from(Concept c) {
        ConceptDbo d = new ConceptDbo();
        d.name = c.name().value();
        d.title = c.title();
        Description desc = c.description();
        d.descriptionPreschool = desc.preschool();
        d.descriptionElementary = desc.elementary();
        d.descriptionSecondary = desc.secondary();
        d.descriptionUniversity = desc.university();
        Observer.forClass(ConceptDbo.class)
                .arguments("from", i -> i.observable(d, "dbo"))
                .throwWhenInvalid();
        return d;
    }

    Concept toEntity() {
        return new Concept(
                ConceptName.of(name),
                title,
                new Description(
                        descriptionPreschool,
                        descriptionElementary,
                        descriptionSecondary,
                        descriptionUniversity));
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .notNull(name, "name").kebabFormat(name, "name").maxLength(name, 64, "name")
                .notBlank(title, "title")
                .notBlank(descriptionPreschool, "descriptionPreschool")
                .notBlank(descriptionElementary, "descriptionElementary")
                .notBlank(descriptionSecondary, "descriptionSecondary")
                .notBlank(descriptionUniversity, "descriptionUniversity");
    }
}
