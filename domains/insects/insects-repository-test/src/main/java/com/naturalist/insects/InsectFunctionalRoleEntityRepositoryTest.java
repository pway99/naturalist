package com.naturalist.insects;

import com.naturalist.data.EntityRepositoryTest;
import com.naturalist.data.TestEntitySource;

import java.util.List;
import java.util.Set;

/**
 * Behavioral contract for {@link InsectRepository.FunctionalRoleRepository}.
 * <p>
 * Inherits the {@link EntityRepositoryTest} cases (ADR-002). Supplies
 * {@code InsectFunctionalRole}-specific identity constants and entity construction.
 * <p>
 * The {@code newEntity} / {@code modifiedEntity} hooks attach to family-rank parents
 * that do not yet carry a role record (e.g. {@code papilionidae}, {@code chrysopidae})
 * — every species and genus in the seeded catalog already has a role record, so the
 * cross-rank pattern's family permits are the natural reservoir of "valid parent
 * without an existing role assignment."
 */
interface InsectFunctionalRoleEntityRepositoryTest
        extends EntityRepositoryTest<InsectFunctionalRoleId, InsectFunctionalRole> {

    @Override
    InsectRepository.FunctionalRoleRepository repository();

    @Override
    default TestEntitySource<InsectFunctionalRoleId, InsectFunctionalRole> source() {
        return db.getNamed(InsectFunctionalRoleTestEntitySource.class);
    }

    @Override
    default InsectFunctionalRoleId notFoundName() {
        return TestInsectsIdentifiers.InsectFunctionalRole.NotFound.name;
    }

    @Override
    default List<InsectFunctionalRoleId> knownEntityNames() {
        return List.of(
                TestInsectsIdentifiers.InsectFamily.Syrphidae.FunctionalRole.name,
                TestInsectsIdentifiers.InsectGenus.Empoasca.FunctionalRole.name,
                TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.FunctionalRole.name);
    }

    @Override
    default InsectFunctionalRole newEntity() {
        // papilionidae family record exists in the catalog (FK passes) and
        // has no role record yet (unique-on-parentName passes).
        return new InsectFunctionalRole(
                InsectFunctionalRoleId.create(),
                TestInsectsIdentifiers.InsectFamily.Papilionidae.name,
                Set.of(FunctionalGuild.POLLINATOR),
                true);
    }

    @Override
    default InsectFunctionalRole ghostEntity() {
        // FK check is skipped on the update-not-found path because
        // EntityNotFoundException fires before preSaveChecks.
        return new InsectFunctionalRole(
                InsectFunctionalRoleId.create(),
                TestInsectsIdentifiers.InsectFamily.Papilionidae.name,
                Set.of(FunctionalGuild.POLLINATOR),
                true);
    }

    @Override
    default InsectFunctionalRole modifiedEntity(InsectFunctionalRole original) {
        // chrysopidae family record exists (FK passes) and has no role record
        // (unique-on-parentName passes after the original's row is overwritten).
        return new InsectFunctionalRole(
                original.name(),
                TestInsectsIdentifiers.InsectFamily.Chrysopidae.name,
                Set.of(FunctionalGuild.PREDATOR),
                false);
    }
}
