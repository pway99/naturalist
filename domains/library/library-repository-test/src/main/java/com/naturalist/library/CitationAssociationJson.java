package com.naturalist.library;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.naturalist.authority.CitationName;
import com.naturalist.catalog.DomainId;
import com.naturalist.catalog.EntityRef;
import com.naturalist.ddd.EntityName;
import com.naturalist.insects.InsectRankName;
import com.naturalist.insects.InsectsDomain;
import com.naturalist.taxonomy.LinealRank;
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
 * <p>The entity's {@link EntityRef} subject pairs a {@link DomainId} — an open
 * interface — with an abstract {@link EntityName}. Jackson can round-trip
 * neither on its own, which is why this catalog needs a hand-written DTO where
 * every other catalog serializes its entity directly. The DTO flattens the pair
 * into {@code subjectDomain} / {@code subjectRank} / {@code subjectName} and
 * reconstructs the typed values on read.
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
    private static final InsectsDomain INSECTS = new InsectsDomain();

    static List<CitationAssociation> parseAll(String json) {
        try {
            List<CitationAssociationJson> dtos =
                    MAPPER.readValue(json, new TypeReference<>() {});
            List<CitationAssociation> entities = new ArrayList<>(dtos.size());
            for (CitationAssociationJson dto : dtos) {
                entities.add(dto.toEntity());
            }
            return entities;
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to parse citation associations", e);
        }
    }

    static CitationAssociationJson fromEntity(CitationAssociation association) {
        EntityRef subject = association.subject();
        EntityName name = subject.name();
        return new CitationAssociationJson(
                association.id().value().toString(),
                association.citationName().value(),
                subject.domain().value(),
                rankOf(name),
                name.value(),
                association.note());
    }

    CitationAssociation toEntity() {
        return new CitationAssociation(
                CitationAssociationId.of(UUID.fromString(id)),
                CitationName.of(citationName),
                new EntityRef(resolveDomain(subjectDomain),
                        resolveEntityName(subjectDomain, subjectRank, subjectName)),
                note);
    }

    private static String rankOf(EntityName name) {
        if (name instanceof InsectRankName rankName) {
            return rankName.rank().name();
        }
        throw new IllegalArgumentException(
                "Unsupported citation subject name type: " + name.getClass().getName());
    }

    private static DomainId resolveDomain(String domain) {
        return switch (domain) {
            case "insects" -> INSECTS;
            default -> throw new IllegalArgumentException("Unknown domain: " + domain);
        };
    }

    private static EntityName resolveEntityName(String domain, String rank, String slug) {
        if ("insects".equals(domain)) {
            // InsectRankName is a sealed interface that does not itself extend
            // EntityName — every permit implements both separately — so the
            // conversion needs an explicit cast rather than a plain return.
            return (EntityName) InsectRankName.of(slug, LinealRank.valueOf(rank));
        }
        throw new IllegalArgumentException("Unknown domain: " + domain);
    }
}
