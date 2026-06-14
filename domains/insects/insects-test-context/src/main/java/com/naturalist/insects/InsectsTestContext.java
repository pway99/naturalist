package com.naturalist.insects;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.insects.lifestage.InsectLifeStageQuery;
import com.naturalist.insects.lifestage.InsectLifeStageTestContext;
import com.naturalist.library.CitationAssociationQuery;
import com.naturalist.library.LibraryTestContext;

/**
 * Pre-wired, in-memory read and write surface for the insects bounded context.
 * Colocates into {@code com.naturalist.insects} so it can assemble the
 * package-private namespace internals ({@link InsectRepository},
 * {@link SpeciesRepositoryMock}, {@link InsectImageRepositoryMock}, and the
 * {@code *QueryImpl} / {@code *CommandImpl} adapters in {@code insects-core})
 * without promoting any of them to public.
 *
 * <p>Two surfaces are re-exposed to consumers:
 * <ul>
 *   <li>{@link #insectQuery()} — the public {@link InsectQuery} namespace for reads.</li>
 *   <li>{@link #insectCommand()} — the public {@link InsectCommand} namespace for writes
 *       (insert / update). The pilot covers {@link InsectSpecies} and {@link InsectImage};
 *       family and genus commands are not wired.</li>
 * </ul>
 * Bulk test-data seeding still goes through the supplied {@link NaturalistDatabase}
 * and the domain's {@code *TestEntitySource} classes — the command surface exists for
 * exercising the public write port end-to-end, not for fixture loading.
 *
 * <p><b>Not a JUnit extension.</b> Consumers that need per-method reset wrap a
 * {@code NaturalistDatabaseExtension} (from {@code framework-test}) alongside
 * this context. Keeping the extension concern out of this class preserves use
 * from non-test contexts (e.g. console bootstraps during pre-RDBMS development).
 */
public class InsectsTestContext {
    private final InsectQuery insectQuery;
    private final InsectCommand insectCommand;
    private final InsectLifeStageQuery insectLifeStageQuery;

    private InsectsTestContext(NaturalistDatabase db) {
        InsectRepository repository = InsectRepository.create(
                new SpeciesRepositoryMock(db),
                new InsectImageRepositoryMock(db),
                new FamilyRepositoryMock(db),
                new GenusRepositoryMock(db),
                new InsectFunctionalRoleRepositoryMock(db),
                new OrderRepositoryMock(db));
        InsectQuery.FamilyQuery familyQuery = new FamilyQueryImpl(repository.familyRepository);
        InsectQuery.GenusQuery genusQuery = new GenusQueryImpl(repository.genusRepository, familyQuery);
        InsectQuery.SpeciesQuery speciesQuery = new SpeciesQueryImpl(repository.speciesRepository, genusQuery);
        InsectQuery.ImageQuery imageQuery = new ImageQueryImpl(repository.imageRepository);
        InsectQuery.FunctionalRoleQuery functionalRoleQuery =
                new FunctionalRoleQueryImpl(repository.functionalRoleRepository);
        InsectQuery.OrderQuery orderQuery = new OrderQueryImpl(repository.orderRepository);
        CitationAssociationQuery citationAssociationQuery =
                LibraryTestContext.create(db).citationAssociationQuery();
        this.insectQuery = new InsectQueryImpl(
                speciesQuery, imageQuery, familyQuery, genusQuery, functionalRoleQuery,
                orderQuery, citationAssociationQuery);
        InsectCommand.SpeciesCommand speciesCommand = new SpeciesCommandImpl(repository.speciesRepository);
        InsectCommand.ImageCommand imageCommand = new ImageCommandImpl(repository.imageRepository);
        this.insectCommand = new InsectCommandImpl(speciesCommand, imageCommand);
        this.insectLifeStageQuery = InsectLifeStageTestContext.createQuery(db);
    }

    public static InsectsTestContext create(NaturalistDatabase db) {
        return new InsectsTestContext(db);
    }

    public InsectQuery insectQuery() {
        return insectQuery;
    }

    public InsectCommand insectCommand() {
        return insectCommand;
    }

    public InsectLifeStageQuery insectLifeStageQuery() {
        return insectLifeStageQuery;
    }
}
