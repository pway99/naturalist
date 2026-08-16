package com.naturalist.plants.phytochemistry;

import com.naturalist.RandomValue;
import com.naturalist.chemistry.compound.CompoundName;
import com.naturalist.fieldnotes.Description;
import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.MethodObserver;
import com.naturalist.observability.Observer;
import com.naturalist.plants.PlantSpeciesName;
import com.naturalist.plants.phytochemistry.role.PhytochemicalRole;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PhytochemicalConstituentTest {

    private static final Observer observer = Observer.forClass(PhytochemicalConstituentTest.class);

    @Test
    void fullyPopulatedConstituentIsValid() {
        MethodObserver mo = observer.forMethod("fullyPopulatedConstituentIsValid");
        PhytochemicalConstituent constituent = constituent(
                Set.of(new PhytochemicalRole.HerbivoreDeterrent()),
                Set.of(PlantTissue.LEAF, PlantTissue.ROOT));

        InvariantObservation result = mo.namedEntity(constituent, "constituent");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void emptyRolesIsInvalid() {
        // A constituent with no role is data without a story — "not yet
        // characterised" is expressed by the absence of the record, not by an
        // empty role set on a present one.
        MethodObserver mo = observer.forMethod("emptyRolesIsInvalid");
        PhytochemicalConstituent constituent = constituent(Set.of(), Set.of(PlantTissue.LEAF));

        InvariantObservation result = mo.namedEntity(constituent, "constituent");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactly(".constituent.roles");
    }

    @Test
    void emptyTissuesIsInvalid() {
        // Unknown tissue is recorded as WHOLE_PLANT, never as an empty set.
        MethodObserver mo = observer.forMethod("emptyTissuesIsInvalid");
        PhytochemicalConstituent constituent = constituent(
                Set.of(new PhytochemicalRole.HerbivoreDeterrent()), Set.of());

        InvariantObservation result = mo.namedEntity(constituent, "constituent");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactly(".constituent.tissues");
    }

    @Test
    void wholePlantTissueIsTheEncodingForUnknown() {
        MethodObserver mo = observer.forMethod("wholePlantTissueIsTheEncodingForUnknown");
        PhytochemicalConstituent constituent = constituent(
                Set.of(new PhytochemicalRole.HerbivoreDeterrent()),
                Set.of(PlantTissue.WHOLE_PLANT));

        InvariantObservation result = mo.namedEntity(constituent, "constituent");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void allNullComponentsReportEveryViolation() {
        MethodObserver mo = observer.forMethod("allNullComponentsReportEveryViolation");
        PhytochemicalConstituent constituent =
                new PhytochemicalConstituent(null, null, null, null, null, null, null, null, null);

        InvariantObservation result = mo.namedEntity(constituent, "constituent");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(
                        ".constituent.name",
                        ".constituent.plantName",
                        ".constituent.compoundName",
                        ".constituent.description",
                        ".constituent.category",
                        ".constituent.roles",
                        ".constituent.tissues",
                        ".constituent.induction");
    }

    @Test
    void axisPredicatesRollUpTheSealedRoleHierarchy() {
        // The axis-level methods are the stable consumer surface — a new
        // PhytochemicalRole permit must land in one of these rather than
        // forcing call sites to pattern-match the sealed type.
        PhytochemicalConstituent defensive = constituent(
                Set.of(new PhytochemicalRole.HerbivoreDeterrent(),
                        new PhytochemicalRole.AntiFungal()),
                Set.of(PlantTissue.LEAF));
        PhytochemicalConstituent medicinal = constituent(
                Set.of(new PhytochemicalRole.Pharmaceutical()),
                Set.of(PlantTissue.ROOT));
        PhytochemicalConstituent toxic = constituent(
                Set.of(new PhytochemicalRole.HumanToxin()),
                Set.of(PlantTissue.SEED));

        assertThat(defensive.isDefensive()).isTrue();
        assertThat(defensive.hasMedicinalApplication()).isFalse();
        assertThat(medicinal.hasMedicinalApplication()).isTrue();
        assertThat(medicinal.isDefensive()).isFalse();
        assertThat(toxic.isToxicToMammals()).isTrue();
        assertThat(defensive.isToxicToMammals()).isFalse();
    }

    @Test
    void playsRoleAndIsPresentInReadTheirSets() {
        PhytochemicalConstituent constituent = constituent(
                Set.of(new PhytochemicalRole.HerbivoreDeterrent()),
                Set.of(PlantTissue.LEAF, PlantTissue.ROOT));

        assertThat(constituent.playsRole(new PhytochemicalRole.HerbivoreDeterrent())).isTrue();
        assertThat(constituent.playsRole(new PhytochemicalRole.AntiFungal())).isFalse();
        assertThat(constituent.isPresentIn(PlantTissue.LEAF)).isTrue();
        assertThat(constituent.isPresentIn(PlantTissue.FLOWER)).isFalse();
    }

    @Test
    void inductionPredicatesReadTheEnum() {
        PhytochemicalConstituent constitutive = new PhytochemicalConstituent(
                PhytochemicalConstituentName.of("creeping-thyme-thymol"),
                PlantSpeciesName.of("creeping-thyme"),
                CompoundName.of("thymol"),
                description(),
                PhytochemicalCategory.TERPENOID,
                Set.of(new PhytochemicalRole.AntiMicrobial()),
                Set.of(PlantTissue.LEAF),
                InductionMode.CONSTITUTIVE,
                null);

        assertThat(constitutive.isInduced()).isFalse();
        assertThat(constitutive.isDevelopmental()).isFalse();
    }

    private static PhytochemicalConstituent constituent(
            Set<PhytochemicalRole> roles, Set<PlantTissue> tissues) {
        return new PhytochemicalConstituent(
                PhytochemicalConstituentName.of(
                        PlantSpeciesName.of("aristolochia-californica"),
                        CompoundName.of("aristolochic-acid-i")),
                PlantSpeciesName.of("aristolochia-californica"),
                CompoundName.of("aristolochic-acid-i"),
                description(),
                PhytochemicalCategory.ALKALOID,
                roles,
                tissues,
                InductionMode.CONSTITUTIVE,
                null);
    }

    private static Description description() {
        return new Description(
                RandomValue.string(), RandomValue.string(),
                RandomValue.string(), RandomValue.string());
    }
}
