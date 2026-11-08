package com.naturalist.plants;

import com.naturalist.catalog.EntityRef;
import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.ddd.EntityName;
import com.naturalist.insects.InsectOrderName;
import com.naturalist.test.context.Wired;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlantsEntityRefResolverTest {

    @RegisterExtension
    NaturalistTestExtension nte = NaturalistTestExtension.create();

    @Wired PlantsEntityRefResolver resolver;

    @Test
    void domainIsPlants() {
        assertThat(resolver.domain()).isEqualTo(new PlantsDomain());
        assertThat(resolver.domain().value()).isEqualTo("plants");
    }

    @Test
    void rankOfReadsTheDiscriminatorOffEachPermit() {
        assertThat(resolver.rankOf(refTo(PlantOrderName.of("lamiales")))).isEqualTo("ORDER");
        assertThat(resolver.rankOf(refTo(PlantFamilyName.of("lamiaceae")))).isEqualTo("FAMILY");
        assertThat(resolver.rankOf(refTo(PlantGenusName.of("thymus")))).isEqualTo("GENUS");
        assertThat(resolver.rankOf(refTo(PlantSpeciesName.of("thymus-vulgaris")))).isEqualTo("SPECIES");
    }

    @Test
    void resolveRebuildsTheTypedPermitForEachRank() {
        assertThat(resolver.resolve("plants", "ORDER", "lamiales").name())
                .isEqualTo(PlantOrderName.of("lamiales"));
        assertThat(resolver.resolve("plants", "GENUS", "thymus").name())
                .isEqualTo(PlantGenusName.of("thymus"));
    }

    @Test
    void resolveRoundTripsThroughTheFlatStoredForm() {
        EntityRef original = refTo(PlantSpeciesName.of("thymus-vulgaris"));

        EntityRef roundTripped = resolver.resolve(
                original.domain().value(), resolver.rankOf(original), original.name().value());

        assertThat(roundTripped).isEqualTo(original);
    }

    /**
     * The reason plants cannot drop the stored rank: {@code EntityName} equality is
     * class-qualified, so a genus and a species holding one slug are different subjects. The flat
     * form distinguishes them only by the rank column.
     */
    @Test
    void theSameSlugAtTwoRanksResolvesToTwoDifferentSubjects() {
        EntityRef asGenus = resolver.resolve("plants", "GENUS", "citrus");
        EntityRef asSpecies = resolver.resolve("plants", "SPECIES", "citrus");

        assertThat(asGenus).isNotEqualTo(asSpecies);
        assertThat(asGenus.name().value()).isEqualTo(asSpecies.name().value());
    }

    /** Plants expresses infraspecific detail as a Cultivar, so there is no subspecies rung. */
    @Test
    void resolveRejectsSubspeciesBecausePlantsHasNoSuchRank() {
        assertThatThrownBy(() -> resolver.resolve("plants", "SUBSPECIES", "anything"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported plant rank");
    }

    @Test
    void resolveRejectsAnotherDomainsSlug() {
        assertThatThrownBy(() -> resolver.resolve("insects", "ORDER", "lepidoptera"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown citation subject domain: insects");
    }

    @Test
    void rankOfRejectsANameTypeFromAnotherDomain() {
        EntityRef insectRef = new EntityRef(new PlantsDomain(), InsectOrderName.of("lepidoptera"));

        assertThatThrownBy(() -> resolver.rankOf(insectRef))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported citation subject name type");
    }

    private static EntityRef refTo(PlantRankName name) {
        return new EntityRef(new PlantsDomain(), (EntityName) name);
    }
}
