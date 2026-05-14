package com.naturalist.insects;

import com.naturalist.clades.CladeTraversal;
import com.naturalist.clades.Holometabola;
import com.naturalist.clades.Papilionidae;
import com.naturalist.data.NaturalistDatabaseExtension;
import com.naturalist.insects.lifestage.Holometabolous;
import com.naturalist.insects.lifestage.InsectLifeStages;
import com.naturalist.insects.lifestage.MetabolyTrait;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end resolution from loaded JSON catalog through {@code placedIn}
 * and {@link CladeTraversal} to a {@link MetabolyTrait}, and through the
 * Phase 5 {@link InsectLifeStages} resolver to the stage-kind list. The
 * Phase 1 sealed-clade vocabulary, the Phase 2 trait declarations on
 * {@code Holometabola}, the Phase 3 {@code placedIn} field on insect
 * records, and the Phase 5 organism-level resolver all line up against
 * real catalog data rather than synthetic fixtures.
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
    void battusPhilenorStagesResolveToHolometabolousSequenceViaResolver() {
        InsectSpeciesTestEntitySource species =
                db.getNamed(InsectSpeciesTestEntitySource.class);

        InsectSpecies battusPhilenor = species
                .getByName(TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name)
                .orElseThrow();

        assertThat(InsectLifeStages.stagesOf(battusPhilenor))
                .containsExactly(
                        LifeStageKind.EGG,
                        LifeStageKind.LARVA,
                        LifeStageKind.PUPA,
                        LifeStageKind.ADULT);
    }

    @Test
    void papilionidaeFamilyStagesResolveToHolometabolousSequenceViaResolver() {
        InsectFamilyTestEntitySource families =
                db.getNamed(InsectFamilyTestEntitySource.class);

        InsectFamily papilionidae = families
                .getByName(TestInsectsIdentifiers.InsectFamily.Papilionidae.name)
                .orElseThrow();

        assertThat(InsectLifeStages.stagesOf(papilionidae))
                .containsExactly(
                        LifeStageKind.EGG,
                        LifeStageKind.LARVA,
                        LifeStageKind.PUPA,
                        LifeStageKind.ADULT);
    }

    @Test
    void andrenaGenusStagesResolveToHolometabolousSequenceViaResolver() {
        InsectGenusTestEntitySource genera =
                db.getNamed(InsectGenusTestEntitySource.class);

        InsectGenus andrena = genera
                .getByName(TestInsectsIdentifiers.InsectGenus.Andrena.name)
                .orElseThrow();

        assertThat(andrena.placedIn()).isEqualTo(new Holometabola());
        assertThat(InsectLifeStages.stagesOf(andrena))
                .containsExactly(
                        LifeStageKind.EGG,
                        LifeStageKind.LARVA,
                        LifeStageKind.PUPA,
                        LifeStageKind.ADULT);
    }

    @Test
    void halictusGenusStagesResolveToHolometabolousSequenceViaResolver() {
        InsectGenusTestEntitySource genera =
                db.getNamed(InsectGenusTestEntitySource.class);

        InsectGenus halictus = genera
                .getByName(TestInsectsIdentifiers.InsectGenus.Halictus.name)
                .orElseThrow();

        assertThat(halictus.placedIn()).isEqualTo(new Holometabola());
        assertThat(InsectLifeStages.stagesOf(halictus))
                .containsExactly(
                        LifeStageKind.EGG,
                        LifeStageKind.LARVA,
                        LifeStageKind.PUPA,
                        LifeStageKind.ADULT);
    }

    @Test
    void chrysoperlaGenusStagesResolveToHolometabolousSequenceViaResolver() {
        InsectGenusTestEntitySource genera =
                db.getNamed(InsectGenusTestEntitySource.class);

        InsectGenus chrysoperla = genera
                .getByName(TestInsectsIdentifiers.InsectGenus.Chrysoperla.name)
                .orElseThrow();

        assertThat(chrysoperla.placedIn()).isEqualTo(new Holometabola());
        assertThat(InsectLifeStages.stagesOf(chrysoperla))
                .containsExactly(
                        LifeStageKind.EGG,
                        LifeStageKind.LARVA,
                        LifeStageKind.PUPA,
                        LifeStageKind.ADULT);
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
