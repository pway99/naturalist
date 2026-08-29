package com.naturalist.insects;

import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;
import com.naturalist.persistence.Fk;

import java.util.function.Consumer;

/**
 * Child row for one member of an {@link InsectFunctionalRole}'s {@code Set<FunctionalGuild>}. The
 * reference is the parent's own surrogate UUID ({@code role_id}), which is known before insert — so
 * the row is inserted directly with {@code role_id = #{roleId}::uuid}, no nested-select.
 * {@link FunctionalGuild} is an enum stored by constant name.
 */
@DboSchema(table = "insect_functional_role_guild", primaryKey = "role_id,guild",
           foreignKeys = @Fk(columns = "role_id", references = "insect_functional_role(id)"),
           entity = InsectFunctionalRole.class)
final class InsectFunctionalRoleGuildDbo implements Dbo {
    String roleId;  // the owning functional-role's own UUID as text; mapper casts it (::uuid)
    String guild;   // FunctionalGuild enum constant name

    static InsectFunctionalRoleGuildDbo from(String roleId, FunctionalGuild guild) {
        InsectFunctionalRoleGuildDbo d = new InsectFunctionalRoleGuildDbo();
        d.roleId = roleId;
        d.guild = guild.name();
        Observer.forClass(InsectFunctionalRoleGuildDbo.class)
                .arguments("from", i -> i.observable(d, "dbo"))
                .throwWhenInvalid();
        return d;
    }

    FunctionalGuild toGuild() {
        return FunctionalGuild.valueOf(guild);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .notBlank(roleId, "roleId")
                .notBlank(guild, "guild").maxLength(guild, 24, "guild");
    }
}
