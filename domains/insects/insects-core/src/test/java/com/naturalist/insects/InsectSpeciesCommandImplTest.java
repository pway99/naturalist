package com.naturalist.insects;

import com.naturalist.RandomValue;
import com.naturalist.data.EntityCommand;
import com.naturalist.data.EntityCommandContractTest;
import com.naturalist.data.EntityQuery;
import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.data.TestEntitySource;
import com.naturalist.fieldnotes.Description;
import com.naturalist.insects.InsectEntityCollections.SpeciesCollection;
import com.naturalist.taxonomy.TaxonomicSpecies;
import com.naturalist.test.context.Wired;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Set;

class InsectSpeciesCommandImplTest
        implements EntityCommandContractTest<InsectSpeciesName, InsectSpeciesInfo, SpeciesCollection> {

    @RegisterExtension
    NaturalistTestExtension nte = NaturalistTestExtension.create();

    @Wired InsectCommand.SpeciesCommand command;
    @Wired InsectQuery.SpeciesQuery query;

    @Override
    public EntityCommand<InsectSpeciesName, InsectSpeciesInfo> command() {
        return command;
    }

    @Override
    public EntityQuery<InsectSpeciesName, InsectSpeciesInfo, SpeciesCollection> query() {
        return query;
    }

    @Override
    public TestEntitySource<InsectSpeciesName, InsectSpeciesInfo> source() {
        return nte.getNamed(InsectSpeciesTestEntitySource.class);
    }

    @Override
    public InsectSpeciesName notFoundName() {
        return TestInsectsIdentifiers.InsectSpecies.NotFound.name;
    }

    @Override
    public List<InsectSpeciesName> knownEntityNames() {
        return List.of(
                TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name,
                TestInsectsIdentifiers.InsectSpecies.ColiasEurytheme.name);
    }

    @Override
    public InsectSpeciesInfo newEntity() {
        return new InsectSpeciesInfo(
                InsectSpeciesName.of("test-command-species-xx"),
                TestInsectsIdentifiers.InsectGenus.Chrysoperla.name,
                TaxonomicSpecies.of("nemoralis"),
                description(),
                Set.of(),
                null,
                null,
                null, null, null, null,
                null, null, null);
    }

    @Override
    public InsectSpeciesInfo ghostEntity() {
        return new InsectSpeciesInfo(
                InsectSpeciesName.of("test-command-ghost-xx"),
                TestInsectsIdentifiers.InsectGenus.Chrysoperla.name,
                TaxonomicSpecies.of("ghost"),
                description(),
                Set.of(),
                null,
                null,
                null, null, null, null,
                null, null, null);
    }

    @Override
    public InsectSpeciesInfo modifiedEntity(InsectSpeciesInfo original) {
        return new InsectSpeciesInfo(
                original.name(),
                TestInsectsIdentifiers.InsectGenus.Chrysoperla.name,
                TaxonomicSpecies.of("nemoralis"),
                description(),
                Set.of(),
                RandomValue.string(),
                null,
                null,
                new InsectSpeciesInfo.Voltinism(
                        InsectSpeciesInfo.Voltinism.VoltinismPattern.UNIVOLTINE,
                        RandomValue.string()),
                null,
                null,
                new InsectSpeciesInfo.GardenConnections(
                        List.of(RandomValue.string()),
                        RandomValue.string(),
                        RandomValue.string()),
                new InsectSpeciesInfo.BeneficialProfile(
                        RandomValue.string(),
                        RandomValue.string(),
                        RandomValue.string()),
                null);
    }

    private static Description description() {
        return new Description(
                RandomValue.string(), RandomValue.string(),
                RandomValue.string(), RandomValue.string());
    }
}
