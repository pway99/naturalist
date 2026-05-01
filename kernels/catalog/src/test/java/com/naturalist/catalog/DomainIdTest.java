package com.naturalist.catalog;

import com.naturalist.catalog.DomainId.Chemistry;
import com.naturalist.catalog.DomainId.Insects;
import com.naturalist.catalog.DomainId.Plants;
import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DomainIdTest {

    private static final Observer observer = Observer.forClass(DomainIdTest.class);

    private static final List<DomainId> ALL = List.of(
            new Plants(),
            new Chemistry(),
            new Insects()
    );

    @Test
    void everySubtypePassesInvariants() {
        var mo = observer.forMethod("everySubtypePassesInvariants");
        for (DomainId domain : ALL) {
            InvariantObservation result = mo.observable(domain, domain.value());

            assertThat(result.violations())
                    .as("invariants for %s", domain.value())
                    .isEmpty();
        }
    }

    @Test
    void everySubtypeHasNonBlankSlug() {
        for (DomainId domain : ALL) {
            assertThat(domain.value())
                    .as("slug for %s", domain.getClass().getSimpleName())
                    .isNotBlank();
        }
    }

    @Test
    void slugsAreUniqueAcrossSubtypes() {
        long distinct = ALL.stream().map(DomainId::value).distinct().count();

        assertThat(distinct).isEqualTo(ALL.size());
    }

    @Test
    void everySubtypeRoundTripsThroughOf() {
        for (DomainId domain : ALL) {
            assertThat(DomainId.of(domain.value()))
                    .as("of(%s) should return same subtype", domain.value())
                    .isEqualTo(domain);
        }
    }

    @Test
    void unknownSlugThrows() {
        assertThatThrownBy(() -> DomainId.of("plnts"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("plnts");
    }

    @Test
    void emptySlugThrows() {
        assertThatThrownBy(() -> DomainId.of(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nullSlugThrows() {
        assertThatThrownBy(() -> DomainId.of(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void uppercaseSlugThrows() {
        assertThatThrownBy(() -> DomainId.of("Plants"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void freshInstancesAreEqualByValue() {
        assertThat(new Plants()).isEqualTo(new Plants());
        assertThat(new Plants()).isNotEqualTo(new Chemistry());
        assertThat(new Plants().hashCode()).isEqualTo(new Plants().hashCode());
    }

    @Test
    void toStringReturnsSlug() {
        assertThat(new Plants().toString()).isEqualTo("plants");
        assertThat(new Chemistry().toString()).isEqualTo("chemistry");
        assertThat(new Insects().toString()).isEqualTo("insects");
    }
}
