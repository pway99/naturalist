package com.naturalist.plants.phytochemistry;

import com.naturalist.exception.EntityNotFoundException;
import com.naturalist.exception.PrimaryKeyConstraintException;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.persistence.RdbmsExceptions;
import com.naturalist.plants.phytochemistry.role.PhytochemicalRole;
import jakarta.transaction.Transactional;

/**
 * The multi-statement writes of {@link PhytochemicalConstituentEntityRepositoryRdbms}, each run as
 * one transaction: the parent row and its child rows commit or roll back together, so a failed
 * child insert leaves no partial row behind and a failed update does not leave the children it
 * deleted gone.
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
class PhytochemicalConstituentTransaction {

    private final PhytochemicalConstituentMapper mapper;

    PhytochemicalConstituentTransaction(PhytochemicalConstituentMapper mapper) {
        this.mapper = mapper;
    }

    /** The parent row, then every child row. */
    @Transactional
    void insert(PhytochemicalConstituent entity) {
        PhytochemicalConstituentDbo dbo = PhytochemicalConstituentDbo.from(entity);   // validates + throws
        try {
            mapper.insertConstituent(dbo);
        } catch (RuntimeException e) {
            if (RdbmsExceptions.isUniqueViolation(e)) throw new PrimaryKeyConstraintException(entity);
            throw e;
        }
        insertChildren(entity);
    }

    /** The parent row, then its child rows replaced wholesale. */
    @Transactional
    void update(PhytochemicalConstituent entity) {
        PhytochemicalConstituentDbo dbo = PhytochemicalConstituentDbo.from(entity);
        if (mapper.updateConstituent(dbo) == 0) throw new EntityNotFoundException(entity);
        String name = entity.name().value();
        mapper.deleteRoles(name);
        mapper.deleteTissues(name);
        insertChildren(entity);
    }

    /** The existence check and the write it chooses, in one transaction. */
    @Transactional
    void save(PhytochemicalConstituent entity) {
        if (mapper.selectByName(entity.name().value()) != null) update(entity);
        else insert(entity);
    }

    private void insertChildren(PhytochemicalConstituent c) {
        PhytochemicalConstituentName name = c.name();
        for (PhytochemicalRole role : c.roles()) {
            mapper.insertRole(PhytochemicalConstituentRoleDbo.from(name, role));
        }
        for (PlantTissue tissue : c.tissues()) {
            mapper.insertTissue(PhytochemicalConstituentTissueDbo.from(name, tissue));
        }
    }
}
