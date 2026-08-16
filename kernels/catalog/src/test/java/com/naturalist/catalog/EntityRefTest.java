package com.naturalist.catalog;

import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.Observer;
import com.naturalist.plants.PlantSpeciesName;
import com.naturalist.plants.TestPlantsIdentifiers;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EntityRefTest {

    private record Plants() implements DomainId {
        @Override
        public String value() {
            return "plants";
        }
    }

    private static final Observer observer = Observer.forClass(EntityRefTest.class);

    @Test
    void wellFormedRefPassesInvariants() {
        EntityRef ref = new EntityRef(new Plants(),
                TestPlantsIdentifiers.Plants.CaliforniaPipevine.name);

        InvariantObservation result = observer.forMethod("wellFormedRefPassesInvariants")
                .observable(ref, "ref");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void nullDomainViolatesInvariants() {
        EntityRef ref = new EntityRef(null,
                TestPlantsIdentifiers.Plants.CaliforniaPipevine.name);

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
        EntityRef ref = new EntityRef(new Plants(), PlantSpeciesName.of("Not Kebab Case"));

        InvariantObservation result = observer.forMethod("invalidNameSlugViolatesInvariants")
                .observable(ref, "ref");

        assertThat(result.violationNames())
                .anyMatch(n -> n.contains(".name"));
    }

    @Test
    void displayLabelReturnsTheSlug() {
        EntityRef ref = new EntityRef(new Plants(),
                TestPlantsIdentifiers.Plants.Borage.name);

        assertThat(ref.displayLabel())
                .isEqualTo(TestPlantsIdentifiers.Plants.Borage.name.value());
    }

    @Test
    void displayLabelOnNullNameReturnsEmpty() {
        EntityRef ref = new EntityRef(new Plants(), null);

        assertThat(ref.displayLabel()).isEmpty();
    }

    @Test
    void valueEqualityIsByContent() {
        EntityRef a = new EntityRef(new Plants(),
                TestPlantsIdentifiers.Plants.CaliforniaPipevine.name);
        EntityRef b = new EntityRef(new Plants(),
                TestPlantsIdentifiers.Plants.CaliforniaPipevine.name);
        EntityRef c = new EntityRef(new Plants(),
                TestPlantsIdentifiers.Plants.Borage.name);

        assertThat(a).isEqualTo(b);
        assertThat(a).isNotEqualTo(c);
    }
}
