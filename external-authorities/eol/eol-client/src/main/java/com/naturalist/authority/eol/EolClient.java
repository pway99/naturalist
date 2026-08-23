package com.naturalist.authority.eol;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.resilience.ResilienceExempt;

/**
 * Production EOL {@link com.naturalist.authority.ExternalAuthority} adapter — the
 * discoverable bean the app wires for external-authority lookups.
 *
 * <p><b>Intermediate state.</b> For now this simply extends {@link EolClientMock},
 * so the deployment runs on a production-named, {@code @DomainService}-discovered
 * bean that is temporarily backed by the in-memory fixture authority. This mirrors
 * the {@code <Entity>RepositoryRdms extends <Entity>RepositoryMock} pattern used for
 * repositories while real persistence does not yet exist.
 *
 * <p><b>The real swap.</b> When the real EOL HTTP client is written (OkHttp against
 * the EOL search + pages APIs), replace {@code extends EolClientMock} with the HTTP
 * body, drop the {@code eol-client-mock} dependency, and swap {@link ResilienceExempt}
 * for {@code @Resilient(name = "authority.eol")} wrapping every call through the
 * {@code Resilience} facade (ADR-026). The bean type, wiring, and every consumer stay
 * put. The design, EOL API notes, and swap checklist live in
 * {@code docs/plans/2026-08-23-eol-client-design.md}.
 */
@DomainService
@ResilienceExempt(reason = "delegates to the in-memory EolClientMock until the real EOL HTTP client lands")
public class EolClient extends EolClientMock {

    EolClient(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
