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
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Set;

class InsectSpeciesCommandImplTest
        implements EntityCommandContractTest<InsectSpeciesName, InsectSpecies, SpeciesCollection> {

    @RegisterExtension
    NaturalistTestExtension nte = NaturalistTestExtension.create();

    InsectSpeciesRepositoryMock repository = new InsectSpeciesRepositoryMock(nte);
    InsectCommand.SpeciesCommand command = new InsectSpeciesCommandImpl(repository);
    InsectQuery.GenusQuery genusQuery =
            new InsectGenusQueryImpl(new InsectGenusRepositoryMock(nte), new InsectFamilyQueryImpl(new InsectFamilyRepositoryMock(nte)));
    InsectQuery.SpeciesQuery query = new InsectSpeciesQueryImpl(repository, genusQuery);

    @Override
    public EntityCommand<InsectSpeciesName, InsectSpecies> command() {
        return command;
    }

    @Override
    public EntityQuery<InsectSpeciesName, InsectSpecies, SpeciesCollection> query() {
        return query;
    }

    @Override
    public TestEntitySource<InsectSpeciesName, InsectSpecies> source() {
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
    public InsectSpecies newEntity() {
        return new InsectSpecies(
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
    public InsectSpecies ghostEntity() {
        return new InsectSpecies(
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
    public InsectSpecies modifiedEntity(InsectSpecies original) {
        return new InsectSpecies(
                original.name(),
                TestInsectsIdentifiers.InsectGenus.Chrysoperla.name,
                TaxonomicSpecies.of("nemoralis"),
                description(),
                Set.of(),
                RandomValue.string(),
                null,
                null,
                new InsectSpecies.Voltinism(
                        InsectSpecies.Voltinism.VoltinismPattern.UNIVOLTINE,
                        RandomValue.string()),
                null,
                null,
                new InsectSpecies.GardenConnections(
                        List.of(RandomValue.string()),
                        RandomValue.string(),
                        RandomValue.string()),
                new InsectSpecies.BeneficialProfile(
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
