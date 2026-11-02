package com.naturalist.insects;

import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.observability.Observer;
import com.naturalist.observation.OrganismImage;
import com.naturalist.observation.OrganismObservation;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Assembles {@link InsectViews} presentation models from the biology {@link InsectQuery} serves.
 * Package-private: only {@link InsectViewQueryImpl} (and its tests) construct one.
 */
class InsectViewFactory {
    private final Observer observer = Observer.forClass(getClass());

    private final InsectQuery insectQuery;

    InsectViewFactory(InsectQuery insectQuery) {
        observer.arguments("constructor", i -> i.notNull(insectQuery, "insectQuery")).throwWhenInvalid();
        this.insectQuery = insectQuery;
    }

    Optional<InsectViews.RankPage> buildRankPage(InsectRankName name, @Nullable NaturalistName viewer) {
        observer.arguments("buildRankPage", i -> i.identifier(name, "name").whenNotNull(viewer, v -> v.entityName(viewer, "viewer"))).throwWhenInvalid();
        Optional<Insect> found = insectQuery.getByName(name);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        Insect insect = found.get();
        if (viewer == null) {
            return Optional.of(InsectViews.RankPage.anonymous(insect));
        }
        InsectEntityCollections.ObservationCollection mine =
                insectQuery.observations().forNaturalistAndSubjects(viewer, Set.<InsectRankName>of(name));
        Map<InsectObservationId, OrganismObservation<InsectObservationId, InsectRankName>> mineById = mine.stream()
                .collect(Collectors.toMap(OrganismObservation::id, Function.identity()));
        List<OrganismImage<InsectImageId, InsectObservationId, InsectRankName>> ownImages = new ArrayList<>();
        Map<InsectImageId, OrganismObservation<InsectObservationId, InsectRankName>> ownObservationByImage = new LinkedHashMap<>();
        for (OrganismImage<InsectImageId, InsectObservationId, InsectRankName> image : insect.observations().stream().toList()) {
            OrganismObservation<InsectObservationId, InsectRankName> observation =
                    image.observationId() == null ? null : mineById.get(image.observationId());
            if (observation != null) {
                ownImages.add(image);
                ownObservationByImage.put(image.id(), observation);
            }
        }
        return Optional.of(new InsectViews.RankPage(insect, new InsectViews.Viewer(
                !mineById.isEmpty(), InsectEntityCollections.ImageCollection.of(ownImages), ownObservationByImage)));
    }

    Page<InsectViews.SpeciesCard> speciesCards(PageRequest request, @Nullable NaturalistName viewer) {
        observer.arguments("speciesCards", i -> i.valueObject(request, "request").whenNotNull(viewer, v -> v.entityName(viewer, "viewer"))).throwWhenInvalid();
        Page<InsectSpecies> page = insectQuery.species().findPage(request);
        Set<InsectSpeciesName> names = page.content().stream().map(InsectSpecies::name).collect(Collectors.toSet());
        Set<InsectSpeciesName> collected = viewer == null || names.isEmpty() ? Set.of() : collectedAmong(viewer, names);
        return new Page<>(cards(page.content(), collected), page.pageNumber(), page.pageSize(),
                page.pagesAheadKnown(), page.moreBeyondLookahead());
    }

    Page<InsectViews.SpeciesCard> collectedBy(NaturalistName viewer, PageRequest request) {
        observer.arguments("collectedBy", i -> i.entityName(viewer, "viewer").valueObject(request, "request")).throwWhenInvalid();
        Set<InsectSpeciesName> observed = insectQuery.observations().forNaturalist(viewer).stream()
                .map(OrganismObservation::subject)
                .filter(InsectSpeciesName.class::isInstance)
                .map(InsectSpeciesName.class::cast)
                .collect(Collectors.toSet());
        List<InsectSpecies> sorted = insectQuery.species().findByNameSet(observed).stream()
                .sorted(Comparator.comparing((InsectSpecies s) -> s.name().value()))
                .toList();
        int pageSize = request.pageSize();
        // long math: a huge ?page= must clamp to an empty page, not overflow the int offset.
        int from = (int) Math.min((long) request.pageNumber() * pageSize, sorted.size());
        int to = Math.min(from + pageSize, sorted.size());
        List<InsectSpecies> slice = sorted.subList(from, to);
        int remaining = sorted.size() - to;
        int lookahead = request.lookahead();
        int pagesAheadKnown = lookahead == 0 ? 0 : Math.min(lookahead, (remaining + pageSize - 1) / pageSize);
        boolean moreBeyondLookahead = remaining > lookahead * pageSize;
        Set<InsectSpeciesName> sliceNames = slice.stream().map(InsectSpecies::name).collect(Collectors.toSet());
        return new Page<>(cards(slice, sliceNames), request.pageNumber(), pageSize, pagesAheadKnown, moreBeyondLookahead);
    }

    private Set<InsectSpeciesName> collectedAmong(NaturalistName viewer, Set<InsectSpeciesName> names) {
        Set<InsectRankName> subjects = new java.util.HashSet<>(names);
        return insectQuery.observations().forNaturalistAndSubjects(viewer, subjects).stream()
                .map(OrganismObservation::subject)
                .filter(InsectSpeciesName.class::isInstance)
                .map(InsectSpeciesName.class::cast)
                .collect(Collectors.toSet());
    }

    /** One batched read per card part for the whole list, then in-memory assembly in list order. */
    private List<InsectViews.SpeciesCard> cards(List<InsectSpecies> species, Set<InsectSpeciesName> collected) {
        if (species.isEmpty()) {
            return List.of();
        }
        Set<InsectSpeciesName> speciesNames = species.stream().map(InsectSpecies::name).collect(Collectors.toSet());
        Set<InsectRankName> rankNames = new java.util.HashSet<>(speciesNames);
        Set<InsectGenusName> genusNames = species.stream().map(InsectSpecies::genusName).collect(Collectors.toSet());
        Map<InsectRankName, List<OrganismImage<InsectImageId, InsectObservationId, InsectRankName>>> imagesByName =
                insectQuery.images().forParentNames(rankNames).stream().collect(Collectors.groupingBy(OrganismImage::parentName));
        Map<InsectRankName, InsectFunctionalRole> roleByName = insectQuery.functionalRoles().getByParentNames(rankNames).stream()
                .collect(Collectors.toMap(InsectFunctionalRole::parentName, Function.identity(),
                        (a, b) -> a)); // one role per parent is the invariant; a duplicate cannot occur, keep the first
        Map<InsectGenusName, InsectGenus> genusByName = insectQuery.genera().findByNameSet(genusNames).stream()
                .collect(Collectors.toMap(InsectGenus::name, Function.identity()));
        Set<InsectFamilyName> familyNames = genusByName.values().stream().map(InsectGenus::familyName).collect(Collectors.toSet());
        Map<InsectFamilyName, InsectFamily> familyByName = insectQuery.families().findByNameSet(familyNames).stream()
                .collect(Collectors.toMap(InsectFamily::name, Function.identity()));
        return species.stream().map(s -> {
            InsectGenus genus = genusByName.get(s.genusName());
            InsectFamily family = genus == null ? null : familyByName.get(genus.familyName());
            return new InsectViews.SpeciesCard(s, genus, family, roleByName.get(s.name()),
                    InsectEntityCollections.ImageCollection.of(imagesByName.getOrDefault(s.name(), List.of())),
                    collected.contains(s.name()));
        }).toList();
    }

    Page<InsectViews.RankCard> orderCards(PageRequest request) {
        observer.arguments("orderCards", i -> i.valueObject(request, "request")).throwWhenInvalid();
        Page<InsectOrder> page = insectQuery.orders().findPage(request);
        return rankCards(page, o -> o.name(), InsectOrder::displayName,
                o -> o.order().value(), o -> null, Map.of());
    }

    Page<InsectViews.RankCard> familyCards(PageRequest request) {
        observer.arguments("familyCards", i -> i.valueObject(request, "request")).throwWhenInvalid();
        Page<InsectFamily> page = insectQuery.families().findPage(request);
        Set<InsectOrderName> orderNames = page.content().stream().map(InsectFamily::orderName).collect(Collectors.toSet());
        Map<InsectRankName, String> labels = page.content().isEmpty() ? Map.of()
                : insectQuery.orders().findByNameSet(orderNames).stream()
                .collect(Collectors.toMap(InsectOrder::name, o -> o.order().value()));
        return rankCards(page, f -> f.name(), InsectFamily::displayName,
                f -> f.family().value(), InsectFamily::orderName, labels);
    }

    Page<InsectViews.RankCard> genusCards(PageRequest request) {
        observer.arguments("genusCards", i -> i.valueObject(request, "request")).throwWhenInvalid();
        Page<InsectGenus> page = insectQuery.genera().findPage(request);
        Set<InsectFamilyName> familyNames = page.content().stream().map(InsectGenus::familyName).collect(Collectors.toSet());
        Map<InsectRankName, String> labels = page.content().isEmpty() ? Map.of()
                : insectQuery.families().findByNameSet(familyNames).stream()
                .collect(Collectors.toMap(InsectFamily::name, f -> f.family().value()));
        return rankCards(page, g -> g.name(), InsectGenus::displayName,
                g -> g.genus().value(), InsectGenus::familyName, labels);
    }

    /** One batched subtree-gallery read for the page's roots, then in-memory assembly in page order. */
    private <E> Page<InsectViews.RankCard> rankCards(Page<E> page,
                                                    Function<E, InsectRankName> nameOf, Function<E, String> displayOf,
                                                    Function<E, String> scientificOf, Function<E, @Nullable InsectRankName> parentOf,
                                                    Map<InsectRankName, String> parentLabels) {
        if (page.content().isEmpty()) {
            return new Page<>(List.of(), page.pageNumber(), page.pageSize(), page.pagesAheadKnown(), page.moreBeyondLookahead());
        }
        InsectEntityCollections.ImageGallery gallery = insectQuery.images().forRankHierarchies(page.content().stream().map(nameOf).collect(Collectors.toCollection(LinkedHashSet::new)));
        List<InsectViews.RankCard> cards = page.content().stream().map(e -> {
            InsectRankName parent = parentOf.apply(e);
            String label = parent == null ? null : parentLabels.getOrDefault(parent, parent.value());
            return new InsectViews.RankCard(nameOf.apply(e), displayOf.apply(e), scientificOf.apply(e), parent, label,
                    gallery.forEntity(nameOf.apply(e)));
        }).toList();
        return new Page<>(cards, page.pageNumber(), page.pageSize(), page.pagesAheadKnown(), page.moreBeyondLookahead());
    }
}
