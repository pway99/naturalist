package com.naturalist.insects;

import com.naturalist.data.NaturalistTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link InsectFeature#value} is uniquely constrained (see
 * {@link InsectFeatureTestEntitySource#uniqueConstraints()}), so an exact
 * repeated feature string cannot be {@code insert()}-ed twice. This is the
 * repository-level coverage of {@code save()}'s resolution for that case --
 * the caller-supplied id (freshly minted per identification, in the real
 * caller) is discarded and the existing row's id is reused. See
 * {@code InsectIdentificationCommandTest#identify_reIdentifyingSameRankWithSameFeatures_doesNotGrowCatalogOrDangleFeatureIds}
 * in {@code insects-core} for the end-to-end path this repository behavior
 * unblocks.
 */
class InsectFeatureSaveTest {

    @RegisterExtension
    NaturalistTestExtension db = NaturalistTestExtension.create();

    InsectFeatureRepositoryMock repository = new InsectFeatureRepositoryMock(db);

    @Test
    void save_valueAlreadyExists_reusesExistingRowIdAndCreatesNoSecondRow() {
        var value = "unobtainium carapace texture";

        var first = repository.save(InsectFeature.of(InsectFeatureId.create(), value));
        var second = repository.save(InsectFeature.of(InsectFeatureId.create(), value));

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(repository.getByName(first.id())).isPresent();

        long matchingRows = db.getNamed(InsectFeatureTestEntitySource.class)
                .entityStream()
                .filter(f -> f.value().equals(value.toLowerCase()))
                .count();
        assertThat(matchingRows).isEqualTo(1);
    }

    @Test
    void save_newValue_insertsAsNew() {
        var feature = InsectFeature.of(InsectFeatureId.create(), "unobtainium sternite pattern");

        var persisted = repository.save(feature);

        assertThat(persisted).isEqualTo(feature);
        assertThat(repository.getByName(feature.id())).contains(feature);
    }
}
