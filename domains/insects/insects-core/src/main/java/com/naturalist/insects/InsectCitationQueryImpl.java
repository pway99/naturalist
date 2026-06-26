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
import java.util.LinkedHashMap;
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

        List<InsectRankName> ancestry = ancestryResolver.resolveAncestry(rankName);
        List<PendingCitation> pending = new ArrayList<>();

        for (InsectRankName rank : ancestry) {
            EntityRef ref = new EntityRef(INSECTS, (EntityName) rank);
            for (CitationAssociation a : citationAssociationQuery.findBySubject(ref).stream().toList()) {
                pending.add(new PendingCitation(a.citationName(), rank, a.note()));
            }
        }

        if (pending.isEmpty()) {
            InsectCitationView view = new InsectCitationView(rankName, List.of());
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
                observer.forMethod("findByRankName").entityName(p.citationName(), "unresolvedCitationName").observe(Level.WARN);
            }
        }

        InsectCitationView view = new InsectCitationView(rankName, List.copyOf(citations));
        observer.observable(view, "citationView").observe(Level.WARN);
        return view;
    }

}
