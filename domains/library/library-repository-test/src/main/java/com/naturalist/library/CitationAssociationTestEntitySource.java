package com.naturalist.library;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.naturalist.authority.CitationName;
import com.naturalist.catalog.DomainId;
import com.naturalist.catalog.EntityRef;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;
import com.naturalist.data.UniqueConstraint;
import com.naturalist.ddd.EntityName;
import com.naturalist.insects.InsectFamilyName;
import com.naturalist.insects.InsectGenusName;
import com.naturalist.insects.InsectOrderName;
import com.naturalist.insects.InsectSpeciesName;
import com.naturalist.insects.InsectsDomain;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

public class CitationAssociationTestEntitySource
        extends TestEntitySource<CitationAssociationId, CitationAssociation> {

    private static final InsectsDomain INSECTS = new InsectsDomain();

    public CitationAssociationTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFromDto("library/citation-associations.json");
    }

    @Override
    protected List<UniqueConstraint<CitationAssociation>> uniqueConstraints() {
        return List.of(
                new UniqueConstraint<>() {
                    @Override
                    public String name() {
                        return "citationName+subject";
                    }

                    @Override
                    public Function<CitationAssociation, ?> valueFunction() {
                        return a -> a.citationName().value() + ":"
                                + a.subject().domain().value() + ":"
                                + a.subject().name().value();
                    }
                });
    }

    private void loadFromDto(String resourcePath) {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalStateException("Resource not found: " + resourcePath);
            }
            List<CitationAssociationDto> dtos = mapper.readValue(is,
                    new TypeReference<>() {});
            for (CitationAssociationDto dto : dtos) {
                insert(dto.toEntity());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load " + resourcePath, e);
        }
    }

    private record CitationAssociationDto(
            String name,
            String citationName,
            String subjectDomain,
            String subjectRank,
            String subjectName,
            @Nullable String note
    ) {
        CitationAssociation toEntity() {
            return new CitationAssociation(
                    CitationAssociationId.of(UUID.fromString(name)),
                    CitationName.of(citationName),
                    new EntityRef(resolveDomain(subjectDomain),
                            resolveEntityName(subjectDomain, subjectRank, subjectName)),
                    note);
        }

        private static DomainId resolveDomain(String domain) {
            return switch (domain) {
                case "insects" -> INSECTS;
                default -> throw new IllegalArgumentException("Unknown domain: " + domain);
            };
        }

        private static EntityName resolveEntityName(String domain, String rank, String slug) {
            if ("insects".equals(domain)) {
                return switch (rank) {
                    case "ORDER" -> InsectOrderName.of(slug);
                    case "FAMILY" -> InsectFamilyName.of(slug);
                    case "GENUS" -> InsectGenusName.of(slug);
                    case "SPECIES" -> InsectSpeciesName.of(slug);
                    default -> throw new IllegalArgumentException(
                            "Unknown insect rank: " + rank);
                };
            }
            throw new IllegalArgumentException("Unknown domain: " + domain);
        }
    }
}
