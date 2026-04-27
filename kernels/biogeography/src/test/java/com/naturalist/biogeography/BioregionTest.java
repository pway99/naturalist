package com.naturalist.biogeography;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.naturalist.fieldnotes.Description;
import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BioregionTest {

    private static final Observer observer = Observer.forClass(BioregionTest.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private static final List<Bioregion> ALL = List.of(
            new SacramentoValley(),
            new SouthernCascades(),
            new KlamathMountains(),
            new CoastRanges(),
            new SierraNevada(),
            new ModocPlateau()
    );

    @Test
    void everyPermitRoundTripsThroughOf() {
        for (Bioregion region : ALL) {
            assertThat(Bioregion.of(region.slug()))
                    .as("of(%s) should return same permit class", region.slug())
                    .isEqualTo(region);
        }
    }

    @Test
    void everyPermitHasNonBlankSlugAndDisplayName() {
        for (Bioregion region : ALL) {
            assertThat(region.slug()).as("slug").isNotBlank();
            assertThat(region.displayName()).as("displayName").isNotBlank();
        }
    }

    @Test
    void everyPermitDescriptionPassesInvariants() {
        var mo = observer.forMethod("everyPermitDescriptionPassesInvariants");
        for (Bioregion region : ALL) {
            Description description = region.description();

            InvariantObservation result = mo.observable(description, region.slug());

            assertThat(result.violations())
                    .as("description for %s", region.slug())
                    .isEmpty();
        }
    }

    @Test
    void unknownSlugThrows() {
        assertThatThrownBy(() -> Bioregion.of("atlantis"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("atlantis");
    }

    @Test
    void serializesToSlugString() throws Exception {
        String json = mapper.writeValueAsString(new SacramentoValley());

        assertThat(json).isEqualTo("\"sacramento-valley\"");
    }

    @Test
    void deserializesFromSlugString() throws Exception {
        Bioregion region = mapper.readValue("\"southern-cascades\"", Bioregion.class);

        assertThat(region).isEqualTo(new SouthernCascades());
    }

    @Test
    void slugsAreUniqueAcrossPermits() {
        long distinctSlugs = ALL.stream().map(Bioregion::slug).distinct().count();

        assertThat(distinctSlugs).isEqualTo(ALL.size());
    }
}
