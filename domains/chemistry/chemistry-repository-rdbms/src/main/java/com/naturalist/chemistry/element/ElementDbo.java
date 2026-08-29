package com.naturalist.chemistry.element;

import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;

import java.math.BigDecimal;
import java.util.function.Consumer;

/**
 * Flat persistence view of {@link Element} — a slug-identified {@code NamedEntity}
 * with no owned value objects, so it maps one-to-one onto a single {@code element} row
 * exactly as the naturalist DBOs do. The {@link AtomicWeight} numeric value-type is
 * unwrapped to a bare {@code BigDecimal} column. Fields are camelCase; MyBatis translates
 * the snake_case columns across on read ({@code atomic_weight} -> {@code atomicWeight}).
 */
@DboSchema(table = "element", primaryKey = "id", unique = {"name"}, entity = Element.class)
final class ElementDbo implements Dbo {
    Long id;              // numeric identity anchor (BIGINT IDENTITY); null before insert.
    String name;
    String symbol;
    BigDecimal atomicWeight;
    String ionicForm;
    Integer ionicCharge;

    static ElementDbo from(Element e) {
        ElementDbo d = new ElementDbo();
        d.name = e.name().value();
        d.symbol = e.symbol();
        d.atomicWeight = e.atomicWeight().value();
        d.ionicForm = e.ionicForm();
        d.ionicCharge = e.ionicCharge();
        Observer.forClass(ElementDbo.class)
                .arguments("from", i -> i.observable(d, "dbo"))
                .throwWhenInvalid();
        return d;
    }

    Element toEntity() {
        return new Element(
                ElementName.of(name),
                symbol,
                AtomicWeight.of(atomicWeight),
                ionicForm,
                ionicCharge);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .notNull(name, "name").kebabFormat(name, "name").maxLength(name, 64, "name")
                .notBlank(symbol, "symbol").maxLength(symbol, 8, "symbol")
                .notNull(atomicWeight, "atomicWeight")
                .notBlank(ionicForm, "ionicForm").maxLength(ionicForm, 16, "ionicForm")
                .notNull(ionicCharge, "ionicCharge");
    }
}
