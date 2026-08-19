package com.naturalist.plants;

import com.naturalist.data.EntityRepositoryTest;
import com.naturalist.data.FileName;
import com.naturalist.data.TestEntitySource;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.observation.OrganismImage;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Behavioral contract for {@link PlantRepository.ImageRepository}. Mirrors the insects
 * image contract, adapted to plant ranks: attachment is by the image's typed
 * {@link OrganismImage#parentName()}, resolved at whichever rank the identification supported.
 */
interface PlantImageRepositoryTest
        extends EntityRepositoryTest<PlantImageId, OrganismImage<PlantImageId, PlantObservationId, PlantRankName>> {

    @Override
    PlantRepository.ImageRepository repository();

    @Override
    default TestEntitySource<PlantImageId, OrganismImage<PlantImageId, PlantObservationId, PlantRankName>> source() {
        return db.getNamed(PlantImageTestEntitySource.class);
    }

    @Override
    default PlantImageId notFoundName() {
        return TestPlantsIdentifiers.Plants.NotFound.imageId;
    }

    @Override
    default List<PlantImageId> knownEntityNames() {
        return List.of(
                TestPlantsIdentifiers.Plants.CaliforniaPipevine.Images.Wide5905,
                TestPlantsIdentifiers.Plants.CaliforniaPipevine.Images.WideC072);
    }

    @Override
    default OrganismImage<PlantImageId, PlantObservationId, PlantRankName> newEntity() {
        return new OrganismImage<PlantImageId, PlantObservationId, PlantRankName>(
                PlantImageId.create(),
                TestPlantsIdentifiers.PlantGenera.Trifolium.name,
                Instant.parse("2026-07-10T09:00:00Z"),
                FileName.of("new-plant-image.jpg"),
                null);
    }

    @Override
    default OrganismImage<PlantImageId, PlantObservationId, PlantRankName> ghostEntity() {
        return new OrganismImage<PlantImageId, PlantObservationId, PlantRankName>(
                PlantImageId.create(),
                TestPlantsIdentifiers.PlantGenera.Trifolium.name,
                Instant.parse("2026-07-11T09:00:00Z"),
                FileName.of("ghost-plant-image.jpg"),
                null);
    }

    @Override
    default OrganismImage<PlantImageId, PlantObservationId, PlantRankName> modifiedEntity(
            OrganismImage<PlantImageId, PlantObservationId, PlantRankName> original) {
        return new OrganismImage<PlantImageId, PlantObservationId, PlantRankName>(
                original.id(),
                TestPlantsIdentifiers.PlantGenera.Trifolium.name,
                Instant.parse("2026-07-12T09:00:00Z"),
                FileName.of("modified-plant-image.jpg"),
                null);
    }

    @Test
    default void getByParentName_rejectsNull() {
        assertThatThrownBy(() -> repository().getByParentName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("parentName");
    }

    @Test
    default void getByParentName_returnsAllImagesForThatParent() {
        var results = repository().getByParentName(
                TestPlantsIdentifiers.Plants.CaliforniaPipevine.name);

        assertThat(results).hasSizeGreaterThanOrEqualTo(2);
        assertThat(results)
                .allMatch(image -> image.parentName()
                        .equals(TestPlantsIdentifiers.Plants.CaliforniaPipevine.name));
        assertThat(results.stream().map(OrganismImage::id))
                .contains(
                        TestPlantsIdentifiers.Plants.CaliforniaPipevine.Images.Wide5905,
                        TestPlantsIdentifiers.Plants.CaliforniaPipevine.Images.WideC072);
    }

    @Test
    default void getByParentName_parentWithNoImages_returnsEmpty() {
        // borago-officinalis is catalogued but carries no photographs.
        var results = repository().getByParentName(
                TestPlantsIdentifiers.Plants.Borage.name);

        assertThat(results).isEmpty();
    }

    @Test
    default void getByParentName_unknownParent_returnsEmpty() {
        var results = repository().getByParentName(TestPlantsIdentifiers.PlantGenera.NotFound.name);

        assertThat(results).isEmpty();
    }
}
