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
    private final InsectQuery.SpeciesQuery speciesQuery;
    private final InsectQuery.GenusQuery genusQuery;
    private final InsectQuery.FamilyQuery familyQuery;

    InsectCitationQueryImpl(CitationAssociationQuery citationAssociationQuery,
                            CitationQuery citationQuery,
                            InsectQuery.SpeciesQuery speciesQuery,
                            InsectQuery.GenusQuery genusQuery,
                            InsectQuery.FamilyQuery familyQuery) {
        observer.arguments("constructor", i -> i
                        .notNull(citationAssociationQuery, "citationAssociationQuery")
                        .notNull(citationQuery, "citationQuery")
                        .notNull(speciesQuery, "speciesQuery")
                        .notNull(genusQuery, "genusQuery")
                        .notNull(familyQuery, "familyQuery"))
                .throwWhenInvalid();
        this.citationAssociationQuery = citationAssociationQuery;
        this.citationQuery = citationQuery;
        this.speciesQuery = speciesQuery;
        this.genusQuery = genusQuery;
        this.familyQuery = familyQuery;
    }

    record PendingCitation(CitationName citationName, InsectRankName attachedAt, String note) {}

    @Override
    public InsectCitationView findByRankName(InsectRankName rankName) {
        observer.arguments("findByRankName", i -> i.identifier(rankName, "rankName"))
                .throwWhenInvalid();

        List<InsectRankName> ancestry = resolveAncestry(rankName);
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

    private List<InsectRankName> resolveAncestry(InsectRankName rankName) {
        List<InsectRankName> ancestry = new ArrayList<>();
        ancestry.add(rankName);

        return switch (rankName) {
            case InsectSpeciesName speciesName -> {
                speciesQuery.getByName(speciesName).ifPresent(species -> {
                    ancestry.add(species.genusName());
                    genusQuery.getByName(species.genusName()).ifPresent(genus -> {
                        ancestry.add(genus.familyName());
                        familyQuery.getByName(genus.familyName()).ifPresent(family ->
                                ancestry.add(family.orderName()));
                    });
                });
                yield ancestry;
            }
            case InsectGenusName genusName -> {
                genusQuery.getByName(genusName).ifPresent(genus -> {
                    ancestry.add(genus.familyName());
                    familyQuery.getByName(genus.familyName()).ifPresent(family ->
                            ancestry.add(family.orderName()));
                });
                yield ancestry;
            }
            case InsectFamilyName familyName -> {
                familyQuery.getByName(familyName).ifPresent(family ->
                        ancestry.add(family.orderName()));
                yield ancestry;
            }
            case InsectOrderName _ -> ancestry;
            case InsectSubspeciesName _ -> ancestry;
        };
    }
}
