package com.naturalist.insects.lifestage;

import com.naturalist.data.AbstractEntityRepository;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.exception.EntityNotFoundException;
import com.naturalist.exception.PrimaryKeyConstraintException;
import com.naturalist.habitat.HabitatZone;
import com.naturalist.habitat.VerticalLayer;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.insects.InsectRankName;
import com.naturalist.insects.InsectSpeciesName;
import com.naturalist.insects.LifeStageName;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.RdbmsExceptions;
import com.naturalist.plants.PlantSpeciesName;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * RDBMS adapter for the sealed {@link LifeStage} single-table hierarchy. Reads assemble each parent row with
 * its six child tables loaded in one batched query each (keyed on the life-stage-name set) — a page costs
 * seven selects, not seven-per-stage. Writes persist the parent then the applicable child rows; an update
 * replaces the child rows wholesale. The polymorphic parent reference is stored flat and rebuilt in-module.
 */
@DomainService
class InsectLifeStageEntityRepositoryRdbms
        extends AbstractEntityRepository<LifeStageName, LifeStage>
        implements LifeStageRepository.LifeStageEntityRepository {

    private final InsectLifeStageMapper mapper;

    InsectLifeStageEntityRepositoryRdbms(InsectLifeStageMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected Optional<LifeStage> doGetByName(LifeStageName name) {
        InsectLifeStageDbo dbo = mapper.selectByName(name.value());
        if (dbo == null) return Optional.empty();
        return Optional.of(assemble(List.of(dbo)).get(0));
    }

    @Override
    protected List<LifeStage> doGetByNameSet(Set<LifeStageName> nameSet) {
        if (nameSet.isEmpty()) return List.of();
        Set<String> slugs = nameSet.stream().map(LifeStageName::value).collect(Collectors.toSet());
        return assemble(mapper.selectByNameSet(slugs));
    }

    @Override
    protected Page<LifeStage> doGetPage(PageRequest request) {
        int pageSize = request.pageSize();
        List<InsectLifeStageDbo> rows = mapper.selectPage(pageSize + 1, request.offset());
        boolean immediateMore = rows.size() > pageSize;
        List<LifeStage> content = assemble(rows.stream().limit(pageSize).toList());

        int pagesAheadKnown = 0;
        boolean moreBeyondLookahead = immediateMore;
        if (request.lookahead() > 0 && immediateMore) {
            int window = request.lookahead() * pageSize + 1;
            int beyond = mapper.countInWindow(request.offset() + pageSize, window);
            pagesAheadKnown = Math.min(request.lookahead(), (beyond + pageSize - 1) / pageSize);
            moreBeyondLookahead = beyond > request.lookahead() * pageSize;
        } else if (request.lookahead() > 0) {
            moreBeyondLookahead = false;
        }
        return new Page<>(content, request.pageNumber(), pageSize, pagesAheadKnown, moreBeyondLookahead);
    }

    @Override
    protected void doInsert(LifeStage entity) {
        InsectLifeStageDbo dbo = InsectLifeStageDbo.from(entity);
        try {
            mapper.insert(dbo);
        } catch (RuntimeException e) {
            if (RdbmsExceptions.isUniqueViolation(e)) throw new PrimaryKeyConstraintException(entity);
            throw e;
        }
        insertChildren(entity);
    }

    @Override
    protected void doUpdate(LifeStage entity) {
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

    @Override
    protected LifeStage doSave(LifeStage entity) {
        if (mapper.selectByName(entity.name().value()) != null) doUpdate(entity);
        else doInsert(entity);
        return entity;
    }

    @Override
    public List<LifeStage> getByParentName(InsectRankName parentName) {
        Observer.forClass(InsectLifeStageEntityRepositoryRdbms.class)
                .arguments("getByParentName", i -> i.identifier(parentName, "parentName"))
                .throwWhenInvalid();
        return assemble(mapper.selectByParentName(parentName.rank().name(), parentName.value()));
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

    private List<LifeStage> assemble(List<InsectLifeStageDbo> parents) {
        if (parents.isEmpty()) return List.of();
        Set<String> names = parents.stream().map(p -> p.name).collect(Collectors.toSet());

        Map<String, List<StagePhenology.ActivityWindow>> windows = mapper.selectWindows(names).stream()
                .collect(Collectors.groupingBy(w -> w.lifeStageName,
                        Collectors.mapping(InsectLifeStageWindowDbo::toWindow, Collectors.toList())));
        Map<String, Set<HabitatZone>> zones = mapper.selectHabitatZones(names).stream()
                .collect(Collectors.groupingBy(z -> z.lifeStageName,
                        Collectors.mapping(InsectLifeStageHabitatZoneDbo::toZone, Collectors.toSet())));
        Map<String, Set<VerticalLayer>> layers = mapper.selectHabitatLayers(names).stream()
                .collect(Collectors.groupingBy(l -> l.lifeStageName,
                        Collectors.mapping(InsectLifeStageHabitatLayerDbo::toLayer, Collectors.toSet())));
        Map<String, List<PlantSpeciesName>> hostPlants = mapper.selectHostPlants(names).stream()
                .collect(Collectors.groupingBy(h -> h.lifeStageName,
                        Collectors.mapping(InsectLifeStageHostPlantDbo::toName, Collectors.toList())));
        Map<String, List<InsectSpeciesName>> parasitoidHosts = mapper.selectParasitoidHosts(names).stream()
                .collect(Collectors.groupingBy(p -> p.lifeStageName,
                        Collectors.mapping(InsectLifeStageParasitoidHostDbo::toName, Collectors.toList())));
        Map<String, List<PlantSpeciesName>> nectarSources = mapper.selectNectarSources(names).stream()
                .collect(Collectors.groupingBy(n -> n.lifeStageName,
                        Collectors.mapping(InsectLifeStageNectarSourceDbo::toName, Collectors.toList())));

        return parents.stream()
                .map(p -> p.toEntity(
                        windows.getOrDefault(p.name, List.of()),
                        zones.getOrDefault(p.name, Set.of()),
                        layers.getOrDefault(p.name, Set.of()),
                        hostPlants.getOrDefault(p.name, List.of()),
                        parasitoidHosts.getOrDefault(p.name, List.of()),
                        nectarSources.getOrDefault(p.name, List.of())))
                .toList();
    }
}
