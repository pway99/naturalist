package com.naturalist.plants;

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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * RDBMS adapter for the {@link PlantGenus} rank record. The upward reference to {@link PlantFamily}
 * is stored as {@code family_id}, resolved from the family name by the mapper's nested-select on
 * write and recovered by JOIN on read; a genus whose family is absent inserts zero rows, which the
 * adapter turns into {@link EntityNotFoundException}. Reads assemble the parent with its
 * {@code commonNames} loaded in one batched query keyed on the genus-name set — no N+1.
 */
@DomainService
class PlantGenusEntityRepositoryRdbms
        extends AbstractEntityRepository<PlantGenusName, PlantGenus>
        implements PlantRepository.GenusRepository {

    private final PlantGenusMapper mapper;

    PlantGenusEntityRepositoryRdbms(PlantGenusMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected Optional<PlantGenus> doGetByName(PlantGenusName name) {
        PlantGenusDbo parent = mapper.selectByName(name.value());
        if (parent == null) return Optional.empty();
        return Optional.of(assemble(List.of(parent), Set.of(name.value())).get(0));
    }

    @Override
    protected List<PlantGenus> doGetByNameSet(Set<PlantGenusName> nameSet) {
        if (nameSet.isEmpty()) return List.of();
        Set<String> slugs = nameSet.stream().map(PlantGenusName::value).collect(Collectors.toSet());
        return assemble(mapper.selectByNameSet(slugs), slugs);
    }

    @Override
    protected Page<PlantGenus> doGetPage(PageRequest request) {
        int pageSize = request.pageSize();
        List<PlantGenusDbo> rows = mapper.selectPage(pageSize + 1, request.offset());
        boolean immediateMore = rows.size() > pageSize;
        List<PlantGenusDbo> parents = rows.stream().limit(pageSize).toList();
        Set<String> names = parents.stream().map(p -> p.name).collect(Collectors.toSet());
        List<PlantGenus> content = assemble(parents, names);

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
    protected void doInsert(PlantGenus entity) {
        PlantGenusDbo dbo = PlantGenusDbo.from(entity);   // validates the parent scalars + throws
        int inserted;
        try {
            inserted = mapper.insert(dbo);
        } catch (RuntimeException e) {
            if (RdbmsExceptions.isUniqueViolation(e)) throw new PrimaryKeyConstraintException(entity);
            throw e;
        }
        if (inserted == 0) throw new EntityNotFoundException(entity);   // referenced family absent
        insertChildren(entity);
    }

    @Override
    protected void doUpdate(PlantGenus entity) {
        PlantGenusDbo dbo = PlantGenusDbo.from(entity);
        if (mapper.updateByName(dbo) == 0) throw new EntityNotFoundException(entity);
        mapper.deleteCommonNames(entity.name().value());
        insertChildren(entity);
    }

    @Override
    protected PlantGenus doSave(PlantGenus entity) {
        if (mapper.selectByName(entity.name().value()) != null) doUpdate(entity);
        else doInsert(entity);
        return entity;
    }

    @Override
    public List<PlantGenus> getByFamilyName(PlantFamilyName familyName) {
        Observer.forClass(PlantGenusEntityRepositoryRdbms.class)
                .arguments("getByFamilyName", i -> i.entityName(familyName, "familyName"))
                .throwWhenInvalid();
        List<PlantGenusDbo> parents = mapper.selectByFamilyName(familyName.value());
        Set<String> names = parents.stream().map(p -> p.name).collect(Collectors.toSet());
        return assemble(parents, names);
    }

    private void insertChildren(PlantGenus g) {
        PlantGenusName name = g.name();
        for (CommonName commonName : g.commonNames()) {
            mapper.insertCommonName(PlantGenusCommonNameDbo.from(name, commonName));
        }
    }

    /** Assemble records from parent rows + one batched common-name load. */
    private List<PlantGenus> assemble(List<PlantGenusDbo> parents, Set<String> names) {
        if (parents.isEmpty()) return List.of();
        var commonNames = mapper.selectCommonNames(names).stream()
                .collect(Collectors.groupingBy(cn -> cn.genusName,
                        Collectors.mapping(PlantGenusCommonNameDbo::toCommonName, Collectors.toSet())));
        return parents.stream()
                .map(p -> p.toEntity(commonNames.getOrDefault(p.name, Set.of())))
                .toList();
    }
}
