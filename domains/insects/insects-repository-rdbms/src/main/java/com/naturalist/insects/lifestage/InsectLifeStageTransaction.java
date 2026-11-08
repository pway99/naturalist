package com.naturalist.insects.lifestage;

import com.naturalist.exception.EntityNotFoundException;
import com.naturalist.exception.PrimaryKeyConstraintException;
import com.naturalist.habitat.HabitatZone;
import com.naturalist.habitat.VerticalLayer;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.insects.InsectSpeciesName;
import com.naturalist.persistence.RdbmsExceptions;
import com.naturalist.plants.PlantSpeciesName;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Set;

/**
 * The multi-statement writes of {@link InsectLifeStageEntityRepositoryRdbms}, each run as one
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
class InsectLifeStageTransaction {

    private final InsectLifeStageMapper mapper;

    InsectLifeStageTransaction(InsectLifeStageMapper mapper) {
        this.mapper = mapper;
    }

    /** The parent row, then every child row. */
    @Transactional
    void insert(LifeStage entity) {
        InsectLifeStageDbo dbo = InsectLifeStageDbo.from(entity);
        try {
            mapper.insert(dbo);
        } catch (RuntimeException e) {
            if (RdbmsExceptions.isUniqueViolation(e)) throw new PrimaryKeyConstraintException(entity);
            throw e;
        }
        insertChildren(entity);
    }

    /** The parent row, then its child rows replaced wholesale. */
    @Transactional
    void update(LifeStage entity) {
        InsectLifeStageDbo dbo = InsectLifeStageDbo.from(entity);
        if (mapper.updateByName(dbo) == 0) throw new EntityNotFoundException(entity);
        String name = entity.name().value();
        mapper.deleteWindows(name);
        mapper.deleteHabitatZones(name);
        mapper.deleteHabitatLayers(name);
        mapper.deleteHostPlants(name);
        mapper.deleteParasitoidHosts(name);
        mapper.deleteNectarSources(name);
        insertChildren(entity);
    }

    /** The existence check and the write it chooses, in one transaction. */
    @Transactional
    void save(LifeStage entity) {
        if (mapper.selectByName(entity.name().value()) != null) update(entity);
        else insert(entity);
    }

    private void insertChildren(LifeStage s) {
        String name = s.name().value();
        List<StagePhenology.ActivityWindow> windows = s.phenology().windows();
        for (int i = 0; i < windows.size(); i++) {
            mapper.insertWindow(InsectLifeStageWindowDbo.from(name, i, windows.get(i)));
        }
        for (HabitatZone zone : s.habitat().profile().zones()) {
            mapper.insertHabitatZone(InsectLifeStageHabitatZoneDbo.from(name, zone));
        }
        Set<VerticalLayer> layers = s.habitat().profile().layers();
        if (layers != null) {
            for (VerticalLayer layer : layers) {
                mapper.insertHabitatLayer(InsectLifeStageHabitatLayerDbo.from(name, layer));
            }
        }
        if (s instanceof LarvaStage l) {
            List<PlantSpeciesName> hosts = l.hostPlants();
            for (int i = 0; i < hosts.size(); i++) {
                mapper.insertHostPlant(InsectLifeStageHostPlantDbo.from(name, i, hosts.get(i)));
            }
            List<InsectSpeciesName> parasitoids = l.parasitoidHosts();
            for (int i = 0; i < parasitoids.size(); i++) {
                mapper.insertParasitoidHost(InsectLifeStageParasitoidHostDbo.from(name, i, parasitoids.get(i)));
            }
        }
        if (s instanceof AdultStage a) {
            List<PlantSpeciesName> nectar = a.nectarSources();
            for (int i = 0; i < nectar.size(); i++) {
                mapper.insertNectarSource(InsectLifeStageNectarSourceDbo.from(name, i, nectar.get(i)));
            }
        }
    }
}
