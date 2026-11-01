package com.naturalist.library;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.naturalist.authority.CitationName;
import com.naturalist.catalog.EntityRef;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * On-disk shape of {@link CitationAssociation} in
 * {@code library/citation-associations.json}.
 *
 * <p>The entity's {@link EntityRef} subject pairs a
 * {@link com.naturalist.catalog.DomainId} — an open interface — with an abstract
 * {@link com.naturalist.ddd.EntityName}. Jackson can round-trip neither on its
 * own, which is why this catalog needs a hand-written DTO where every other
 * catalog serializes its entity directly. The DTO flattens the pair into
 * {@code subjectDomain} / {@code subjectRank} / {@code subjectName}.
 *
 * <h2>Why both directions take a resolver</h2>
 * Flattening and rebuilding the pair needs the owning domain's factory, and the
 * rank discriminator is encoded in the concrete {@code EntityName} subtype — so
 * neither direction can be done generically. This DTO therefore names no domain
 * at all: it delegates both to an {@link EntityRefResolver}, the same port the
 * {@code library-repository-rdbms} adapter is handed at the composition root.
 *
 * <p>It used to resolve the pair inline, with a {@code case "insects"} switch and
 * an {@code instanceof InsectRankName} check. That duplicated
 * {@code InsectsEntityRefResolver} branch for branch, and it made the catalog
 * file insects-only — a plant or chemistry subject threw
 * {@code IllegalArgumentException} from inside library's own serialization.
 * Adding a second subject domain is now a different resolver rather than an
 * edit here.
 *
 * <p>Both directions are declared here so
 * {@link CitationAssociationTestEntitySource} can register this shape as its
 * flush form via {@code TestEntitySource.writable(...)} — without that, console-
 * driven inserts round-trip to a shape the loader cannot read back.
 */
record CitationAssociationJson(
        String id,
        String citationName,
        String subjectDomain,
        String subjectRank,
        String subjectName,
        @Nullable String note
) {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static List<CitationAssociation> parseAll(String json, EntityRefResolver resolver) {
        try {
            List<CitationAssociationJson> dtos =
                    MAPPER.readValue(json, new TypeReference<>() {});
            List<CitationAssociation> entities = new ArrayList<>(dtos.size());
            for (CitationAssociationJson dto : dtos) {
                entities.add(dto.toEntity(resolver));
            }
            return entities;
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to parse citation associations", e);
        }
    }

    static CitationAssociationJson fromEntity(CitationAssociation association,
                                              EntityRefResolver resolver) {
        EntityRef subject = association.subject();
        return new CitationAssociationJson(
                association.id().value().toString(),
                association.citationName().value(),
                subject.domain().value(),
                resolver.rankOf(subject),
                subject.name().value(),
                association.note());
    }

    CitationAssociation toEntity(EntityRefResolver resolver) {
        return new CitationAssociation(
                CitationAssociationId.of(UUID.fromString(id)),
                CitationName.of(citationName),
                resolver.resolve(subjectDomain, subjectRank, subjectName),
                note);
    }
}
