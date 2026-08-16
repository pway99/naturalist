package com.naturalist.plants.phytochemistry;

import com.naturalist.catalog.*;
import com.naturalist.catalog.inmem.CatalogAssembly;
import com.naturalist.chemistry.TestChemistryIdentifiers.Compounds;
import com.naturalist.chemistry.compound.CompoundName;
import com.naturalist.data.NaturalistDatabaseExtension;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.plants.PlantSpeciesName;
import com.naturalist.plants.PlantsDomain;
import com.naturalist.plants.TestPlantsIdentifiers.PlantGenera;
import com.naturalist.plants.TestPlantsIdentifiers.Plants;
import com.naturalist.plants.catalog.PlantsCompoundReferences;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Lives in {@code com.naturalist.plants.phytochemistry} (not {@code .catalog})
 * so the test can see the package-private {@link PhytochemicalConstituentQueryImpl}
 * and the package-private {@link PhytochemicalConstituentRepositoryMock}
 * without exposing either to the wider test classpath.
 */
class PlantsCompoundReferencesTest {

    private static final CompoundName ARISTOLOCHIC_ACID_I = CompoundName.of("aristolochic-acid-i");
    private static final CompoundName ARISTOLOCHIC_ACID_II = CompoundName.of("aristolochic-acid-ii");

    @RegisterExtension
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    private final PhytochemicalConstituentRepository repository =
            new PhytochemicalConstituentRepositoryMock(db);
    private final PhytochemicalConstituentQuery entityQuery =
            new PhytochemicalConstituentQueryImpl(repository);
    private final PlantsCompoundReferences provider = new PlantsCompoundReferences(entityQuery);

    @Test
    void domainIsPlants() {
        assertThat(provider.domain()).isEqualTo(new PlantsDomain());
    }

    @Test
    void referenceTypeIsCompoundName() {
        assertThat(provider.referenceType()).isEqualTo(CompoundName.class);
    }

    @Test
    void constructorRejectsNullEntityQuery() {
        assertThatThrownBy(() -> new PlantsCompoundReferences(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("constituents");
    }

    @Test
    void aristolochicAcidIResolvesToCaliforniaPipevineAndItsConstituent() {
        List<EntityRef> refs = provider.referencesTo(ARISTOLOCHIC_ACID_I).toList();

        EntityRef plantRef = new EntityRef(new PlantsDomain(), Plants.CaliforniaPipevine.name);
        EntityRef constituentRef = new EntityRef(
                new PlantsDomain(),
                Plants.CaliforniaPipevine.Constituents.AristolochicAcidI);

        assertThat(refs).containsExactlyInAnyOrder(plantRef, constituentRef);
    }

    @Test
    void aristolochicAcidIIResolvesIndependentlyOfAristolochicAcidI() {
        List<EntityRef> refs = provider.referencesTo(ARISTOLOCHIC_ACID_II).toList();

        EntityRef plantRef = new EntityRef(new PlantsDomain(), Plants.CaliforniaPipevine.name);
        EntityRef constituentRef = new EntityRef(
                new PlantsDomain(),
                Plants.CaliforniaPipevine.Constituents.AristolochicAcidII);

        // The plant ref recurs across both compounds, but each compound's
        // result is computed independently — looking up AA-II returns its own
        // constituent, not AA-I's.
        assertThat(refs).containsExactlyInAnyOrder(plantRef, constituentRef);
    }

    @Test
    void thymolResolvesToTheThymusGenusAndItsConstituent() {
        List<EntityRef> refs = provider.referencesTo(Compounds.Thymol.name).toList();

        EntityRef plantRef = new EntityRef(new PlantsDomain(), PlantGenera.Thymus.name);
        EntityRef constituentRef = new EntityRef(
                new PlantsDomain(),
                PlantGenera.Thymus.Constituents.Thymol);

        assertThat(refs).containsExactlyInAnyOrder(plantRef, constituentRef);
    }

    @Test
    void unknownCompoundReturnsEmpty() {
        assertThat(provider.referencesTo(Compounds.NotFound.name)).isEmpty();
    }

    @Test
    void nullTargetReturnsEmpty() {
        // Defensive — the kernel already maps null to an empty result at the
        // Catalog surface, so a provider should not throw on null.
        assertThat(provider.referencesTo(null)).isEmpty();
    }

    @Test
    void plantAndConstituentRefsAreDistinguishableByNameType() {
        List<EntityRef> refs = provider.referencesTo(ARISTOLOCHIC_ACID_I).toList();

        long plantNames = refs.stream().filter(r -> r.name() instanceof PlantSpeciesName).count();
        long constituentNames = refs.stream()
                .filter(r -> r.name() instanceof PhytochemicalConstituentName)
                .count();

        assertThat(plantNames).isEqualTo(1);
        assertThat(constituentNames).isEqualTo(1);
    }

    @Test
    void catalogFindReferencesToGroupsResultsUnderPlantsDomain() {
        Catalog catalog = CatalogAssembly.from(List.<CatalogContribution>of(),
                List.<EntityReferences<?>>of(provider));

        Map<DomainId, List<EntityRef>> result = catalog.findReferencesTo(ARISTOLOCHIC_ACID_I);

        assertThat(result).containsOnlyKeys(new PlantsDomain());
        assertThat(result.get(new PlantsDomain()))
                .extracting(EntityRef::name)
                .containsExactlyInAnyOrder(
                        Plants.CaliforniaPipevine.name,
                        Plants.CaliforniaPipevine.Constituents.AristolochicAcidI);
    }

    @Test
    void catalogFindReferencesToReturnsEmptyForUnknownCompound() {
        Catalog catalog = CatalogAssembly.from(List.<CatalogContribution>of(),
                List.<EntityReferences<?>>of(provider));

        assertThat(catalog.findReferencesTo(Compounds.NotFound.name)).isEmpty();
    }

    @Test
    void catalogDomainsReferencingExposesPlantsForCompoundName() {
        Catalog catalog = CatalogAssembly.from(List.<CatalogContribution>of(),
                List.<EntityReferences<?>>of(provider));

        assertThat(catalog.domainsReferencing(CompoundName.class))
                .containsExactly(new PlantsDomain());
    }
}
