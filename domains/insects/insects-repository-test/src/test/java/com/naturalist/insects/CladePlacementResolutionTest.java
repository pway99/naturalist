package com.naturalist.insects;

import com.naturalist.clades.CladeTraversal;
import com.naturalist.clades.Papilionidae;
import com.naturalist.data.NaturalistDatabaseExtension;
import com.naturalist.insects.lifestage.Holometabolous;
import com.naturalist.insects.lifestage.MetabolyTrait;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end resolution from loaded JSON catalog through {@code placedIn}
 * and {@link CladeTraversal} to a {@link MetabolyTrait}. This is the
 * Phase 4 validation: the Phase 1 sealed-clade vocabulary, the Phase 2
 * trait declarations on {@code Holometabola}, and the Phase 3
 * {@code placedIn} field on insect records all line up against real
 * catalog data rather than synthetic fixtures.
 */
class CladePlacementResolutionTest {

    @RegisterExtension
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    @Test
    void battusPhilenorPlacementResolvesToHolometabolyTraitViaPapilionidae() {
        InsectSpeciesTestEntitySource species =
                db.getNamed(InsectSpeciesTestEntitySource.class);

        InsectSpecies battusPhilenor = species
                .getByName(TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name)
                .orElseThrow();

        assertThat(battusPhilenor.placedIn()).isEqualTo(new Papilionidae());

        Optional<MetabolyTrait> resolved = CladeTraversal.findTrait(
                battusPhilenor.placedInOptional().orElseThrow(),
                MetabolyTrait.class,
                InsectClades::traitsFor);

        assertThat(resolved).contains(new MetabolyTrait(new Holometabolous()));
    }

    @Test
    void papilionidaeFamilyRecordCarriesItsCladePlacement() {
        InsectFamilyTestEntitySource families =
                db.getNamed(InsectFamilyTestEntitySource.class);

        InsectFamily papilionidae = families
                .getByName(TestInsectsIdentifiers.InsectFamily.Papilionidae.name)
                .orElseThrow();

        assertThat(papilionidae.placedIn()).isEqualTo(new Papilionidae());
        assertThat(papilionidae.placedInOptional()).contains(new Papilionidae());
    }
}
