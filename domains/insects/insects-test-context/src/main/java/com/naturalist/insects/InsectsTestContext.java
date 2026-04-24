package com.naturalist.insects;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.insects.lifestage.InsectLifeStageQuery;
import com.naturalist.insects.lifestage.InsectLifeStageTestContext;

/**
 * Pre-wired, in-memory read surface for the insects bounded context. Colocates into
 * {@code com.naturalist.insects} so it can assemble the package-private namespace
 * internals ({@link InsectRepository}, {@link SpeciesRepositoryMock},
 * {@link InsectImageRepositoryMock}, and the {@code *QueryImpl} adapters in
 * {@code insects-core}) without promoting any of them to public.
 *
 * <p><b>Read seam only.</b> The only surface re-exposed to consumers is
 * {@link #insectQuery()} — the public {@link InsectQuery} namespace. Write-side
 * test-data manipulation goes through the supplied {@link NaturalistDatabase}
 * and the domain's {@code *TestEntitySource} classes; do not grow write methods
 * on this class.
 *
 * <p><b>Not a JUnit extension.</b> Consumers that need per-method reset wrap a
 * {@code NaturalistDatabaseExtension} (from {@code framework-test}) alongside
 * this context. Keeping the extension concern out of this class preserves use
 * from non-test contexts (e.g. console bootstraps during pre-RDBMS development).
 */
public class InsectsTestContext {
    private final InsectQuery insectQuery;
    private final InsectLifeStageQuery insectLifeStageQuery;

    private InsectsTestContext(NaturalistDatabase db) {
        InsectRepository repository = InsectRepository.create(
                new SpeciesRepositoryMock(db),
                new InsectImageRepositoryMock(db));
        InsectQuery.SpeciesQuery speciesQuery = new SpeciesQueryImpl(repository.speciesRepository);
        InsectQuery.ImageQuery imageQuery = new ImageQueryImpl(repository.imageRepository);
        this.insectQuery = new InsectQueryImpl(speciesQuery, imageQuery);
        this.insectLifeStageQuery = InsectLifeStageTestContext.createQuery(db);
    }

    public static InsectsTestContext create(NaturalistDatabase db) {
        return new InsectsTestContext(db);
    }

    public InsectQuery insectQuery() {
        return insectQuery;
    }

    public InsectLifeStageQuery insectLifeStageQuery() {
        return insectLifeStageQuery;
    }
}
