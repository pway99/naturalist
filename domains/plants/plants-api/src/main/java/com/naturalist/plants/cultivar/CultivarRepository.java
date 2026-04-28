package com.naturalist.plants.cultivar;

import com.naturalist.data.EntityRepository;

class CultivarRepository {
    protected interface CultivarEntityRepository
            extends EntityRepository<CultivarName, Cultivar> {}
}
