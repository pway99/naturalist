package com.naturalist.chemistry;

import com.naturalist.chemistry.compound.CompoundQuery;
import com.naturalist.chemistry.compound.CompoundTestContext;
import com.naturalist.data.NaturalistDatabase;

/**
 * Pre-wired, in-memory read surface for the chemistry bounded context. Mirrors
 * {@code InsectsTestContext} — colocates the cross-sub-context wiring so the
 * package-private adapters in {@code chemistry-core} and the package-private
 * mocks in {@code chemistry-repository-test} can be assembled without
 * promoting any of them to public.
 *
 * <p><b>Read seam only.</b> Write-side test-data manipulation goes through
 * the supplied {@link NaturalistDatabase} and the domain's
 * {@code *TestEntitySource} classes; do not grow write methods on this class.
 *
 * <p><b>Not a JUnit extension.</b> Consumers that need per-method reset wrap
 * a {@code NaturalistDatabaseExtension} (from {@code framework-test})
 * alongside this context. Keeping the extension concern out preserves use
 * from non-test contexts (e.g. console bootstraps during pre-RDBMS
 * development).
 */
public class ChemistryTestContext {

    private final CompoundQuery compoundQuery;

    private ChemistryTestContext(NaturalistDatabase db) {
        this.compoundQuery = CompoundTestContext.createQuery(db);
    }

    public static ChemistryTestContext create(NaturalistDatabase db) {
        return new ChemistryTestContext(db);
    }

    public CompoundQuery compoundQuery() {
        return compoundQuery;
    }
}
