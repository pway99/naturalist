package com.naturalist.plants;

import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;
import com.naturalist.persistence.Fk;

import java.util.function.Consumer;

/**
 * Child row for one member of a {@link PlantEcologicalRole}'s {@code Set<PlantRole>}. Unlike the
 * chemistry child tables, the reference is the parent's own surrogate UUID ({@code role_id}), which
 * is known before insert — so the row is inserted directly with {@code role_id = #{roleId}::uuid},
 * no nested-select. {@link PlantRole} is an enum stored by constant name.
 */
@DboSchema(table = "plant_ecological_role_role", primaryKey = "role_id,role",
           foreignKeys = @Fk(columns = "role_id", references = "plant_ecological_role(id)"),
           entity = PlantEcologicalRole.class)
final class PlantEcologicalRoleRoleDbo implements Dbo {
    String roleId;  // the owning ecological-role's own UUID as text; mapper casts it (::uuid)
    String role;    // PlantRole enum constant name

    static PlantEcologicalRoleRoleDbo from(String roleId, PlantRole role) {
        PlantEcologicalRoleRoleDbo d = new PlantEcologicalRoleRoleDbo();
        d.roleId = roleId;
        d.role = role.name();
        Observer.forClass(PlantEcologicalRoleRoleDbo.class)
                .arguments("from", i -> i.observable(d, "dbo"))
                .throwWhenInvalid();
        return d;
    }

    PlantRole toRole() {
        return PlantRole.valueOf(role);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .notBlank(roleId, "roleId")
                .notBlank(role, "role").maxLength(role, 48, "role");
    }
}
