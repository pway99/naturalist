package com.naturalist.chemistry.product;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

/**
 * Intermediate product repository: the production-named adapter the app wires,
 * temporarily backed by {@link ProductEntityRepositoryMock} until the SQL body
 * lands (design: docs/plans/2026-08-22-repository-rdms-intermediate-design.md).
 */
@DomainService
class ProductEntityRepositoryRdms extends ProductEntityRepositoryMock {
    ProductEntityRepositoryRdms(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
