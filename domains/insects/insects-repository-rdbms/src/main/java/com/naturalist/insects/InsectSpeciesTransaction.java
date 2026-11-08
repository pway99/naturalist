package com.naturalist.insects;

import com.naturalist.exception.EntityNotFoundException;
import com.naturalist.exception.PrimaryKeyConstraintException;
import com.naturalist.fieldnotes.CommonName;
import com.naturalist.habitat.HabitatZone;
import com.naturalist.habitat.VerticalLayer;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.persistence.RdbmsExceptions;
import jakarta.transaction.Transactional;

import java.util.List;

/**
 * The multi-statement writes of {@link InsectSpeciesEntityRepositoryRdbms}, each run as one
 * transaction. A species write touches six tables — the {@code insect_species} row and its five
 * child tables — so without a transaction a failure part-way commits the statements before it:
 * an insert leaves a parent with only some of its children, and an update that fails while
 * re-inserting children has already committed their deletion.
 *
 * <p>A separate bean, not annotations on the adapter: the adapter's write entry points are the
 * {@code final} {@code insert} / {@code update} / {@code save} of
 * {@link com.naturalist.data.AbstractEntityRepository}, which CGLIB cannot override, and their
 * calls to {@code doInsert} et al. are self-invocations no proxy sees. The adapter's call into
 * this bean crosses a bean boundary, so Spring's transaction proxy applies. Nothing here is
 * {@code final}, for the same reason. Inside a caller's transaction (a {@code Transaction}
 * aggregate write) these join it. Constructed by hand — the ITs and the seeder — no proxy exists
 * and the caller's own {@code SqlSession} is the transaction.
 */
@DomainService
class InsectSpeciesTransaction {

    private final InsectSpeciesMapper mapper;

    InsectSpeciesTransaction(InsectSpeciesMapper mapper) {
        this.mapper = mapper;
    }

    /** The parent row, then every child row. */
    @Transactional
    void insert(InsectSpeciesInfo entity) {
        InsectSpeciesDbo dbo = InsectSpeciesDbo.from(entity);   // validates the parent scalars + throws
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

    /** The parent row, then the child rows replaced wholesale. */
    @Transactional
    void update(InsectSpeciesInfo entity) {
        InsectSpeciesDbo dbo = InsectSpeciesDbo.from(entity);
        if (mapper.updateByName(dbo) == 0) throw new EntityNotFoundException(entity);
        String name = entity.name().value();
        mapper.deleteCommonNames(name);
        mapper.deleteProtectedStages(name);
        mapper.deleteHabitatZones(name);
        mapper.deleteHabitatLayers(name);
        mapper.deleteSupportingPlants(name);
        insertChildren(entity);
    }

    /** The existence check and the write it chooses, in one transaction. */
    @Transactional
    void save(InsectSpeciesInfo entity) {
        if (mapper.selectByName(entity.name().value()) != null) update(entity);
        else insert(entity);
    }

    private void insertChildren(InsectSpeciesInfo s) {
        InsectSpeciesName name = s.name();
        for (CommonName commonName : s.commonNames()) {
            mapper.insertCommonName(InsectSpeciesCommonNameDbo.from(name, commonName));
        }
        if (s.chemicalDefense() != null) {
            for (LifeStageKind stage : s.chemicalDefense().protectedStages()) {
                mapper.insertProtectedStage(InsectSpeciesProtectedStageDbo.from(name, stage));
            }
        }
        if (s.habitatProfile() != null) {
            for (HabitatZone zone : s.habitatProfile().zones()) {
                mapper.insertHabitatZone(InsectSpeciesHabitatZoneDbo.from(name, zone));
            }
            if (s.habitatProfile().layers() != null) {
                for (VerticalLayer layer : s.habitatProfile().layers()) {
                    mapper.insertHabitatLayer(InsectSpeciesHabitatLayerDbo.from(name, layer));
                }
            }
        }
        if (s.gardenConnections() != null) {
            List<String> supportingPlants = s.gardenConnections().supportingPlants();
            for (int ordinal = 0; ordinal < supportingPlants.size(); ordinal++) {
                mapper.insertSupportingPlant(
                        InsectSpeciesSupportingPlantDbo.from(name, ordinal, supportingPlants.get(ordinal)));
            }
        }
    }
}
