package com.naturalist.plants;

import com.naturalist.biogeography.Bioregion;
import com.naturalist.data.AbstractEntityRepository;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.exception.EntityNotFoundException;
import com.naturalist.exception.PrimaryKeyConstraintException;
import com.naturalist.fieldnotes.CommonName;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.RdbmsExceptions;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * RDBMS adapter for the {@link PlantSpecies} rank record — the bottom of the plant Linnaean chain.
 * The upward reference to {@link PlantGenus} is stored as {@code genus_id}, resolved from the genus
 * name by nested-select on write and recovered by JOIN on read; a species whose genus is absent
 * inserts zero rows, which becomes {@link EntityNotFoundException}. Reads assemble each parent with
 * its {@code commonNames} and {@code nativeBioregions} loaded in one batched query per child type
 * keyed on the species-name set — a page of species costs three selects, not three-per-species.
 */
@DomainService
class PlantSpeciesEntityRepositoryRdbms
        extends AbstractEntityRepository<PlantSpeciesName, PlantSpecies>
        implements PlantRepository.SpeciesRepository {

    private final PlantSpeciesMapper mapper;

    PlantSpeciesEntityRepositoryRdbms(PlantSpeciesMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected Optional<PlantSpecies> doGetByName(PlantSpeciesName name) {
        PlantSpeciesDbo parent = mapper.selectByName(name.value());
        if (parent == null) return Optional.empty();
        return Optional.of(assemble(List.of(parent), Set.of(name.value())).get(0));
    }

    @Override
    protected List<PlantSpecies> doGetByNameSet(Set<PlantSpeciesName> nameSet) {
        if (nameSet.isEmpty()) return List.of();
        Set<String> slugs = nameSet.stream().map(PlantSpeciesName::value).collect(Collectors.toSet());
        return assemble(mapper.selectByNameSet(slugs), slugs);
    }

    @Override
    protected Page<PlantSpecies> doGetPage(PageRequest request) {
        int pageSize = request.pageSize();
        List<PlantSpeciesDbo> rows = mapper.selectPage(pageSize + 1, request.offset());
        boolean immediateMore = rows.size() > pageSize;
        List<PlantSpeciesDbo> parents = rows.stream().limit(pageSize).toList();
        Set<String> names = parents.stream().map(p -> p.name).collect(Collectors.toSet());
        List<PlantSpecies> content = assemble(parents, names);

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
    protected void doInsert(PlantSpecies entity) {
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

    @Override
    protected void doUpdate(PlantSpecies entity) {
        PlantSpeciesDbo dbo = PlantSpeciesDbo.from(entity);
        if (mapper.updateByName(dbo) == 0) throw new EntityNotFoundException(entity);
        String name = entity.name().value();
        mapper.deleteCommonNames(name);
        mapper.deleteNativeBioregions(name);
        insertChildren(entity);
    }

    @Override
    protected PlantSpecies doSave(PlantSpecies entity) {
        if (mapper.selectByName(entity.name().value()) != null) doUpdate(entity);
        else doInsert(entity);
        return entity;
    }

    @Override
    public List<PlantSpecies> getByGenusName(PlantGenusName genusName) {
        Observer.forClass(PlantSpeciesEntityRepositoryRdbms.class)
                .arguments("getByGenusName", i -> i.entityName(genusName, "genusName"))
                .throwWhenInvalid();
        List<PlantSpeciesDbo> parents = mapper.selectByGenusName(genusName.value());
        Set<String> names = parents.stream().map(p -> p.name).collect(Collectors.toSet());
        return assemble(parents, names);
    }

    @Override
    public List<PlantSpecies> getByGenusNames(Set<PlantGenusName> genusNames) {
        Observer.forClass(PlantSpeciesEntityRepositoryRdbms.class)
                .arguments("getByGenusNames", i -> i.entityNameCollection(genusNames, "genusNames"))
                .throwWhenInvalid();
        if (genusNames.isEmpty()) return List.of();
        Set<String> genusSlugs = genusNames.stream().map(PlantGenusName::value).collect(Collectors.toSet());
        List<PlantSpeciesDbo> parents = mapper.selectByGenusNames(genusSlugs);
        Set<String> names = parents.stream().map(p -> p.name).collect(Collectors.toSet());
        return assemble(parents, names);
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

    /** Assemble records from parent rows + one batched load per child table. */
    private List<PlantSpecies> assemble(List<PlantSpeciesDbo> parents, Set<String> names) {
        if (parents.isEmpty()) return List.of();

        Map<String, Set<CommonName>> commonNames = mapper.selectCommonNames(names).stream()
                .collect(Collectors.groupingBy(cn -> cn.speciesName,
                        Collectors.mapping(PlantSpeciesCommonNameDbo::toCommonName, Collectors.toSet())));
        Map<String, Set<Bioregion>> bioregions = mapper.selectNativeBioregions(names).stream()
                .collect(Collectors.groupingBy(nb -> nb.speciesName,
                        Collectors.mapping(PlantSpeciesNativeBioregionDbo::toBioregion, Collectors.toSet())));

        return parents.stream()
                .map(p -> p.toEntity(
                        bioregions.getOrDefault(p.name, Set.of()),
                        commonNames.getOrDefault(p.name, Set.of())))
                .toList();
    }
}
