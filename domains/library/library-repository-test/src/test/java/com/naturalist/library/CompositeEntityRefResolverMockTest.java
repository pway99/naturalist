package com.naturalist.library;

import com.naturalist.catalog.EntityRef;
import com.naturalist.ddd.EntityName;
import com.naturalist.insects.InsectOrderName;
import com.naturalist.insects.InsectsDomain;
import com.naturalist.plants.PlantGenusName;
import com.naturalist.plants.PlantsDomain;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompositeEntityRefResolverMockTest {

    private final CompositeEntityRefResolverMock composite = new CompositeEntityRefResolverMock(
            List.of(new InsectsEntityRefResolverMock(), new PlantsEntityRefResolverMock()));

    @Test
    void resolveDispatchesOnTheDomainSlug() {
        EntityRef insect = composite.resolve("insects", "ORDER", "lepidoptera");
        EntityRef plant = composite.resolve("plants", "GENUS", "thymus");

        assertThat(insect.domain()).isEqualTo(new InsectsDomain());
        assertThat(insect.name()).isEqualTo(InsectOrderName.of("lepidoptera"));
        assertThat(plant.domain()).isEqualTo(new PlantsDomain());
        assertThat(plant.name()).isEqualTo(PlantGenusName.of("thymus"));
    }

    @Test
    void rankOfDispatchesOnTheSubjectsOwnDomain() {
        EntityRef insect = new EntityRef(new InsectsDomain(), InsectOrderName.of("lepidoptera"));
        EntityRef plant = new EntityRef(new PlantsDomain(), (EntityName) PlantGenusName.of("thymus"));

        assertThat(composite.rankOf(insect)).isEqualTo("ORDER");
        assertThat(composite.rankOf(plant)).isEqualTo("GENUS");
    }

    @Test
    void bothDirectionsRoundTripForEitherDomain() {
        for (EntityRef original : List.of(
                new EntityRef(new InsectsDomain(), InsectOrderName.of("lepidoptera")),
                new EntityRef(new PlantsDomain(), (EntityName) PlantGenusName.of("thymus")))) {

            EntityRef roundTripped = composite.resolve(
                    original.domain().value(),
                    composite.rankOf(original),
                    original.name().value());

            assertThat(roundTripped).isEqualTo(original);
        }
    }

    @Test
    void resolveRejectsADomainNoDelegateClaims() {
        assertThatThrownBy(() -> composite.resolve("chemistry", "GENUS", "whatever"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown citation subject domain: chemistry")
                .hasMessageContaining("insects");
    }

    /**
     * The collision rule {@code CatalogAssembly} applies to {@code DomainId}s, applied to
     * resolvers: two delegates claiming one slug is a wiring error, caught at construction rather
     * than silently settled by list order.
     */
    @Test
    void twoDelegatesClaimingOneSlugIsRejectedAtConstruction() {
        assertThatThrownBy(() -> new CompositeEntityRefResolverMock(List.of(
                new InsectsEntityRefResolverMock(), new InsectsEntityRefResolverMock())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Two resolvers claim the citation subject domain 'insects'");
    }

    @Test
    void anEmptyDelegateListIsRejected() {
        assertThatThrownBy(() -> new CompositeEntityRefResolverMock(List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one delegate");
    }

    @Test
    void theCompositeCannotNameASingleDomain() {
        assertThatThrownBy(composite::domain)
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("several domains");
    }
}
