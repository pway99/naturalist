package com.naturalist.atlas;

import com.naturalist.atlas.DomainId.Plants;
import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.Observer;
import com.naturalist.plants.PlantName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EntityRefTest {

    private static final Observer observer = Observer.forClass(EntityRefTest.class);

    @Test
    void wellFormedRefPassesInvariants() {
        EntityRef ref = new EntityRef(new Plants(), PlantName.of("california-pipevine"));

        InvariantObservation result = observer.forMethod("wellFormedRefPassesInvariants")
                .observable(ref, "ref");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void nullDomainViolatesInvariants() {
        EntityRef ref = new EntityRef(null, PlantName.of("california-pipevine"));

        InvariantObservation result = observer.forMethod("nullDomainViolatesInvariants")
                .observable(ref, "ref");

        assertThat(result.violationNames())
                .anyMatch(n -> n.contains(".domain"));
    }

    @Test
    void nullNameViolatesInvariants() {
        EntityRef ref = new EntityRef(new Plants(), null);

        InvariantObservation result = observer.forMethod("nullNameViolatesInvariants")
                .observable(ref, "ref");

        assertThat(result.violationNames())
                .anyMatch(n -> n.endsWith(".name"));
    }

    @Test
    void invalidNameSlugViolatesInvariants() {
        EntityRef ref = new EntityRef(new Plants(), PlantName.of("Not Kebab Case"));

        InvariantObservation result = observer.forMethod("invalidNameSlugViolatesInvariants")
                .observable(ref, "ref");

        assertThat(result.violationNames())
                .anyMatch(n -> n.contains(".name"));
    }

    @Test
    void displayLabelReturnsTheSlug() {
        EntityRef ref = new EntityRef(new Plants(), PlantName.of("crimson-clover"));

        assertThat(ref.displayLabel()).isEqualTo("crimson-clover");
    }

    @Test
    void displayLabelOnNullNameReturnsEmpty() {
        EntityRef ref = new EntityRef(new Plants(), null);

        assertThat(ref.displayLabel()).isEmpty();
    }

    @Test
    void valueEqualityIsByContent() {
        EntityRef a = new EntityRef(new Plants(), PlantName.of("california-pipevine"));
        EntityRef b = new EntityRef(new Plants(), PlantName.of("california-pipevine"));
        EntityRef c = new EntityRef(new Plants(), PlantName.of("crimson-clover"));

        assertThat(a).isEqualTo(b);
        assertThat(a).isNotEqualTo(c);
    }
}
