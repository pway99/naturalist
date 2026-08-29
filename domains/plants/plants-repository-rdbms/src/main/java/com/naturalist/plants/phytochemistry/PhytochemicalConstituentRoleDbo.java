package com.naturalist.plants.phytochemistry;

import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;
import com.naturalist.persistence.Fk;
import com.naturalist.plants.phytochemistry.role.PhytochemicalRole;

import java.util.function.Consumer;

/**
 * Child row for one member of a constituent's {@code Set<PhytochemicalRole>}. The stored
 * reference is the numeric {@code constituent_id} (FK-enforced); the DBO carries the
 * constituent's {@code name} — the mapper JOINs {@code phytochemical_constituent} to project
 * it on read and nested-selects the id from it on write, so no name is persisted here and
 * nothing can drift. {@link PhytochemicalRole} is a sealed type persisted as its
 * {@code kind()} discriminator (mirroring {@code chemistry.StructuralType}).
 */
@DboSchema(table = "phytochemical_constituent_role", primaryKey = "constituent_id,role_kind",
           foreignKeys = @Fk(columns = "constituent_id", references = "phytochemical_constituent(id)"),
           entity = PhytochemicalConstituent.class)
final class PhytochemicalConstituentRoleDbo implements Dbo {
    String constituentName;  // JOIN projection (read) / nested-select key (write); not a stored column
    String roleKind;

    static PhytochemicalConstituentRoleDbo from(PhytochemicalConstituentName constituentName, PhytochemicalRole role) {
        PhytochemicalConstituentRoleDbo d = new PhytochemicalConstituentRoleDbo();
        d.constituentName = constituentName.value();
        d.roleKind = role.kind();
        Observer.forClass(PhytochemicalConstituentRoleDbo.class)
                .arguments("from", i -> i.observable(d, "dbo"))
                .throwWhenInvalid();
        return d;
    }

    PhytochemicalRole toRole() {
        return PhytochemicalRole.ofKind(roleKind);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .notNull(constituentName, "constituentName").kebabFormat(constituentName, "constituentName")
                .notBlank(roleKind, "roleKind").maxLength(roleKind, 48, "roleKind");
    }
}
