package com.naturalist.insects;

import com.naturalist.RandomValue;
import com.naturalist.data.EntityCommand;
import com.naturalist.data.EntityCommandContractTest;
import com.naturalist.data.EntityQuery;
import com.naturalist.data.NaturalistDatabaseExtension;
import com.naturalist.data.TestEntitySource;
import com.naturalist.fieldnotes.Description;
import com.naturalist.insects.InsectEntityCollections.SpeciesCollection;
import com.naturalist.taxonomy.TaxonomicClassification;
import com.naturalist.taxonomy.TaxonomicFamily;
import com.naturalist.taxonomy.TaxonomicGenus;
import com.naturalist.taxonomy.TaxonomicOrder;
import com.naturalist.taxonomy.TaxonomicSpecies;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Set;

class SpeciesCommandImplTest
        implements EntityCommandContractTest<InsectSpeciesName, InsectSpecies, SpeciesCollection> {

    @RegisterExtension
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    SpeciesRepositoryMock repository = new SpeciesRepositoryMock(db);
    InsectCommand.SpeciesCommand command = new SpeciesCommandImpl(repository);
    InsectQuery.SpeciesQuery query = new SpeciesQueryImpl(repository);

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
        return db.getNamed(InsectSpeciesTestEntitySource.class);
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
                taxonomy(),
                null, null,
                description(),
                Set.of(),
                null, null,
                null,
                null, null, null, null,
                null, null, null, null, null, null, null);
    }

    @Override
    public InsectSpecies ghostEntity() {
        return new InsectSpecies(
                InsectSpeciesName.of("test-command-ghost-xx"),
                taxonomy(),
                null, null,
                description(),
                Set.of(),
                null, null,
                null,
                null, null, null, null,
                null, null, null, null, null, null, null);
    }

    @Override
    public InsectSpecies modifiedEntity(InsectSpecies original) {
        return new InsectSpecies(
                original.name(),
                taxonomy(),
                null, null,
                description(),
                Set.of(),
                RandomValue.string(),
                new InsectSpecies.IdentificationFeatures(List.of(RandomValue.string())),
                null,
                null, null, null, null,
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

    private static TaxonomicClassification taxonomy() {
        return new TaxonomicClassification(
                TaxonomicOrder.of("Coleoptera"),
                TaxonomicFamily.of("Carabidae"),
                TaxonomicGenus.of("Carabus"),
                TaxonomicSpecies.of("nemoralis"));
    }

    private static Description description() {
        return new Description(
                RandomValue.string(), RandomValue.string(),
                RandomValue.string(), RandomValue.string());
    }
}
