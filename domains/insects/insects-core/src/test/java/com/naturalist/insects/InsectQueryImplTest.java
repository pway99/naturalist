package com.naturalist.insects;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class InsectQueryImplTest {
    static final Observer observer = Observer.forClass(InsectQueryImplTest.class);

    @RegisterExtension
    NaturalistDatabase db = NaturalistDatabase.create();

    SpeciesRepositoryMock speciesRepository = new SpeciesRepositoryMock(db);
    InsectImageRepositoryMock imageRepository = new InsectImageRepositoryMock(db);
    InsectQuery.SpeciesQuery speciesQuery = new SpeciesQueryImpl(speciesRepository);
    InsectQuery.ImageQuery imageQuery = new ImageQueryImpl(imageRepository);
    InsectQuery insectQuery = new InsectQueryImpl(speciesQuery, imageQuery);

    @Test
    void getInsectAggregate() {
        Optional<InsectAggregate> insectAggregate =
                insectQuery.insect().getByName(TestInsectsIdentifiers.InsectSpecies.PotatoLeafhopper.name);

        assertThat(insectAggregate).isPresent();
        assertThat(observer.observable(insectAggregate.get(), "insectAggregate").violations()).isEmpty();
    }
}
