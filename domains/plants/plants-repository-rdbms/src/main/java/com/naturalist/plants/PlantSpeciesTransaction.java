package com.naturalist.plants;

import com.naturalist.biogeography.Bioregion;
import com.naturalist.exception.EntityNotFoundException;
import com.naturalist.exception.PrimaryKeyConstraintException;
import com.naturalist.fieldnotes.CommonName;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.persistence.RdbmsExceptions;
import jakarta.transaction.Transactional;

/**
 * The multi-statement writes of {@link PlantSpeciesEntityRepositoryRdbms}, each run as one
 * transaction: the parent row and its child rows commit or roll back together, so a failed child
 * insert leaves no partial row behind and a failed update does not leave the children it deleted
 * gone.
 *
 * <p>A separate bean, not annotations on the adapter: the adapter's write entry points are the
 * {@code final} {@code insert} / {@code update} / {@code save} of
 * {@link com.naturalist.data.AbstractEntityRepository}, which CGLIB cannot override, and their
 * calls to {@code doInsert} et al. are self-invocations no proxy sees. Nothing here is
 * {@code final}, for the same reason. Inside a caller's transaction these join it. Constructed
 * by hand (the ITs and the seeder) no proxy exists and the caller's own {@code SqlSession} is
 * the transaction.
 */
@DomainService
class PlantSpeciesTransaction {

    private final PlantSpeciesMapper mapper;

    PlantSpeciesTransaction(PlantSpeciesMapper mapper) {
        this.mapper = mapper;
    }

    /** The parent row, then every child row. */
    @Transactional
    void insert(PlantSpecies entity) {
        PlantSpeciesDbo dbo = PlantSpeciesDbo.from(entity);   // validates the parent scalars + throws
        int inserted;
        try {
            inserted = mapper.insert(dbo);
        } catch (RuntimeException e) {
            if (RdbmsExceptions.isUniqueViolation(e)) throw new PrimaryKeyConstraintException(entity);
            throw e;
        }
        if (inserted == 0) throw new EntityNotFoundException(entity);   // referenced genus absent
        insertChildren(entity);
    }

    /** The parent row, then its child rows replaced wholesale. */
    @Transactional
    void update(PlantSpecies entity) {
        PlantSpeciesDbo dbo = PlantSpeciesDbo.from(entity);
        if (mapper.updateByName(dbo) == 0) throw new EntityNotFoundException(entity);
        String name = entity.name().value();
        mapper.deleteCommonNames(name);
        mapper.deleteNativeBioregions(name);
        insertChildren(entity);
    }

    /** The existence check and the write it chooses, in one transaction. */
    @Transactional
    void save(PlantSpecies entity) {
        if (mapper.selectByName(entity.name().value()) != null) update(entity);
        else insert(entity);
    }

    private void insertChildren(PlantSpecies s) {
        PlantSpeciesName name = s.name();
        for (CommonName commonName : s.commonNames()) {
            mapper.insertCommonName(PlantSpeciesCommonNameDbo.from(name, commonName));
        }
        for (Bioregion bioregion : s.nativeBioregions()) {
            mapper.insertNativeBioregion(PlantSpeciesNativeBioregionDbo.from(name, bioregion));
        }
    }
}
