package com.naturalist.insects.lifestage;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

/**
 * Intermediate insects life-stage repository: production-named adapter,
 * temporarily backed by {@link InsectLifeStageEntityRepositoryMock}.
 */
@DomainService
class InsectLifeStageEntityRepositoryRdms extends InsectLifeStageEntityRepositoryMock {
    InsectLifeStageEntityRepositoryRdms(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
