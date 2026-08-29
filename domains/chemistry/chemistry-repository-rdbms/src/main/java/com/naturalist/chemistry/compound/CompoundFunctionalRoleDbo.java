package com.naturalist.chemistry.compound;

import com.naturalist.chemistry.compound.role.FunctionalRole;
import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;
import com.naturalist.persistence.Fk;

import java.util.function.Consumer;

/**
 * Child row for one member of a compound's {@code Set<FunctionalRole>}. The stored reference is
 * the numeric {@code compound_id} (FK-enforced); the DBO carries the compound's {@code name} —
 * the mapper JOINs {@code compound} to project it on read and nested-selects {@code compound.id}
 * from it on write, so no name is persisted here and nothing can drift. {@link FunctionalRole}
 * is a sealed type persisted as its {@code kind()} discriminator.
 */
@DboSchema(table = "compound_functional_role", primaryKey = "compound_id,functional_role",
           foreignKeys = @Fk(columns = "compound_id", references = "compound(id)"),
           entity = Compound.class)
final class CompoundFunctionalRoleDbo implements Dbo {
    String compoundName;  // JOIN projection (read) / nested-select key (write); not a stored column
    String functionalRole;

    static CompoundFunctionalRoleDbo from(CompoundName compoundName, FunctionalRole role) {
        CompoundFunctionalRoleDbo d = new CompoundFunctionalRoleDbo();
        d.compoundName = compoundName.value();
        d.functionalRole = role.kind();
        Observer.forClass(CompoundFunctionalRoleDbo.class)
                .arguments("from", i -> i.observable(d, "dbo"))
                .throwWhenInvalid();
        return d;
    }

    FunctionalRole toRole() {
        return FunctionalRole.ofKind(functionalRole);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .notNull(compoundName, "compoundName").kebabFormat(compoundName, "compoundName")
                .notBlank(functionalRole, "functionalRole").maxLength(functionalRole, 48, "functionalRole");
    }
}
