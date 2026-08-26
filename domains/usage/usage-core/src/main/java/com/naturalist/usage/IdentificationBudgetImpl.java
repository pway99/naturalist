package com.naturalist.usage;

import com.naturalist.infrastructure.DomainService;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.observability.Observer;

/**
 * Narrow write seam for insects: forwards to {@link UsageCommand#reserve}. Kept as
 * its own thin adapter (rather than having insects depend on {@link UsageCommand}
 * directly) so the wider command surface — {@code acknowledge},
 * {@code claimUnsentAlerts} — never leaks across the domain boundary insects
 * consumes.
 */
@DomainService
class IdentificationBudgetImpl implements IdentificationBudget {

    private final UsageCommand usageCommand;

    IdentificationBudgetImpl(UsageCommand usageCommand) {
        Observer.forClass(IdentificationBudgetImpl.class).arguments("constructor", i -> i
                        .notNull(usageCommand, "usageCommand"))
                .throwWhenInvalid();
        this.usageCommand = usageCommand;
    }

    @Override
    public void reserve(NaturalistName naturalist) {
        usageCommand.reserve(naturalist);
    }
}
