package com.naturalist.insects;

import com.naturalist.data.NaturalistDatabaseExtension;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InsectQueryImplTest {
    private static final Observer observer = Observer.forClass(InsectQueryImplTest.class);

    @RegisterExtension
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    SpeciesRepositoryMock speciesRepository = new SpeciesRepositoryMock(db);
    InsectImageRepositoryMock imageRepository = new InsectImageRepositoryMock(db);
    InsectQuery.SpeciesQuery speciesQuery = new SpeciesQueryImpl(speciesRepository);
    InsectQuery.ImageQuery imageQuery = new ImageQueryImpl(imageRepository);
    InsectQuery insectQuery = new InsectQueryImpl(speciesQuery, imageQuery);

    @Test
    void getByFunctionalGuild_validaton() {
        // Act & Assert
        assertThatThrownBy(() -> insectQuery.species().getByFunctionalGuild(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("functionalGuild");
    }
        @Test
    void getByFunctionalGuild() {
        InsectEntityCollections.SpeciesCollection predatorCollection = insectQuery.species().getByFunctionalGuild(FunctionalGuild.PREDATOR);

        assertThat(observer.forMethod("getByFunctionalGuild")
                .observable(predatorCollection, "predatorColloction")
                .violationNames()).isEmpty();

        assertThat(predatorCollection.isEmpty()).isFalse();
        assertThat(predatorCollection.stream()
                .allMatch(s -> s.guilds().contains(FunctionalGuild.PREDATOR)))
                .isTrue();
    }

    @Test
    void accessors_returnNonNullDelegates() {
        assertThat(insectQuery.species()).isSameAs(speciesQuery);
        assertThat(insectQuery.images()).isSameAs(imageQuery);
        assertThat(insectQuery.insect()).isNotNull();
    }

    @Test
    void accessors_idempotent() {
        assertThat(insectQuery.species()).isSameAs(insectQuery.species());
        assertThat(insectQuery.images()).isSameAs(insectQuery.images());
        assertThat(insectQuery.insect()).isSameAs(insectQuery.insect());
    }

    @Test
    void constructor_rejectsNullSpeciesQuery() {
        assertThatThrownBy(() -> new InsectQueryImpl(null, imageQuery))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("speciesQuery");
    }

    @Test
    void constructor_rejectsNullImageQuery() {
        assertThatThrownBy(() -> new InsectQueryImpl(speciesQuery, null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("imageQuery");
    }

    @Test
    void constructor_collectsAllViolationsInSinglePass() {
        assertThatThrownBy(() -> new InsectQueryImpl(null, null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("speciesQuery", "imageQuery");
    }
}
