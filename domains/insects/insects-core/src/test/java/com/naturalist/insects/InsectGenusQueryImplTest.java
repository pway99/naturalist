package com.naturalist.insects;

import com.naturalist.data.EntityQuery;
import com.naturalist.data.EntityQueryContractTest;
import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.insects.InsectEntityCollections.GenusCollection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InsectGenusQueryImplTest
        implements EntityQueryContractTest<InsectGenusName, InsectGenus, GenusCollection> {

    @RegisterExtension
    NaturalistTestExtension db = NaturalistTestExtension.create();

    InsectGenusRepositoryMock repository = new InsectGenusRepositoryMock(db);
    InsectFamilyRepositoryMock familyRepository = new InsectFamilyRepositoryMock(db);
    InsectQuery.FamilyQuery familyQuery = new InsectFamilyQueryImpl(familyRepository);
    InsectQuery.GenusQuery query = new InsectGenusQueryImpl(repository, familyQuery);

    @Override
    public EntityQuery<InsectGenusName, InsectGenus, GenusCollection> query() {
        return query;
    }

    @Override
    public InsectGenusName notFoundName() {
        return TestInsectsIdentifiers.InsectGenus.NotFound.name;
    }

    @Override
    public List<InsectGenusName> knownEntityNames() {
        return List.of(
                TestInsectsIdentifiers.InsectGenus.Halictus.name,
                TestInsectsIdentifiers.InsectGenus.Andrena.name);
    }

    @Test
    void forFamilyName_rejectsNull() {
        assertThatThrownBy(() -> query.forFamilyName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("familyName");
    }

    @Test
    void forFamilyName_returnsGeneraInThatFamily() {
        GenusCollection collection = query.forFamilyName(InsectFamilyName.of("halictidae"));

        assertThat(collection.stream())
                .extracting(g -> g.name().value())
                .contains("halictus");
    }

    @Test
    void forOrderName_rejectsNull() {
        assertThatThrownBy(() -> query.forOrderName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("orderName");
    }

    @Test
    void forOrderName_returnsGeneraInThatOrderViaFanOut() {
        GenusCollection collection = query.forOrderName(InsectOrderName.of("hymenoptera"));

        assertThat(collection.stream())
                .extracting(g -> g.name().value())
                .contains("halictus");
    }
}
