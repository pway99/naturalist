package com.naturalist.plants.phytochemistry;

import com.naturalist.atlas.*;
import com.naturalist.atlas.inmem.AtlasAssembly;
import com.naturalist.chemistry.TestChemistryIdentifiers.Compounds;
import com.naturalist.chemistry.compound.CompoundName;
import com.naturalist.data.NaturalistDatabaseExtension;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.plants.PlantName;
import com.naturalist.plants.TestPlantsIdentifiers.Plants;
import com.naturalist.plants.atlas.PlantCompoundReferences;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Lives in {@code com.naturalist.plants.phytochemistry} (not {@code .atlas})
 * so the test can see the package-private {@link PhytochemicalConstituentEntityQueryImpl}
 * and the protected-constructor {@link PhytochemicalConstituentEntityRepositoryMock}
 * without exposing either to the wider test classpath.
 */
class PlantCompoundReferencesTest {

    private static final CompoundName ARISTOLOCHIC_ACID_I = CompoundName.of("aristolochic-acid-i");
    private static final CompoundName ARISTOLOCHIC_ACID_II = CompoundName.of("aristolochic-acid-ii");

    @RegisterExtension
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    private final PhytochemicalConstituentRepository.PhytochemicalConstituentEntityRepository repository =
            new PhytochemicalConstituentEntityRepositoryMock(db);
    private final PhytochemicalConstituentQuery.PhytochemicalConstituentEntityQuery entityQuery =
            new PhytochemicalConstituentEntityQueryImpl(repository);
    private final PlantCompoundReferences provider = new PlantCompoundReferences(entityQuery);

    @Test
    void domainIsPlants() {
        assertThat(provider.domain()).isEqualTo(new DomainId.Plants());
    }

    @Test
    void referenceTypeIsCompoundName() {
        assertThat(provider.referenceType()).isEqualTo(CompoundName.class);
    }

    @Test
    void constructorRejectsNullEntityQuery() {
        assertThatThrownBy(() -> new PlantCompoundReferences(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("constituents");
    }

    @Test
    void aristolochicAcidIResolvesToCaliforniaPipevineAndItsConstituent() {
        List<EntityRef> refs = provider.referencesTo(ARISTOLOCHIC_ACID_I).toList();

        EntityRef plantRef = new EntityRef(new DomainId.Plants(), Plants.CaliforniaPipevine.name);
        EntityRef constituentRef = new EntityRef(
                new DomainId.Plants(),
                Plants.CaliforniaPipevine.Constituents.AristolochicAcidI);

        assertThat(refs).containsExactlyInAnyOrder(plantRef, constituentRef);
    }

    @Test
    void aristolochicAcidIIResolvesIndependentlyOfAristolochicAcidI() {
        List<EntityRef> refs = provider.referencesTo(ARISTOLOCHIC_ACID_II).toList();

        EntityRef plantRef = new EntityRef(new DomainId.Plants(), Plants.CaliforniaPipevine.name);
        EntityRef constituentRef = new EntityRef(
                new DomainId.Plants(),
                Plants.CaliforniaPipevine.Constituents.AristolochicAcidII);

        // The plant ref recurs across both compounds, but each compound's
        // result is computed independently — looking up AA-II returns its own
        // constituent, not AA-I's.
        assertThat(refs).containsExactlyInAnyOrder(plantRef, constituentRef);
    }

    @Test
    void thymolResolvesToCreepingThymeAndItsConstituent() {
        List<EntityRef> refs = provider.referencesTo(Compounds.Thymol.name).toList();

        EntityRef plantRef = new EntityRef(new DomainId.Plants(), Plants.CreepingThyme.name);
        EntityRef constituentRef = new EntityRef(
                new DomainId.Plants(),
                Plants.CreepingThyme.Constituents.Thymol);

        assertThat(refs).containsExactlyInAnyOrder(plantRef, constituentRef);
    }

    @Test
    void unknownCompoundReturnsEmpty() {
        assertThat(provider.referencesTo(Compounds.NotFound.name)).isEmpty();
    }

    @Test
    void nullTargetReturnsEmpty() {
        // Defensive — the kernel already maps null to an empty result at the
        // Atlas surface, so a provider should not throw on null.
        assertThat(provider.referencesTo(null)).isEmpty();
    }

    @Test
    void plantAndConstituentRefsAreDistinguishableByNameType() {
        List<EntityRef> refs = provider.referencesTo(ARISTOLOCHIC_ACID_I).toList();

        long plantNames = refs.stream().filter(r -> r.name() instanceof PlantName).count();
        long constituentNames = refs.stream()
                .filter(r -> r.name() instanceof PhytochemicalConstituentName)
                .count();

        assertThat(plantNames).isEqualTo(1);
        assertThat(constituentNames).isEqualTo(1);
    }

    @Test
    void atlasFindReferencesToGroupsResultsUnderPlantsDomain() {
        Atlas atlas = AtlasAssembly.from(List.<AtlasContribution>of(),
                List.<EntityReferences<?>>of(provider));

        Map<DomainId, List<EntityRef>> result = atlas.findReferencesTo(ARISTOLOCHIC_ACID_I);

        assertThat(result).containsOnlyKeys(new DomainId.Plants());
        assertThat(result.get(new DomainId.Plants()))
                .extracting(EntityRef::name)
                .containsExactlyInAnyOrder(
                        Plants.CaliforniaPipevine.name,
                        Plants.CaliforniaPipevine.Constituents.AristolochicAcidI);
    }

    @Test
    void atlasFindReferencesToReturnsEmptyForUnknownCompound() {
        Atlas atlas = AtlasAssembly.from(List.<AtlasContribution>of(),
                List.<EntityReferences<?>>of(provider));

        assertThat(atlas.findReferencesTo(Compounds.NotFound.name)).isEmpty();
    }

    @Test
    void atlasDomainsReferencingExposesPlantsForCompoundName() {
        Atlas atlas = AtlasAssembly.from(List.<AtlasContribution>of(),
                List.<EntityReferences<?>>of(provider));

        assertThat(atlas.domainsReferencing(CompoundName.class))
                .containsExactly(new DomainId.Plants());
    }
}
