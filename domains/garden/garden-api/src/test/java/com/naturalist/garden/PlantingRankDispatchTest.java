package com.naturalist.garden;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.naturalist.plants.PlantRankName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link Planting} dispatches on {@link PlantRankName}, so it must name every permit.
 * <p>
 * Adding a permit to the sealed interface breaks nothing at compile time — the
 * {@code @JsonSubTypes} list is an annotation, not an exhaustive switch — so a new rank
 * silently fails to deserialize here. When plants added {@code PlantOrderName} in M2g,
 * this consumer sat one domain away and was easy to miss; this test makes the miss loud.
 * The plants-side consumers carry their own copy — garden-api and plants-api cannot see
 * each other's tests.
 */
class PlantingRankDispatchTest {

    @Test
    void plantingDispatchesOverEveryPermit() {
        Set<Class<?>> permits = Set.of(PlantRankName.class.getPermittedSubclasses());

        RecordComponent component = Arrays.stream(Planting.class.getRecordComponents())
                .filter(rc -> rc.getType().equals(PlantRankName.class))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Planting has no PlantRankName component"));

        JsonSubTypes annotation;
        try {
            annotation = Planting.class.getDeclaredField(component.getName())
                    .getAnnotation(JsonSubTypes.class);
        } catch (NoSuchFieldException e) {
            throw new AssertionError(e);
        }
        assertThat(annotation)
                .as("Planting.%s must declare @JsonSubTypes", component.getName())
                .isNotNull();

        Set<Class<?>> dispatched = Arrays.stream(annotation.value())
                .map(JsonSubTypes.Type::value)
                .collect(Collectors.toSet());

        assertThat(dispatched)
                .as("Planting must name every PlantRankName permit")
                .containsExactlyInAnyOrderElementsOf(permits);
    }
}
