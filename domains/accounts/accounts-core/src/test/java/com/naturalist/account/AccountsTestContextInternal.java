package com.naturalist.account;

import com.naturalist.test.context.Provisions;
import com.naturalist.test.context.TestContext;

import java.time.Clock;

/**
 * The test context of {@code com.naturalist.account}. The graph builds the package's {@code @DomainService}
 * implementations ({@link AccountQueryImpl}, {@link AccountCommandImpl}) and its {@code @MockDomainService}
 * repository mocks by reflection. This context provides only what production takes from the composition
 * root (the app's {@code AccountConfiguration} and the shared clock):
 *
 * <ul>
 *   <li>{@link SecureTokens}: production's {@link SecureTokens#jdk()}. Tokens are random, so a test reads a
 *       token's hash back through the graph's {@code SecureTokens}, never a value it chose.</li>
 *   <li>{@link AccountPolicy}: {@link AccountPolicy#selfServe()}, the default of
 *       {@code naturalist.access.grant-policy}, unless {@link #policy(AccountPolicy)} chose another.</li>
 *   <li>{@link Clock}: an outside-world port, the UTC system clock. A test that needs a fixed or moving
 *       instant declares {@code @Fake Clock}.</li>
 * </ul>
 *
 * <p>Choose the policy before the first {@code get} that builds what needs it; a late choice fails.
 */
public class AccountsTestContextInternal extends TestContext {

    private AccountPolicy policy = AccountPolicy.selfServe();
    private boolean policyBuilt;

    public AccountsTestContextInternal() {
    }

    @Override
    protected void provisions(Provisions p) {
        p.provide(SecureTokens.class, g -> SecureTokens.jdk());
        p.provide(AccountPolicy.class, g -> {
            policyBuilt = true;
            return policy;
        });
        p.provide(Clock.class, g -> Clock.systemUTC());
    }

    /** How the graph's command grants vision access, in place of the self-serve default. */
    void policy(AccountPolicy chosen) {
        if (policyBuilt) {
            throw new IllegalStateException("AccountPolicy was already built; choose it before the first get");
        }
        this.policy = chosen;
    }
}
