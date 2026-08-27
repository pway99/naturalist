package com.naturalist.usage;

import com.naturalist.infrastructure.DomainService;
import com.naturalist.naturalist.NaturalistName;

/**
 * Default {@link EntitlementLookup} that never entitles a naturalist — the
 * production adapter until a real credit-balance check is wired. Equivalent to
 * {@link EntitlementLookup#none()}, registered as a {@code @DomainService} bean
 * so composition roots wire it the same way as every other adapter.
 */
@DomainService
class NoOpEntitlementLookup implements EntitlementLookup {

    @Override
    public boolean isEntitled(NaturalistName naturalist) {
        return false;
    }
}
