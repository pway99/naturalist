package com.naturalist.insects.lifestage;

import com.naturalist.data.AbstractEntityRepository;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.habitat.HabitatZone;
import com.naturalist.habitat.VerticalLayer;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.insects.InsectRankName;
import com.naturalist.insects.InsectSpeciesName;
import com.naturalist.insects.LifeStageName;
import com.naturalist.observability.Level;
import com.naturalist.observability.MethodObserver;
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
 *
 * <p>Each write spans several tables, so it runs through {@link InsectLifeStageTransaction} as one
 * transaction.
 */
@DomainService
class InsectLifeStageEntityRepositoryRdbms
        extends AbstractEntityRepository<LifeStageName, LifeStage>
        implements LifeStageRepository.LifeStageEntityRepository {

    private final InsectLifeStageMapper mapper;
    private final InsectLifeStageTransaction transaction;

    InsectLifeStageEntityRepositoryRdbms(InsectLifeStageMapper mapper,
            InsectLifeStageTransaction transaction) {
        this.mapper = mapper;
        this.transaction = transaction;
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
        transaction.insert(entity);
    }

    @Override
    protected void doUpdate(LifeStage entity) {
        transaction.update(entity);
    }

    @Override
    protected LifeStage doSave(LifeStage entity) {
        transaction.save(entity);
        return entity;
    }

    @Override
    public List<LifeStage> getByParentName(InsectRankName parentName) {
        MethodObserver getByParentName = observer().forMethod("getByParentName");
        getByParentName.arguments(i -> i.identifier(parentName, "parentName"))
                .throwWhenInvalid();
        var result = assemble(mapper.selectByParentName(parentName.rank().name(), parentName.value()));
        getByParentName.state(s -> s.observableCollection(result, "result")).observe(Level.WARN);
        return result;
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
