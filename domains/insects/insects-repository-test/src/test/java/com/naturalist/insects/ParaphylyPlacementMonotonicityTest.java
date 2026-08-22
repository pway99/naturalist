package com.naturalist.insects;

import com.naturalist.clades.Anthophila;
import com.naturalist.clades.Apoidea;
import com.naturalist.clades.Blattodea;
import com.naturalist.clades.Clade;
import com.naturalist.clades.CladeTraversal;
import com.naturalist.clades.DrosophilaSensuStricto;
import com.naturalist.clades.Drosophilinae;
import com.naturalist.clades.Holometabola;
import com.naturalist.clades.Sophophora;
import com.naturalist.clades.Termitoidae;
import com.naturalist.clades.Troidini;
import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.insects.lifestage.Hemimetabolous;
import com.naturalist.insects.lifestage.Holometabolous;
import com.naturalist.insects.lifestage.MetabolyTrait;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Acceptance suite for placement-chain monotonicity across the paraphyly
 * fixtures. Asserts that {@code child.placedIn} is a descendant-or-equal
 * of {@code parent.placedIn} in the clade DAG whenever both are non-null.
 * <p>
 * Three fixture lineages exercise non-trivial paths:
 * <ul>
 *   <li>2.1 Drosophila — genus-level paraphyly (decisive case)</li>
 *   <li>2.2 Blattodea/termites — order/family absorption</li>
 *   <li>2.3 Bees within apoid wasps — rank-less clade</li>
 * </ul>
 * Plus the Battus philenor control (clean monophyletic nesting).
 *
 * @see <a href="docs/notes/clade-assignment-investigation/paraphyly-fixtures-spec.md">Spec §6</a>
 */
class ParaphylyPlacementMonotonicityTest {

    @RegisterExtension
    NaturalistTestExtension db = NaturalistTestExtension.create();

    // -- Utility: DAG-descendant check --

    /**
     * Returns true if {@code candidate} is a descendant-or-equal of
     * {@code ancestor} in the clade parent-chain DAG.
     */
    private static boolean isDescendantOrEqual(Clade candidate, Clade ancestor) {
        return CladeTraversal.ancestry(candidate).contains(ancestor);
    }

    // ===== 2.0 Control — Battus philenor (monophyletic) =====

    @Test
    void control_battusPhilenorPlacedAtTroidini() {
        InsectSpecies species = loadSpecies(
                TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name);

        assertThat(species.placedIn()).isEqualTo(new Troidini());
    }

    @Test
    void control_troidiniIsDescendantOfHolometabola() {
        assertThat(isDescendantOrEqual(new Troidini(), new Holometabola())).isTrue();
    }

    @Test
    void control_battusPhilenorResolvesHolometabolous() {
        InsectSpecies species = loadSpecies(
                TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name);

        Optional<MetabolyTrait> trait = CladeTraversal.findTrait(
                species.placedIn(), MetabolyTrait.class, InsectClades::traitsFor);

        assertThat(trait).contains(new MetabolyTrait(new Holometabolous()));
    }

    // ===== 2.1 Drosophila — genus paraphyly (decisive case) =====

    @Test
    void drosophila_genusPlacedAtDrosophilinae() {
        InsectGenus genus = loadGenus(
                TestInsectsIdentifiers.InsectGenus.Drosophila.name);

        assertThat(genus.placedIn()).isEqualTo(new Drosophilinae());
    }

    @Test
    void drosophila_melanogasterPlacedAtSophophora() {
        InsectSpecies species = loadSpecies(
                TestInsectsIdentifiers.InsectSpecies.DrosophilaMelanogaster.name);

        assertThat(species.placedIn()).isEqualTo(new Sophophora());
    }

    @Test
    void drosophila_funebrisPlacedAtDrosophilaSensuStricto() {
        InsectSpecies species = loadSpecies(
                TestInsectsIdentifiers.InsectSpecies.DrosophilaFunebris.name);

        assertThat(species.placedIn()).isEqualTo(new DrosophilaSensuStricto());
    }

    @Test
    void drosophila_monotonicity_sophohoraDescendantOfDrosophilinae() {
        assertThat(isDescendantOrEqual(new Sophophora(), new Drosophilinae())).isTrue();
    }

    @Test
    void drosophila_monotonicity_drosophilaSensuStrictoDescendantOfDrosophilinae() {
        assertThat(isDescendantOrEqual(new DrosophilaSensuStricto(), new Drosophilinae()))
                .isTrue();
    }

    @Test
    void drosophila_melanogasterResolvesHolometabolous() {
        InsectSpecies species = loadSpecies(
                TestInsectsIdentifiers.InsectSpecies.DrosophilaMelanogaster.name);

        Optional<MetabolyTrait> trait = CladeTraversal.findTrait(
                species.placedIn(), MetabolyTrait.class, InsectClades::traitsFor);

        assertThat(trait).contains(new MetabolyTrait(new Holometabolous()));
    }

    @Test
    void drosophila_funebrisResolvesHolometabolous() {
        InsectSpecies species = loadSpecies(
                TestInsectsIdentifiers.InsectSpecies.DrosophilaFunebris.name);

        Optional<MetabolyTrait> trait = CladeTraversal.findTrait(
                species.placedIn(), MetabolyTrait.class, InsectClades::traitsFor);

        assertThat(trait).contains(new MetabolyTrait(new Holometabolous()));
    }

    // ===== 2.2 Blattodea/termites — order absorption =====

    @Test
    void blattodea_orderPlacedAtBlattodea() {
        InsectOrder order = loadOrder(
                TestInsectsIdentifiers.InsectOrder.Blattodea.name);

        assertThat(order.placedIn()).isEqualTo(new Blattodea());
    }

    @Test
    void blattodea_periplanetaAmericanaPlacedAtBlattodea() {
        InsectSpecies species = loadSpecies(
                TestInsectsIdentifiers.InsectSpecies.PeriplanetaAmericana.name);

        assertThat(species.placedIn()).isEqualTo(new Blattodea());
    }

    @Test
    void blattodea_reticulitermesHesperusPlacedAtTermitoidae() {
        InsectSpecies species = loadSpecies(
                TestInsectsIdentifiers.InsectSpecies.ReticulitermesHesperus.name);

        assertThat(species.placedIn()).isEqualTo(new Termitoidae());
    }

    @Test
    void blattodea_monotonicity_termitoidaeDescendantOfBlattodea() {
        assertThat(isDescendantOrEqual(new Termitoidae(), new Blattodea())).isTrue();
    }

    @Test
    void blattodea_periplanetaAmericanaResolvesHemimetabolous() {
        InsectSpecies species = loadSpecies(
                TestInsectsIdentifiers.InsectSpecies.PeriplanetaAmericana.name);

        Optional<MetabolyTrait> trait = CladeTraversal.findTrait(
                species.placedIn(), MetabolyTrait.class, InsectClades::traitsFor);

        assertThat(trait).contains(new MetabolyTrait(new Hemimetabolous()));
    }

    @Test
    void blattodea_reticulitermesHesperusResolvesHemimetabolous() {
        InsectSpecies species = loadSpecies(
                TestInsectsIdentifiers.InsectSpecies.ReticulitermesHesperus.name);

        Optional<MetabolyTrait> trait = CladeTraversal.findTrait(
                species.placedIn(), MetabolyTrait.class, InsectClades::traitsFor);

        assertThat(trait).contains(new MetabolyTrait(new Hemimetabolous()));
    }

    // ===== 2.3 Bees within apoid wasps — rank-less clade =====

    @Test
    void bees_apidaeFamilyPlacedAtAnthophila() {
        InsectFamily family = loadFamily(
                TestInsectsIdentifiers.InsectFamily.Apidae.name);

        assertThat(family.placedIn()).isEqualTo(new Anthophila());
    }

    @Test
    void bees_crabronidaeFamilyPlacedAtApoidea() {
        InsectFamily family = loadFamily(
                TestInsectsIdentifiers.InsectFamily.Crabronidae.name);

        assertThat(family.placedIn()).isEqualTo(new Apoidea());
    }

    @Test
    void bees_monotonicity_anthophilaDescendantOfApoidea() {
        assertThat(isDescendantOrEqual(new Anthophila(), new Apoidea())).isTrue();
    }

    @Test
    void bees_apidaeResolvesHolometabolous() {
        InsectFamily family = loadFamily(
                TestInsectsIdentifiers.InsectFamily.Apidae.name);

        Optional<MetabolyTrait> trait = CladeTraversal.findTrait(
                family.placedIn(), MetabolyTrait.class, InsectClades::traitsFor);

        assertThat(trait).contains(new MetabolyTrait(new Holometabolous()));
    }

    @Test
    void bees_crabronidaeResolvesHolometabolous() {
        InsectFamily family = loadFamily(
                TestInsectsIdentifiers.InsectFamily.Crabronidae.name);

        Optional<MetabolyTrait> trait = CladeTraversal.findTrait(
                family.placedIn(), MetabolyTrait.class, InsectClades::traitsFor);

        assertThat(trait).contains(new MetabolyTrait(new Holometabolous()));
    }

    // -- Helpers --

    private InsectOrder loadOrder(InsectOrderName name) {
        return db.getNamed(InsectOrderTestEntitySource.class)
                .getByName(name).orElseThrow();
    }

    private InsectFamily loadFamily(InsectFamilyName name) {
        return db.getNamed(InsectFamilyTestEntitySource.class)
                .getByName(name).orElseThrow();
    }

    private InsectGenus loadGenus(InsectGenusName name) {
        return db.getNamed(InsectGenusTestEntitySource.class)
                .getByName(name).orElseThrow();
    }

    private InsectSpecies loadSpecies(InsectSpeciesName name) {
        return db.getNamed(InsectSpeciesTestEntitySource.class)
                .getByName(name).orElseThrow();
    }
}
