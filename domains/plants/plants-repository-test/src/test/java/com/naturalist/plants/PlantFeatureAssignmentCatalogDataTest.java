package com.naturalist.plants;

import com.naturalist.data.NaturalistDatabase;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Data assertions over the feature-assignment catalog, not framework assertions.
 * <p>
 * {@code featureId} carries no declarative {@link com.naturalist.data.ForeignKeyConstraint}
 * gap here — it does have one ({@link PlantFeatureAssignmentTestEntitySource}), so this test
 * is the belt to that constraint's suspenders: it names the rule and fails with a readable
 * message listing every missing id, where the constraint failure names only the first.
 * Mirrors {@code PlantGenusCatalogDataTest}.
 */
class PlantFeatureAssignmentCatalogDataTest {

    private final NaturalistDatabase db = NaturalistDatabase.create();

    private List<PlantFeatureAssignment> assignments() {
        return db.getNamed(PlantFeatureAssignmentTestEntitySource.class).entityStream().toList();
    }

    private List<PlantFeature> features() {
        return db.getNamed(PlantFeatureTestEntitySource.class).entityStream().toList();
    }

    @Test
    void everyAssignmentFeatureIdIsCatalogued() {
        Set<PlantFeatureId> catalogued = features().stream()
                .map(PlantFeature::id)
                .collect(Collectors.toSet());

        List<PlantFeatureId> referenced = assignments().stream()
                .map(PlantFeatureAssignment::featureId)
                .distinct()
                .toList();

        assertThat(catalogued)
                .as("every featureId a PlantFeatureAssignment points at must have a PlantFeature record")
                .containsAll(referenced);
    }
}
