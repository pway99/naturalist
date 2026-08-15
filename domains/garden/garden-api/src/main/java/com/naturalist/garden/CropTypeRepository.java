package com.naturalist.garden;

import com.naturalist.data.EntityRepository;

/**
 * Repository port for {@link CropType}, the root entity of the {@link GardenPlan} aggregate.
 * Package-private (ADR-020); cross-domain access goes through the public {@code GardenPlanQuery}.
 */
interface CropTypeRepository extends EntityRepository<CropTypeName, CropType> {
}
