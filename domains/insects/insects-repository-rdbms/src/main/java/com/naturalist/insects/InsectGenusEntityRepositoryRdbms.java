package com.naturalist.insects;

import com.naturalist.data.AbstractEntityRepository;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.exception.EntityNotFoundException;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.observability.Level;
import com.naturalist.observability.MethodObserver;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * RDBMS adapter for the {@link InsectGenusInfo} rank record. The upward reference to {@link InsectFamilyInfo}
 * is stored as {@code family_id}, resolved from the family name by the mapper's nested-select on write
 * and recovered by JOIN on read; a genus whose family is absent inserts zero rows, which the adapter
 * turns into {@link EntityNotFoundException}. Reads assemble the parent with its {@code commonNames}
 * loaded in one batched query keyed on the genus-name set — no N+1. The {@link #getByFamilyName} /
 * {@link #getByFamilyNames} queries each cost two selects regardless of the family-set size.
 *
 * <p>Each write spans several tables, so it runs through {@link InsectGenusTransaction} as one
 * transaction.
 */
@DomainService
class InsectGenusEntityRepositoryRdbms
        extends AbstractEntityRepository<InsectGenusName, InsectGenusInfo>
        implements InsectRepository.GenusRepository {

    private final InsectGenusMapper mapper;
    private final InsectGenusTransaction transaction;

    InsectGenusEntityRepositoryRdbms(InsectGenusMapper mapper, InsectGenusTransaction transaction) {
        this.mapper = mapper;
        this.transaction = transaction;
    }

    @Override
    protected Optional<InsectGenusInfo> doGetByName(InsectGenusName name) {
        InsectGenusDbo parent = mapper.selectByName(name.value());
        if (parent == null) return Optional.empty();
        return Optional.of(assemble(List.of(parent), Set.of(name.value())).get(0));
    }

    @Override
    protected List<InsectGenusInfo> doGetByNameSet(Set<InsectGenusName> nameSet) {
        if (nameSet.isEmpty()) return List.of();
        Set<String> slugs = nameSet.stream().map(InsectGenusName::value).collect(Collectors.toSet());
        return assemble(mapper.selectByNameSet(slugs), slugs);
    }

    @Override
    protected Page<InsectGenusInfo> doGetPage(PageRequest request) {
        int pageSize = request.pageSize();
        List<InsectGenusDbo> rows = mapper.selectPage(pageSize + 1, request.offset());
        boolean immediateMore = rows.size() > pageSize;
        List<InsectGenusDbo> parents = rows.stream().limit(pageSize).toList();
        Set<String> names = parents.stream().map(p -> p.name).collect(Collectors.toSet());
        List<InsectGenusInfo> content = assemble(parents, names);

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
    protected void doInsert(InsectGenusInfo entity) {
        transaction.insert(entity);
    }

    @Override
    protected void doUpdate(InsectGenusInfo entity) {
        transaction.update(entity);
    }

    @Override
    protected InsectGenusInfo doSave(InsectGenusInfo entity) {
        transaction.save(entity);
        return entity;
    }

    @Override
    public List<InsectGenusInfo> getByFamilyName(InsectFamilyName familyName) {
        MethodObserver getByFamilyName = observer().forMethod("getByFamilyName");
        getByFamilyName.arguments(i -> i.entityName(familyName, "familyName"))
                .throwWhenInvalid();
        List<InsectGenusDbo> parents = mapper.selectByFamilyName(familyName.value());
        Set<String> names = parents.stream().map(p -> p.name).collect(Collectors.toSet());
        var result = assemble(parents, names);
        getByFamilyName.state(s -> s.observableCollection(result, "result")).observe(Level.WARN);
        return result;
    }

    @Override
    public List<InsectGenusInfo> getByFamilyNames(Set<InsectFamilyName> familyNames) {
        MethodObserver getByFamilyNames = observer().forMethod("getByFamilyNames");
        getByFamilyNames.arguments(i -> i.entityNameCollection(familyNames, "familyNames"))
                .throwWhenInvalid();
        if (familyNames.isEmpty()) return List.of();
        Set<String> familySlugs = familyNames.stream().map(InsectFamilyName::value).collect(Collectors.toSet());
        List<InsectGenusDbo> parents = mapper.selectByFamilyNameSet(familySlugs);
        Set<String> names = parents.stream().map(p -> p.name).collect(Collectors.toSet());
        var result = assemble(parents, names);
        getByFamilyNames.state(s -> s.observableCollection(result, "result")).observe(Level.WARN);
        return result;
    }

    /** Assemble records from parent rows + one batched common-name load. */
    private List<InsectGenusInfo> assemble(List<InsectGenusDbo> parents, Set<String> names) {
        if (parents.isEmpty()) return List.of();
        var commonNames = mapper.selectCommonNames(names).stream()
                .collect(Collectors.groupingBy(cn -> cn.genusName,
                        Collectors.mapping(InsectGenusCommonNameDbo::toCommonName, Collectors.toSet())));
        return parents.stream()
                .map(p -> p.toEntity(commonNames.getOrDefault(p.name, Set.of())))
                .toList();
    }
}
