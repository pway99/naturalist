package com.naturalist.insects;

import com.naturalist.data.AbstractEntityRepository;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.exception.EntityNotFoundException;
import com.naturalist.exception.PrimaryKeyConstraintException;
import com.naturalist.fieldnotes.CommonName;
import com.naturalist.habitat.HabitatZone;
import com.naturalist.habitat.VerticalLayer;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.observability.Level;
import com.naturalist.observability.MethodObserver;
import com.naturalist.persistence.RdbmsExceptions;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * RDBMS adapter for the {@link InsectSpeciesInfo} aggregate — the bottom of the insect Linnaean chain.
 * The upward reference to {@link InsectGenusInfo} is stored as {@code genus_id}, resolved from the genus
 * name by nested-select on write and recovered by JOIN on read; a species whose genus is absent
 * inserts zero rows, which becomes {@link EntityNotFoundException}. Reads assemble each parent with
 * its five child collections ({@code commonNames}, {@code protectedStages}, habitat {@code zones} and
 * {@code layers}, and the ordered {@code supportingPlants}) loaded in one batched query per child
 * type keyed on the species-name set — a page of species costs six selects, not six-per-species.
 * Writes persist the parent then the children (each child insert nested-selects the parent id from
 * the name); an update replaces the child rows wholesale. The seven owned value objects are
 * reconstructed only when their null-group presence condition holds, so an all-null species (every
 * species but {@code battus-philenor} in the seed) round-trips with all VOs absent.
 */
@DomainService
class InsectSpeciesEntityRepositoryRdbms
        extends AbstractEntityRepository<InsectSpeciesName, InsectSpeciesInfo>
        implements InsectRepository.SpeciesRepository {

    private final InsectSpeciesMapper mapper;

    InsectSpeciesEntityRepositoryRdbms(InsectSpeciesMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected Optional<InsectSpeciesInfo> doGetByName(InsectSpeciesName name) {
        InsectSpeciesDbo parent = mapper.selectByName(name.value());
        if (parent == null) return Optional.empty();
        return Optional.of(assemble(List.of(parent), Set.of(name.value())).get(0));
    }

    @Override
    protected List<InsectSpeciesInfo> doGetByNameSet(Set<InsectSpeciesName> nameSet) {
        if (nameSet.isEmpty()) return List.of();
        Set<String> slugs = nameSet.stream().map(InsectSpeciesName::value).collect(Collectors.toSet());
        return assemble(mapper.selectByNameSet(slugs), slugs);
    }

    @Override
    protected Page<InsectSpeciesInfo> doGetPage(PageRequest request) {
        int pageSize = request.pageSize();
        List<InsectSpeciesDbo> rows = mapper.selectPage(pageSize + 1, request.offset());
        boolean immediateMore = rows.size() > pageSize;
        List<InsectSpeciesDbo> parents = rows.stream().limit(pageSize).toList();
        Set<String> names = parents.stream().map(p -> p.name).collect(Collectors.toSet());
        List<InsectSpeciesInfo> content = assemble(parents, names);

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
    protected void doInsert(InsectSpeciesInfo entity) {
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

    @Override
    protected void doUpdate(InsectSpeciesInfo entity) {
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

    @Override
    protected InsectSpeciesInfo doSave(InsectSpeciesInfo entity) {
        if (mapper.selectByName(entity.name().value()) != null) doUpdate(entity);
        else doInsert(entity);
        return entity;
    }

    @Override
    public List<InsectSpeciesInfo> getByGenusName(InsectGenusName genusName) {
        MethodObserver getByGenusName = observer().forMethod("getByGenusName");
        getByGenusName.arguments(i -> i.entityName(genusName, "genusName"))
                .throwWhenInvalid();
        List<InsectSpeciesDbo> parents = mapper.selectByGenusName(genusName.value());
        Set<String> names = parents.stream().map(p -> p.name).collect(Collectors.toSet());
        var result = assemble(parents, names);
        getByGenusName.state(s -> s.observableCollection(result, "result")).observe(Level.WARN);
        return result;
    }

    @Override
    public List<InsectSpeciesInfo> getByGenusNames(Set<InsectGenusName> genusNames) {
        MethodObserver getByGenusNames = observer().forMethod("getByGenusNames");
        getByGenusNames.arguments(i -> i.entityNameCollection(genusNames, "genusNames"))
                .throwWhenInvalid();
        if (genusNames.isEmpty()) return List.of();
        Set<String> genusSlugs = genusNames.stream().map(InsectGenusName::value).collect(Collectors.toSet());
        List<InsectSpeciesDbo> parents = mapper.selectByGenusNames(genusSlugs);
        Set<String> names = parents.stream().map(p -> p.name).collect(Collectors.toSet());
        var result = assemble(parents, names);
        getByGenusNames.state(s -> s.observableCollection(result, "result")).observe(Level.WARN);
        return result;
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

    /** Assemble aggregates from parent rows + one batched load per child table. */
    private List<InsectSpeciesInfo> assemble(List<InsectSpeciesDbo> parents, Set<String> names) {
        if (parents.isEmpty()) return List.of();

        Map<String, Set<CommonName>> commonNames = mapper.selectCommonNames(names).stream()
                .collect(Collectors.groupingBy(cn -> cn.speciesName,
                        Collectors.mapping(InsectSpeciesCommonNameDbo::toCommonName, Collectors.toSet())));
        Map<String, Set<LifeStageKind>> protectedStages = mapper.selectProtectedStages(names).stream()
                .collect(Collectors.groupingBy(ps -> ps.speciesName,
                        Collectors.mapping(InsectSpeciesProtectedStageDbo::toStageKind, Collectors.toSet())));
        Map<String, Set<HabitatZone>> zones = mapper.selectHabitatZones(names).stream()
                .collect(Collectors.groupingBy(hz -> hz.speciesName,
                        Collectors.mapping(InsectSpeciesHabitatZoneDbo::toZone, Collectors.toSet())));
        Map<String, Set<VerticalLayer>> layers = mapper.selectHabitatLayers(names).stream()
                .collect(Collectors.groupingBy(hl -> hl.speciesName,
                        Collectors.mapping(InsectSpeciesHabitatLayerDbo::toLayer, Collectors.toSet())));
        Map<String, List<String>> supportingPlants = mapper.selectSupportingPlants(names).stream()
                .collect(Collectors.groupingBy(sp -> sp.speciesName,
                        Collectors.mapping(InsectSpeciesSupportingPlantDbo::toPlant, Collectors.toList())));

        return parents.stream()
                .map(p -> p.toEntity(
                        commonNames.getOrDefault(p.name, Set.of()),
                        protectedStages.getOrDefault(p.name, Set.of()),
                        zones.getOrDefault(p.name, Set.of()),
                        layers.getOrDefault(p.name, Set.of()),
                        supportingPlants.getOrDefault(p.name, List.of())))
                .toList();
    }
}
