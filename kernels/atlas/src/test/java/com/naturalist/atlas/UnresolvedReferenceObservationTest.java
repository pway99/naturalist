package com.naturalist.atlas;

import com.naturalist.atlas.DomainId.Plants;
import com.naturalist.chemistry.compound.CompoundName;
import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.Observer;
import com.naturalist.plants.PlantName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UnresolvedReferenceObservationTest {

    private static final Observer observer = Observer.forClass(UnresolvedReferenceObservationTest.class);

    private static final EntityRef SOURCE =
            new EntityRef(new Plants(), PlantName.of("california-pipevine"));

    @Test
    void wellFormedObservationPassesInvariants() {
        UnresolvedReferenceObservation event = new UnresolvedReferenceObservation(
                SOURCE,
                CompoundName.class,
                "made-up-compound",
                "slug not present in domain catalogue"
        );

        InvariantObservation result = observer.forMethod("wellFormedObservationPassesInvariants")
                .observable(event, "event");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void nullSourceViolatesInvariants() {
        UnresolvedReferenceObservation event = new UnresolvedReferenceObservation(
                null,
                CompoundName.class,
                "made-up-compound",
                "slug not present in domain catalogue"
        );

        InvariantObservation result = observer.forMethod("nullSourceViolatesInvariants")
                .observable(event, "event");

        assertThat(result.violationNames())
                .anyMatch(n -> n.contains(".source"));
    }

    @Test
    void nullTargetTypeViolatesInvariants() {
        UnresolvedReferenceObservation event = new UnresolvedReferenceObservation(
                SOURCE,
                null,
                "made-up-compound",
                "slug not present in domain catalogue"
        );

        InvariantObservation result = observer.forMethod("nullTargetTypeViolatesInvariants")
                .observable(event, "event");

        assertThat(result.violationNames())
                .anyMatch(n -> n.endsWith(".targetType"));
    }

    @Test
    void blankTargetSlugViolatesInvariants() {
        UnresolvedReferenceObservation event = new UnresolvedReferenceObservation(
                SOURCE,
                CompoundName.class,
                "   ",
                "slug not present in domain catalogue"
        );

        InvariantObservation result = observer.forMethod("blankTargetSlugViolatesInvariants")
                .observable(event, "event");

        assertThat(result.violationNames())
                .anyMatch(n -> n.endsWith(".targetSlug"));
    }

    @Test
    void blankReasonViolatesInvariants() {
        UnresolvedReferenceObservation event = new UnresolvedReferenceObservation(
                SOURCE,
                CompoundName.class,
                "made-up-compound",
                ""
        );

        InvariantObservation result = observer.forMethod("blankReasonViolatesInvariants")
                .observable(event, "event");

        assertThat(result.violationNames())
                .anyMatch(n -> n.endsWith(".reason"));
    }

    @Test
    void componentsAreReadable() {
        UnresolvedReferenceObservation event = new UnresolvedReferenceObservation(
                SOURCE,
                CompoundName.class,
                "made-up-compound",
                "slug not present in domain catalogue"
        );

        assertThat(event.source()).isEqualTo(SOURCE);
        assertThat(event.targetType()).isEqualTo(CompoundName.class);
        assertThat(event.targetSlug()).isEqualTo("made-up-compound");
        assertThat(event.reason()).isEqualTo("slug not present in domain catalogue");
    }
}
