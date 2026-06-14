package com.naturalist.insects;

import com.naturalist.data.EntityQuery;
import com.naturalist.data.EntityQueryContractTest;
import com.naturalist.data.NaturalistDatabaseExtension;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.insects.InsectEntityCollections.FunctionalRoleCollection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FunctionalRoleQueryImplTest
        implements EntityQueryContractTest<InsectFunctionalRoleId, InsectFunctionalRole, FunctionalRoleCollection> {

    @RegisterExtension
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    InsectFunctionalRoleRepositoryMock repository = new InsectFunctionalRoleRepositoryMock(db);
    InsectQuery.FunctionalRoleQuery query = new FunctionalRoleQueryImpl(repository);

    @Override
    public EntityQuery<InsectFunctionalRoleId, InsectFunctionalRole, FunctionalRoleCollection> query() {
        return query;
    }

    @Override
    public InsectFunctionalRoleId notFoundName() {
        return TestInsectsIdentifiers.InsectFunctionalRole.NotFound.id;
    }

    @Override
    public List<InsectFunctionalRoleId> knownEntityNames() {
        return List.of(
                TestInsectsIdentifiers.InsectFamily.Syrphidae.FunctionalRole.id,
                TestInsectsIdentifiers.InsectGenus.Empoasca.FunctionalRole.id,
                TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.FunctionalRole.id);
    }

    @Test
    void getByGuild_rejectsNull() {
        assertThatThrownBy(() -> query.getByGuild(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("guild");
    }

    @Test
    void getByGuild_singleMatch_returnsThatRecord() {
        // KEYSTONE is the sole guild held by exactly one seed record
        // (battus-philenor). Confirms the single-result path.
        FunctionalRoleCollection collection = query.getByGuild(FunctionalGuild.KEYSTONE);

        assertThat(collection.size()).isEqualTo(1);
        assertThat(collection.stream().findFirst().orElseThrow().parentName())
                .isEqualTo(TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name);
    }

    @Test
    void getByGuild_foodWebMatch_returnsTheCrossRankGenusRecord() {
        // Empoasca (genus) carries FOOD_WEB — the headline cross-rank case
        // surfaced by PL-11. Confirms a genus-rank parent is returned by the
        // guild query exactly the same way a species-rank parent would be.
        FunctionalRoleCollection collection = query.getByGuild(FunctionalGuild.FOOD_WEB);

        assertThat(collection.stream())
                .allMatch(role -> role.guilds().contains(FunctionalGuild.FOOD_WEB));
        assertThat(collection.stream().map(InsectFunctionalRole::parentName))
                .contains(TestInsectsIdentifiers.InsectGenus.Empoasca.name);
    }

    @Test
    void getByParentName_rejectsNull() {
        assertThatThrownBy(() -> query.getByParentName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("parentName");
    }

    @Test
    void getByParentName_unknownParent_returnsEmpty() {
        Optional<InsectFunctionalRole> result =
                query.getByParentName(TestInsectsIdentifiers.InsectFamily.NotFound.name);

        assertThat(result).isEmpty();
    }

    @Test
    void getByParentName_knownGenusParent_returnsThatRole() {
        // empoasca is the canonical PL-11 cross-rank case — a genus carrying
        // FOOD_WEB. getByParentName at genus rank returns the same record that
        // getByGuild(FOOD_WEB) would.
        Optional<InsectFunctionalRole> result =
                query.getByParentName(TestInsectsIdentifiers.InsectGenus.Empoasca.name);

        assertThat(result).isPresent();
        assertThat(result.get().guilds()).contains(FunctionalGuild.FOOD_WEB);
        assertThat(result.get().parentName())
                .isEqualTo(TestInsectsIdentifiers.InsectGenus.Empoasca.name);
    }

    @Test
    void getByGuild_multipleMatchesAcrossRanks_returnsAllOfThem() {
        // POLLINATOR spans all three ranks in the seed — families syrphidae +
        // hesperiidae, genera halictus + andrena + chrysoperla, species
        // xylocopa-varipuncta + vanessa-cardui + colias-eurytheme. Returning
        // all of them in one call is the point of the cross-rank query.
        FunctionalRoleCollection collection = query.getByGuild(FunctionalGuild.POLLINATOR);

        assertThat(collection.stream())
                .allMatch(role -> role.guilds().contains(FunctionalGuild.POLLINATOR));

        Set<Class<?>> parentRankClasses = collection.stream()
                .map(role -> role.parentName().getClass())
                .collect(Collectors.toUnmodifiableSet());
        assertThat(parentRankClasses).containsExactlyInAnyOrder(
                InsectFamilyName.class, InsectGenusName.class, InsectSpeciesName.class);
    }
}
