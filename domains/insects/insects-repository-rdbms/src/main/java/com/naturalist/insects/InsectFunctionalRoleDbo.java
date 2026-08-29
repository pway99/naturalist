package com.naturalist.insects;

import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;
import com.naturalist.taxonomy.LinealRank;

import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Parent row of {@link InsectFunctionalRole} — a surrogate-UUID {@code Entity} keyed by
 * {@link InsectFunctionalRoleId}. The polymorphic {@link InsectRankName} it describes splits into a
 * {@code parent_rank} discriminator plus a {@code parent_name} slug (no FK — it spans five rank
 * tables), rebuilt in-module via {@link InsectRankName#of(String, LinealRank)}. The non-empty
 * {@code Set<FunctionalGuild>} becomes the {@link InsectFunctionalRoleGuildDbo} child table, loaded
 * in one batched query and passed to {@link #toEntity(Set)}.
 *
 * <p>Unlike the plant ecological role's composite unique, {@code parent_name} is a <em>single-column</em>
 * {@code UNIQUE} here (one role record per organism) — so it is declared in {@code @DboSchema.unique}
 * and the drift validator checks it.
 */
@DboSchema(table = "insect_functional_role", primaryKey = "id", unique = {"parent_name"},
           entity = InsectFunctionalRole.class)
final class InsectFunctionalRoleDbo implements Dbo {
    String id;          // the InsectFunctionalRoleId's UUID as text; mapper casts it (::uuid)
    String parentRank;  // ORDER|FAMILY|GENUS|SPECIES|SUBSPECIES — parentName.rank().name()
    String parentName;  // rank-name slug (polymorphic, no FK) — parentName.value()
    boolean beneficial;

    static InsectFunctionalRoleDbo from(InsectFunctionalRole role) {
        InsectFunctionalRoleDbo d = new InsectFunctionalRoleDbo();
        d.id = role.id().value().toString();
        d.parentRank = role.parentName().rank().name();
        d.parentName = role.parentName().value();
        d.beneficial = role.beneficial();
        Observer.forClass(InsectFunctionalRoleDbo.class)
                .arguments("from", i -> i.observable(d, "dbo"))
                .throwWhenInvalid();
        return d;
    }

    InsectFunctionalRole toEntity(Set<FunctionalGuild> guilds) {
        return new InsectFunctionalRole(
                InsectFunctionalRoleId.of(UUID.fromString(id)),
                InsectRankName.of(parentName, LinealRank.valueOf(parentRank)),
                guilds,
                beneficial);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .notBlank(id, "id")
                .notBlank(parentRank, "parentRank").maxLength(parentRank, 16, "parentRank")
                .notNull(parentName, "parentName").kebabFormat(parentName, "parentName").maxLength(parentName, 96, "parentName");
    }
}
