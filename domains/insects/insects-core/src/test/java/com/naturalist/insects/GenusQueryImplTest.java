package com.naturalist.insects;

import com.naturalist.data.EntityQuery;
import com.naturalist.data.EntityQueryContractTest;
import com.naturalist.data.NaturalistDatabaseExtension;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.insects.InsectEntityCollections.GenusCollection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GenusQueryImplTest
        implements EntityQueryContractTest<InsectGenusName, InsectGenus, GenusCollection> {

    @RegisterExtension
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    GenusRepositoryMock repository = new GenusRepositoryMock(db);
    FamilyRepositoryMock familyRepository = new FamilyRepositoryMock(db);
    InsectQuery.FamilyQuery familyQuery = new FamilyQueryImpl(familyRepository);
    InsectQuery.GenusQuery query = new GenusQueryImpl(repository, familyQuery);

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
}
