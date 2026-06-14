package com.naturalist.taxonomy;

import com.naturalist.ddd.Named;
import com.naturalist.observability.Constraints;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LinnaeanSubspeciesTest {

    @Test
    void trinomialSlugDerivesFromAllThreeEpithets() {
        LinnaeanSubspecies<TestParent> sub = subspecies("Battus", "philenor", "hirsuta");
        assertThat(sub.trinomialSlug()).isEqualTo("battus-philenor-hirsuta");
    }

    @Test
    void trinomialSlugRejectsNullSubspeciesEpithet() {
        LinnaeanSubspecies<TestParent> broken = new TestSubspecies(
                new TestParent("battus-philenor"),
                new TaxonomicGenus("Battus"),
                new TaxonomicSpecies("philenor"),
                null);
        assertThatThrownBy(broken::trinomialSlug).isInstanceOf(NullPointerException.class);
    }

    private static LinnaeanSubspecies<TestParent> subspecies(String genus, String species, String subspecies) {
        return new TestSubspecies(
                new TestParent(genus.toLowerCase() + "-" + species),
                new TaxonomicGenus(genus),
                new TaxonomicSpecies(species),
                new TaxonomicSubspecies(subspecies));
    }

    private record TestParent(String value) implements Named<String> {
        @Override
        public String key() {
            return value;
        }

        @Override
        public Consumer<? extends Constraints> invariants() {
            return i -> {
            };
        }
    }

    private record TestSubspecies(
            TestParent parentSpecies,
            TaxonomicGenus genus,
            TaxonomicSpecies species,
            TaxonomicSubspecies subspeciesEpithet
    ) implements LinnaeanSubspecies<TestParent> {
    }
}
