package com.naturalist.chemistry.compound;

import com.naturalist.chemistry.element.PeriodicElement;
import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;
import com.naturalist.persistence.Fk;

import java.util.function.Consumer;

/**
 * Child row for one member of a compound's {@code Set<PeriodicElement>}. The stored reference is
 * the numeric {@code compound_id} (FK-enforced); the DBO carries the compound's {@code name} for
 * the mapper's JOIN projection (read) and nested-select (write). The constituent elements are the
 * {@link PeriodicElement} enum (IUPAC symbols) — not the {@code Element} entity — so the symbol
 * column has NO foreign key to the {@code element} table.
 */
@DboSchema(table = "compound_constituent_element", primaryKey = "compound_id,element_symbol",
           foreignKeys = @Fk(columns = "compound_id", references = "compound(id)"),
           entity = Compound.class)
final class CompoundConstituentElementDbo implements Dbo {
    String compoundName;  // JOIN projection (read) / nested-select key (write); not a stored column
    String elementSymbol;

    static CompoundConstituentElementDbo from(CompoundName compoundName, PeriodicElement element) {
        CompoundConstituentElementDbo d = new CompoundConstituentElementDbo();
        d.compoundName = compoundName.value();
        d.elementSymbol = element.name();
        Observer.forClass(CompoundConstituentElementDbo.class)
                .arguments("from", i -> i.observable(d, "dbo"))
                .throwWhenInvalid();
        return d;
    }

    PeriodicElement toElement() {
        return PeriodicElement.valueOf(elementSymbol);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .notNull(compoundName, "compoundName").kebabFormat(compoundName, "compoundName")
                .notBlank(elementSymbol, "elementSymbol").maxLength(elementSymbol, 3, "elementSymbol");
    }
}
