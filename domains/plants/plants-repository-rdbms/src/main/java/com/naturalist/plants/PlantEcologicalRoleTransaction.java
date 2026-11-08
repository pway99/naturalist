package com.naturalist.plants;

import com.naturalist.exception.EntityNotFoundException;
import com.naturalist.exception.PrimaryKeyConstraintException;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.persistence.RdbmsExceptions;
import jakarta.transaction.Transactional;

/**
 * The multi-statement writes of {@link PlantEcologicalRoleEntityRepositoryRdbms}, each run as one
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
class PlantEcologicalRoleTransaction {

    private final PlantEcologicalRoleMapper mapper;

    PlantEcologicalRoleTransaction(PlantEcologicalRoleMapper mapper) {
        this.mapper = mapper;
    }

    /** The parent row, then every child row. */
    @Transactional
    void insert(PlantEcologicalRole entity) {
        PlantEcologicalRoleDbo dbo = PlantEcologicalRoleDbo.from(entity);   // validates + throws
        try {
            mapper.insert(dbo);
        } catch (RuntimeException e) {
            if (RdbmsExceptions.isUniqueViolation(e)) throw new PrimaryKeyConstraintException(entity);
            throw e;
        }
        insertRoles(entity);
    }

    /** The parent row, then its child rows replaced wholesale. */
    @Transactional
    void update(PlantEcologicalRole entity) {
        PlantEcologicalRoleDbo dbo = PlantEcologicalRoleDbo.from(entity);
        if (mapper.updateById(dbo) == 0) throw new EntityNotFoundException(entity);
        mapper.deleteRoles(entity.id().value().toString());
        insertRoles(entity);
    }

    /** The existence check and the write it chooses, in one transaction. */
    @Transactional
    void save(PlantEcologicalRole entity) {
        if (mapper.selectById(entity.id().value().toString()) != null) update(entity);
        else insert(entity);
    }

    private void insertRoles(PlantEcologicalRole entity) {
        String id = entity.id().value().toString();
        for (PlantRole role : entity.roles()) {
            mapper.insertRole(PlantEcologicalRoleRoleDbo.from(id, role));
        }
    }
}
