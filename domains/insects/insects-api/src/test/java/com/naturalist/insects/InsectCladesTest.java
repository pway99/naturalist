package com.naturalist.insects;

import com.naturalist.clades.CladeTraversal;
import com.naturalist.clades.Eukaryota;
import com.naturalist.clades.Hemiptera;
import com.naturalist.clades.Holometabola;
import com.naturalist.clades.Insecta;
import com.naturalist.clades.Lepidoptera;
import com.naturalist.clades.Papilionidae;
import com.naturalist.insects.lifestage.Hemimetabolous;
import com.naturalist.insects.lifestage.Holometabolous;
import com.naturalist.insects.lifestage.MetabolyTrait;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class InsectCladesTest {

    @Test
    void metabolyResolvesFromPapilionidaeViaTraversalToHolometabola() {
        Optional<MetabolyTrait> result = CladeTraversal.findTrait(
                new Papilionidae(),
                MetabolyTrait.class,
                InsectClades::traitsFor);

        assertThat(result).isPresent();
        assertThat(result.get().metaboly()).isEqualTo(new Holometabolous());
    }

    @Test
    void metabolyResolvesFromLepidopteraViaTraversal() {
        Optional<MetabolyTrait> result = CladeTraversal.findTrait(
                new Lepidoptera(),
                MetabolyTrait.class,
                InsectClades::traitsFor);

        assertThat(result).isPresent();
        assertThat(result.get().metaboly()).isEqualTo(new Holometabolous());
    }

    @Test
    void metabolyResolvesAtTheDeclaringNode() {
        Optional<MetabolyTrait> result = CladeTraversal.findTrait(
                new Holometabola(),
                MetabolyTrait.class,
                InsectClades::traitsFor);

        assertThat(result).isPresent();
        assertThat(result.get().metaboly()).isEqualTo(new Holometabolous());
    }

    @Test
    void metabolyIsAbsentAboveHolometabola() {
        // Insecta is one node above the declaration on Holometabola —
        // traversal walks upward, not downward, so nothing is found.
        Optional<MetabolyTrait> result = CladeTraversal.findTrait(
                new Insecta(),
                MetabolyTrait.class,
                InsectClades::traitsFor);

        assertThat(result).isEmpty();
    }

    @Test
    void metabolyIsAbsentAtTheRoot() {
        Optional<MetabolyTrait> result = CladeTraversal.findTrait(
                new Eukaryota(),
                MetabolyTrait.class,
                InsectClades::traitsFor);

        assertThat(result).isEmpty();
    }

    @Test
    void metabolyResolvesAtHemipteraToHemimetabolous() {
        Optional<MetabolyTrait> result = CladeTraversal.findTrait(
                new Hemiptera(),
                MetabolyTrait.class,
                InsectClades::traitsFor);

        assertThat(result).isPresent();
        assertThat(result.get().metaboly()).isEqualTo(new Hemimetabolous());
    }
}
