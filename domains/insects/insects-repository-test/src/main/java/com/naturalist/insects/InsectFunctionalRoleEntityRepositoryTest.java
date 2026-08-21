package com.naturalist.insects;

import com.naturalist.data.EntityRepositoryTest;
import com.naturalist.data.TestEntitySource;
import com.naturalist.exception.InvariantViolationException;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        return TestInsectsIdentifiers.InsectFunctionalRole.NotFound.id;
    }

    @Override
    default List<InsectFunctionalRoleId> knownEntityNames() {
        return List.of(
                TestInsectsIdentifiers.InsectFamily.Syrphidae.FunctionalRole.id,
                TestInsectsIdentifiers.InsectGenus.Empoasca.FunctionalRole.id,
                TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.FunctionalRole.id);
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
                original.id(),
                TestInsectsIdentifiers.InsectFamily.Chrysopidae.name,
                Set.of(FunctionalGuild.PREDATOR),
                false);
    }

    @Test
    default void getByGuild_rejectsNull() {
        assertThatThrownBy(() -> repository().getByGuild(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("guild");
    }

    @Test
    default void getByParentName_rejectsNull() {
        assertThatThrownBy(() -> repository().getByParentName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("parentName");
    }

    @Test
    default void getByParentNames_rejectsNull() {
        assertThatThrownBy(() -> repository().getByParentNames(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("parentNames");
    }

    @Test
    default void getByParentNames_returnsRolesAcrossTheGivenParents() {
        var results = repository().getByParentNames(Set.of(
                TestInsectsIdentifiers.InsectFamily.Syrphidae.name,
                TestInsectsIdentifiers.InsectGenus.Empoasca.name));

        assertThat(results.stream().map(InsectFunctionalRole::id))
                .contains(
                        TestInsectsIdentifiers.InsectFamily.Syrphidae.FunctionalRole.id,
                        TestInsectsIdentifiers.InsectGenus.Empoasca.FunctionalRole.id);
        assertThat(results).allSatisfy(role ->
                assertThat(role.parentName()).isIn(
                        TestInsectsIdentifiers.InsectFamily.Syrphidae.name,
                        TestInsectsIdentifiers.InsectGenus.Empoasca.name));
    }
}
