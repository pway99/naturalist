package com.naturalist.insects;

import com.naturalist.authority.Citation;
import com.naturalist.authority.CitationName;
import com.naturalist.catalog.EntityRef;
import com.naturalist.ddd.EntityName;
import com.naturalist.library.CitationAssociation;
import com.naturalist.library.CitationAssociationQuery;
import com.naturalist.library.CitationQuery;
import com.naturalist.observability.Level;
import com.naturalist.observability.Observer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

class InsectCitationQueryImpl implements InsectQuery.CitationQuery {

    private static final InsectsDomain INSECTS = new InsectsDomain();
    private final Observer observer = Observer.forClass(getClass());
    private final CitationAssociationQuery citationAssociationQuery;
    private final CitationQuery citationQuery;
    private final InsectAncestryResolver ancestryResolver;

    InsectCitationQueryImpl(CitationAssociationQuery citationAssociationQuery,
                            CitationQuery citationQuery,
                            InsectAncestryResolver ancestryResolver) {
        observer.arguments("constructor", i -> i
                        .notNull(citationAssociationQuery, "citationAssociationQuery")
                        .notNull(citationQuery, "citationQuery")
                        .notNull(ancestryResolver, "ancestryResolver"))
                .throwWhenInvalid();
        this.citationAssociationQuery = citationAssociationQuery;
        this.citationQuery = citationQuery;
        this.ancestryResolver = ancestryResolver;
    }

    record PendingCitation(CitationName citationName, InsectRankName attachedAt, String note) {}

    @Override
    public InsectCitationView findByRankName(InsectRankName rankName) {
        observer.arguments("findByRankName", i -> i.identifier(rankName, "rankName"))
                .throwWhenInvalid();
        return findByAncestry(rankName, ancestryResolver.ancestry(rankName));
    }

    /**
     * The citation view for {@code subject}, resolved over a <em>pre-computed</em> ancestor-first
     * ancestry (order → … → subject) rather than re-walking the rank chain. The composed
     * {@code InsectFactory} resolves the lineage once and hands it here so the whole read model
     * costs a single ancestry walk; {@link #findByRankName} supplies the walk for direct callers.
     */
    InsectCitationView findByAncestry(InsectRankName subject, Set<InsectRankName> ancestry) {
        observer.arguments("findByAncestry", i -> i
                        .identifier(subject, "subject")
                        .observableCollection(ancestry, "ancestry"))
                .throwWhenInvalid();

        // Resolve the whole ancestry's citation associations in ONE batched query rather than
        // one findBySubject per ancestor rank — the per-rank loop was exactly the fan-out the
        // N+1 select gate exists to catch.
        Set<EntityRef> subjects = ancestry.stream()
                .map(rank -> new EntityRef(INSECTS, (EntityName) rank))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<InsectRankName, List<CitationAssociation>> associationsByRank =
                citationAssociationQuery.findBySubjects(subjects).stream()
                        .collect(Collectors.groupingBy(a -> (InsectRankName) a.subject().name()));

        // Preserve subject-first ordering (the rank's own citations first, then ancestors
        // ascending): walk the ancestry subject-first over the already-fetched batch.
        List<InsectRankName> subjectFirst = new ArrayList<>(ancestry);
        Collections.reverse(subjectFirst);
        List<PendingCitation> pending = new ArrayList<>();
        for (InsectRankName rank : subjectFirst) {
            for (CitationAssociation association : associationsByRank.getOrDefault(rank, List.of())) {
                pending.add(new PendingCitation(association.citationName(), rank, association.note()));
            }
        }

        if (pending.isEmpty()) {
            InsectCitationView view = new InsectCitationView(subject, List.of());
            observer.observable(view, "citationView").observe(Level.WARN);
            return view;
        }

        Set<CitationName> names = pending.stream().map(PendingCitation::citationName).collect(Collectors.toSet());
        Map<CitationName, Citation> resolved = new LinkedHashMap<>();
        for (Citation c : citationQuery.findByNameSet(names).stream().toList()) {
            resolved.put(c.name(), c);
        }

        List<InsectCitationView.RankedCitation> citations = new ArrayList<>();
        for (PendingCitation p : pending) {
            Citation citation = resolved.get(p.citationName());
            if (citation != null) {
                citations.add(new InsectCitationView.RankedCitation(citation, p.attachedAt(), p.note()));
            } else {
                observer.forMethod("findByAncestry").entityName(p.citationName(), "unresolvedCitationName").observe(Level.WARN);
            }
        }

        InsectCitationView view = new InsectCitationView(subject, List.copyOf(citations));
        observer.observable(view, "citationView").observe(Level.WARN);
        return view;
    }

}
