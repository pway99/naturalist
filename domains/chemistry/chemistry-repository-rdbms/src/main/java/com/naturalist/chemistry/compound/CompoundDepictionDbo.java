package com.naturalist.chemistry.compound;

import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;
import com.naturalist.persistence.Fk;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Persistence view of {@link CompoundDepiction} — the first surrogate-key entity to persist:
 * an {@code Entity<DepictionId>} whose UUIDv7 identity stores as a native {@code uuid} column.
 * Its reference to the compound is the DB-enforced numeric {@code compound_id}; the DBO carries
 * the compound's {@code name}, which the mapper JOINs to project on read and nested-selects
 * {@code compound.id} from on write — so no name is persisted on this row and nothing can drift.
 */
@DboSchema(table = "compound_depiction", primaryKey = "id",
           foreignKeys = @Fk(columns = "compound_id", references = "compound(id)"),
           entity = CompoundDepiction.class)
final class CompoundDepictionDbo implements Dbo {
    UUID id;
    String compoundName;  // JOIN projection (read) / nested-select key (write); not a stored column
    String smiles;
    String note;

    static CompoundDepictionDbo from(CompoundDepiction depiction) {
        CompoundDepictionDbo d = new CompoundDepictionDbo();
        d.id = depiction.id().value();
        d.compoundName = depiction.compoundName().value();
        d.smiles = depiction.smiles();
        d.note = depiction.note();
        Observer.forClass(CompoundDepictionDbo.class)
                .arguments("from", i -> i.observable(d, "dbo"))
                .throwWhenInvalid();
        return d;
    }

    CompoundDepiction toEntity() {
        return new CompoundDepiction(
                DepictionId.of(id),
                CompoundName.of(compoundName),
                smiles,
                note);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .notNull(id, "id")
                .notNull(compoundName, "compoundName").kebabFormat(compoundName, "compoundName")
                .notBlank(smiles, "smiles").maxLength(smiles, 512, "smiles")
                .notBlank(note, "note");
    }
}
