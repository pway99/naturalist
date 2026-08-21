package com.naturalist.plants;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.naturalist.plants.management.PlantProgram;
import com.naturalist.plants.phytochemistry.PhytochemicalConstituent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every record that dispatches on {@link PlantRankName} must name all four permits.
 * <p>
 * Adding a permit to the sealed interface breaks nothing at compile time — the
 * {@code @JsonSubTypes} lists are annotations, not exhaustive switches — so a new rank
 * silently fails to deserialize at whichever consumers were not updated. M2g added
 * {@code PlantOrderName} and left all three of these behind; this test turns that class
 * of miss into a build failure.
 * <p>
 * {@code OrganismImage} no longer appears here: its {@code parentName} moved off
 * {@code @JsonSubTypes} onto the shared {@code RankName} {@code {"rank","value"}} codec
 * (registered {@code RankNameReconstructor}), so it is not a {@code @JsonSubTypes}
 * dispatch consumer. The three records below still are.
 * <p>
 * {@code Planting} in the garden domain declares the same dispatch and is covered by its
 * own copy of this test — plants-api cannot see garden-api.
 */
class PlantRankNameDispatchTest {

    private static final List<Class<?>> CONSUMERS = List.of(
            PlantEcologicalRole.class,
            PlantProgram.class,
            PhytochemicalConstituent.class,
            PlantFeatureAssignment.class);

    @Test
    void everyConsumerDispatchesOverEveryPermit() {
        Set<Class<?>> permits = Set.of(PlantRankName.class.getPermittedSubclasses());

        assertThat(CONSUMERS).allSatisfy(consumer ->
                assertThat(dispatchedTypes(consumer))
                        .as("%s dispatches on PlantRankName and must name every permit",
                                consumer.getSimpleName())
                        .containsExactlyInAnyOrderElementsOf(permits));
    }

    private static Set<Class<?>> dispatchedTypes(Class<?> consumer) {
        RecordComponent component = Arrays.stream(consumer.getRecordComponents())
                .filter(rc -> rc.getType().equals(PlantRankName.class))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        consumer.getSimpleName() + " has no PlantRankName component"));

        // The annotation is distributed to the backing field because @JsonSubTypes
        // targets FIELD; reading it there is what makes the lookup reliable.
        JsonSubTypes annotation;
        try {
            annotation = consumer.getDeclaredField(component.getName())
                    .getAnnotation(JsonSubTypes.class);
        } catch (NoSuchFieldException e) {
            throw new AssertionError(e);
        }
        assertThat(annotation)
                .as("%s.%s must declare @JsonSubTypes",
                        consumer.getSimpleName(), component.getName())
                .isNotNull();

        return Arrays.stream(annotation.value())
                .map(JsonSubTypes.Type::value)
                .collect(Collectors.toSet());
    }
}
