package com.naturalist.library;

import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;

import java.util.function.Consumer;

/**
 * Flat persistence view of {@link GlossaryTerm} — a slug-identified {@code NamedEntity} with no
 * owned value objects, so it maps one-to-one onto a single {@code glossary_term} row. {@code name}
 * is the slug, {@code term} the display form, {@code definition} the body, and {@code example} an
 * optional usage sentence (nullable). Fields are camelCase; MyBatis translates snake_case across.
 */
@DboSchema(table = "glossary_term", primaryKey = "id", unique = {"name"}, entity = GlossaryTerm.class)
final class GlossaryTermDbo implements Dbo {
    Long id;              // numeric identity anchor (BIGINT IDENTITY); null before insert.
    String name;
    String term;
    String definition;
    String example;       // nullable — not every term carries a usage example

    static GlossaryTermDbo from(GlossaryTerm t) {
        GlossaryTermDbo d = new GlossaryTermDbo();
        d.name = t.name().value();
        d.term = t.term();
        d.definition = t.definition();
        d.example = t.example();
        Observer.forClass(GlossaryTermDbo.class)
                .arguments("from", i -> i.observable(d, "dbo"))
                .throwWhenInvalid();
        return d;
    }

    GlossaryTerm toEntity() {
        return new GlossaryTerm(
                GlossaryTermName.of(name),
                term,
                definition,
                example);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .notNull(name, "name").kebabFormat(name, "name").maxLength(name, 64, "name")
                .notBlank(term, "term").maxLength(term, 128, "term")
                .notBlank(definition, "definition");
    }
}
