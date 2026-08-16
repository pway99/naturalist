package com.naturalist.plants;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.plants.cultivar.CultivarQuery;
import com.naturalist.plants.cultivar.CultivarTestContext;
import com.naturalist.plants.heritage.SeedLineageQuery;
import com.naturalist.plants.heritage.SeedLineageTestContext;
import com.naturalist.plants.management.PlantProgramQuery;
import com.naturalist.plants.management.PlantProgramTestContext;
import com.naturalist.plants.phytochemistry.PhytochemicalConstituentQuery;
import com.naturalist.plants.phytochemistry.PhytochemicalConstituentTestContext;

/**
 * Pre-wired, in-memory read surface for the plants bounded context. Mirrors
 * {@code ChemistryTestContext} and {@code InsectsTestContext} — colocates the
 * cross-sub-context wiring so the package-private adapters in
 * {@code plants-core} and the package-private mocks in
 * {@code plants-repository-test} can be assembled without promoting any of
 * them to public.
 *
 * <p><b>Single test context per namespace.</b> Because the {@link PlantSpecies}
 * entity lives at the {@code com.naturalist.plants} root package — the same
 * package this class lives in — the PlantSpecies query assembly is inlined here
 * rather than living in a separate {@code PlantTestContext}. The four
 * sub-context packages each get their own
 * {@code <Subject>TestContext} ({@link CultivarTestContext},
 * {@link SeedLineageTestContext}, {@link PlantProgramTestContext},
 * {@link PhytochemicalConstituentTestContext}) — one test context per
 * namespace.
 *
 * <p><b>Read seam only.</b> Write-side test-data manipulation goes through
 * the supplied {@link NaturalistDatabase} and the domain's
 * {@code *TestEntitySource} classes; do not grow write methods on this class.
 *
 * <p><b>Not a JUnit extension.</b> Consumers that need per-method reset wrap
 * a {@code NaturalistDatabaseExtension} (from {@code framework-test})
 * alongside this context. Keeping the extension concern out preserves use
 * from non-test contexts (e.g. the eventual plants-console bootstrap during
 * pre-RDBMS development).
 */
public class PlantsTestContext {

    private final PlantQuery plantQuery;
    private final CultivarQuery cultivarQuery;
    private final SeedLineageQuery seedLineageQuery;
    private final PlantProgramQuery plantProgramQuery;
    private final PhytochemicalConstituentQuery phytochemicalConstituentQuery;

    private PlantsTestContext(NaturalistDatabase db) {
        this.plantQuery = createPlantQuery(db);
        this.cultivarQuery = CultivarTestContext.createQuery(db);
        this.seedLineageQuery = SeedLineageTestContext.createQuery(db);
        this.plantProgramQuery = PlantProgramTestContext.createQuery(db);
        this.phytochemicalConstituentQuery = PhytochemicalConstituentTestContext.createQuery(db);
    }

    public static PlantsTestContext create(NaturalistDatabase db) {
        return new PlantsTestContext(db);
    }

    public PlantQuery plantQuery() {
        return plantQuery;
    }

    public CultivarQuery cultivarQuery() {
        return cultivarQuery;
    }

    public SeedLineageQuery seedLineageQuery() {
        return seedLineageQuery;
    }

    public PlantProgramQuery plantProgramQuery() {
        return plantProgramQuery;
    }

    public PhytochemicalConstituentQuery phytochemicalConstituentQuery() {
        return phytochemicalConstituentQuery;
    }

    /**
     * PlantSpecies query assembly. Inlined here rather than in a separate
     * {@code PlantTestContext} because the PlantSpecies entity lives at the plants
     * root package and a sibling test context would collide with this class
     * in the same namespace.
     */
    private static PlantQuery createPlantQuery(NaturalistDatabase db) {
        PlantQuery.PlantGenusEntityQuery genusQuery =
                new PlantGenusEntityQueryImpl(new PlantGenusEntityRepositoryMock(db));
        PlantQuery.PlantEntityQuery entityQuery =
                new PlantEntityQueryImpl(new PlantSpeciesEntityRepositoryMock(db), genusQuery);
        PlantQuery.PlantFamilyEntityQuery familyQuery =
                new PlantFamilyEntityQueryImpl(new PlantFamilyEntityRepositoryMock(db));
        PlantQuery.PlantOrderEntityQuery orderQuery =
                new PlantOrderEntityQueryImpl(new PlantOrderEntityRepositoryMock(db));
        PlantQuery.PlantEcologicalRoleEntityQuery roleQuery =
                new PlantEcologicalRoleEntityQueryImpl(new PlantEcologicalRoleEntityRepositoryMock(db));
        return new PlantQueryImpl(entityQuery, orderQuery, familyQuery, genusQuery, roleQuery);
    }
}
