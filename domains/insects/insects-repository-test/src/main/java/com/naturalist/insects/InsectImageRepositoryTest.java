package com.naturalist.insects;

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
 * Behavioral contract for {@link InsectRepository.ImageRepository}. Attachment is by
 * the image's typed {@link OrganismImage#parentName()}, resolved at whichever rank the
 * identification supported — mirrored by the plants image contract.
 */
interface InsectImageRepositoryTest
        extends EntityRepositoryTest<InsectImageId, OrganismImage<InsectImageId, InsectObservationId, InsectRankName>> {

    @Override
    InsectRepository.ImageRepository repository();

    @Override
    default TestEntitySource<InsectImageId, OrganismImage<InsectImageId, InsectObservationId, InsectRankName>> source() {
        return db.getNamed(InsectImageTestEntitySource.class);
    }

    @Override
    default InsectImageId notFoundName() {
        return TestInsectsIdentifiers.InsectSpecies.NotFound.imageId;
    }

    @Override
    default List<InsectImageId> knownEntityNames() {
        return List.of(
                TestInsectsIdentifiers.InsectGenus.Empoasca.Images.Img9047.id,
                TestInsectsIdentifiers.InsectGenus.Empoasca.Images.Img9048.id);
    }

    @Override
    default OrganismImage<InsectImageId, InsectObservationId, InsectRankName> newEntity() {
        return new OrganismImage<InsectImageId, InsectObservationId, InsectRankName>(
                InsectImageId.create(),
                TestInsectsIdentifiers.InsectGenus.Empoasca.name,
                Instant.parse("2026-07-10T09:00:00Z"),
                FileName.of("new-insect-image.jpg"),
                null);
    }

    @Override
    default OrganismImage<InsectImageId, InsectObservationId, InsectRankName> ghostEntity() {
        return new OrganismImage<InsectImageId, InsectObservationId, InsectRankName>(
                InsectImageId.create(),
                TestInsectsIdentifiers.InsectGenus.Empoasca.name,
                Instant.parse("2026-07-11T09:00:00Z"),
                FileName.of("ghost-insect-image.jpg"),
                null);
    }

    @Override
    default OrganismImage<InsectImageId, InsectObservationId, InsectRankName> modifiedEntity(
            OrganismImage<InsectImageId, InsectObservationId, InsectRankName> original) {
        return new OrganismImage<InsectImageId, InsectObservationId, InsectRankName>(
                original.id(),
                TestInsectsIdentifiers.InsectGenus.Empoasca.name,
                Instant.parse("2026-07-12T09:00:00Z"),
                FileName.of("modified-insect-image.jpg"),
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
                TestInsectsIdentifiers.InsectGenus.Empoasca.name);

        assertThat(results).hasSizeGreaterThanOrEqualTo(2);
        assertThat(results)
                .allMatch(image -> image.parentName()
                        .equals(TestInsectsIdentifiers.InsectGenus.Empoasca.name));
        assertThat(results.stream().map(OrganismImage::id))
                .contains(
                        TestInsectsIdentifiers.InsectGenus.Empoasca.Images.Img9047.id,
                        TestInsectsIdentifiers.InsectGenus.Empoasca.Images.Img9048.id);
    }

    @Test
    default void getByParentName_parentWithNoImages_returnsEmpty() {
        // apis-mellifera is catalogued but carries no photographs.
        var results = repository().getByParentName(
                TestInsectsIdentifiers.InsectSpecies.ApisMellifera.name);

        assertThat(results).isEmpty();
    }

    @Test
    default void getByParentName_unknownParent_returnsEmpty() {
        var results = repository().getByParentName(TestInsectsIdentifiers.InsectSpecies.NotFound.name);

        assertThat(results).isEmpty();
    }
}
