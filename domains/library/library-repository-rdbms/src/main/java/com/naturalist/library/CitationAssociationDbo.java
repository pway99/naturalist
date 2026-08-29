package com.naturalist.library;

import com.naturalist.authority.CitationName;
import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;
import com.naturalist.persistence.Fk;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Persistence view of {@link CitationAssociation} — a surrogate-UUID {@code Entity} that ties a
 * citation to the entity it is about. Its {@code citation} reference is the DB-enforced numeric
 * {@code citation_id} (within-library FK); the DBO carries the citation's {@code name}, which the
 * mapper JOINs to project on read and nested-selects {@code citation.id} from on write — so no name
 * is persisted on this row and nothing can drift.
 *
 * <p>The {@code subject} is a cross-domain {@link com.naturalist.catalog.EntityRef} that this module
 * may not reconstruct on its own (the DAG forbids importing the subject domains). It is stored flat
 * as {@code subject_domain} / {@code subject_rank} / {@code subject_name} (no FK — the target lives in
 * another domain), and the typed value is rebuilt through the injected {@link EntityRefResolver};
 * {@link #from} likewise reads the rank discriminator through it. The uuid {@code id} travels as text
 * and is cast to uuid in SQL ({@code ::uuid}) — MyBatis has no UUID type handler.
 */
@DboSchema(table = "citation_association", primaryKey = "id",
           foreignKeys = @Fk(columns = "citation_id", references = "citation(id)"),
           entity = CitationAssociation.class)
final class CitationAssociationDbo implements Dbo {
    String id;             // the CitationAssociationId's UUID as text; the mapper casts it (::uuid)
    String citationName;   // JOIN projection (read) / nested-select key (write); not a stored column
    String subjectDomain;
    String subjectRank;
    String subjectName;
    String note;           // nullable

    static CitationAssociationDbo from(CitationAssociation a, EntityRefResolver resolver) {
        CitationAssociationDbo d = new CitationAssociationDbo();
        d.id = a.id().value().toString();
        d.citationName = a.citationName().value();
        d.subjectDomain = a.subject().domain().value();
        d.subjectName = a.subject().name().value();
        d.subjectRank = resolver.rankOf(a.subject());
        d.note = a.note();
        Observer.forClass(CitationAssociationDbo.class)
                .arguments("from", i -> i.observable(d, "dbo"))
                .throwWhenInvalid();
        return d;
    }

    CitationAssociation toEntity(EntityRefResolver resolver) {
        return new CitationAssociation(
                CitationAssociationId.of(UUID.fromString(id)),
                CitationName.of(citationName),
                resolver.resolve(subjectDomain, subjectRank, subjectName),
                note);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .notBlank(id, "id")
                .notNull(citationName, "citationName").kebabFormat(citationName, "citationName")
                    .maxLength(citationName, 200, "citationName")
                .notBlank(subjectDomain, "subjectDomain").maxLength(subjectDomain, 64, "subjectDomain")
                .notBlank(subjectRank, "subjectRank").maxLength(subjectRank, 32, "subjectRank")
                .notBlank(subjectName, "subjectName").maxLength(subjectName, 128, "subjectName");
    }
}
