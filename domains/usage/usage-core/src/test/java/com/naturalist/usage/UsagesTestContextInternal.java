package com.naturalist.usage;

import com.naturalist.test.context.Provisions;
import com.naturalist.test.context.TestContext;

import java.time.Clock;

/**
 * The test context of {@code com.naturalist.usage}. The graph builds the package's {@code @DomainService}
 * implementations ({@link UsageQueryImpl}, {@link UsageCommandImpl}, {@link IdentificationBudgetImpl},
 * {@link NoOpEntitlementLookup}) and its {@code @MockDomainService} repository mocks by reflection. This
 * context provides only what production takes from the composition root:
 *
 * <ul>
 *   <li>{@link WarningPercent}: the app's {@code UsageConfiguration} bean. 80, the value the app ships with,
 *       unless {@link #warningPercent(int)} chose another.</li>
 *   <li>{@link Clock}: an outside-world port, the UTC system clock. A test that needs a fixed instant
 *       declares {@code @Fake Clock}; {@code UsagesTestContext} hands over its caller's clock the same way.</li>
 *   <li>{@link EntitlementLookup}: production's {@link NoOpEntitlementLookup}, which entitles nobody, unless
 *       {@link #entitlements(EntitlementLookup)} chose another. The lookup is the seam a future credit-balance
 *       check plugs into; until that exists, the only way to exercise the entitled branch is to choose who is
 *       entitled here.</li>
 * </ul>
 *
 * <p>Choose a value before the first {@code get} that builds what needs it; a late choice fails.
 */
public class UsagesTestContextInternal extends TestContext {

    private int warningPercent = 80;
    private EntitlementLookup entitlements;
    private boolean warningPercentBuilt;
    private boolean entitlementsBuilt;

    public UsagesTestContextInternal() {
    }

    @Override
    protected void provisions(Provisions p) {
        p.provide(WarningPercent.class, g -> {
            warningPercentBuilt = true;
            return new WarningPercent(warningPercent);
        });
        p.provide(Clock.class, g -> Clock.systemUTC());
        p.provide(EntitlementLookup.class, g -> {
            entitlementsBuilt = true;
            return entitlements != null ? entitlements : g.get(NoOpEntitlementLookup.class);
        });
    }

    /** The warning threshold, in percent of each limit, the graph's query and command count against. */
    void warningPercent(int percent) {
        if (warningPercentBuilt) {
            throw new IllegalStateException("WarningPercent was already built; choose it before the first get");
        }
        this.warningPercent = percent;
    }

    /** Who the graph's command treats as entitled, in place of production's entitle-nobody lookup. */
    void entitlements(EntitlementLookup lookup) {
        if (entitlementsBuilt) {
            throw new IllegalStateException("EntitlementLookup was already built; choose it before the first get");
        }
        this.entitlements = lookup;
    }
}
