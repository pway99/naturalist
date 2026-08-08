package com.naturalist.library;

import com.naturalist.authority.CitationName;
import com.naturalist.catalog.EntityRef;
import com.naturalist.insects.InsectFamilyName;
import com.naturalist.insects.InsectsDomain;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CitationAssociationJsonTest {

    @Test
    void fromEntityFlattensTheSubjectRef() {
        var association = new CitationAssociation(
                CitationAssociationId.of(UUID.fromString("019f0001-a001-7001-8001-a00000000009")),
                CitationName.of("ref-chrysomelidae"),
                new EntityRef(new InsectsDomain(), InsectFamilyName.of("chrysomelidae")),
                "Identified via vision");

        var json = CitationAssociationJson.fromEntity(association);

        assertThat(json.citationName()).isEqualTo("ref-chrysomelidae");
        assertThat(json.subjectDomain()).isEqualTo("insects");
        assertThat(json.subjectRank()).isEqualTo("FAMILY");
        assertThat(json.subjectName()).isEqualTo("chrysomelidae");
        assertThat(json.note()).isEqualTo("Identified via vision");
    }

    @Test
    void toEntityRebuildsTheTypedSubjectRef() {
        var original = new CitationAssociation(
                CitationAssociationId.of(UUID.fromString("019f0001-a001-7001-8001-a00000000009")),
                CitationName.of("ref-chrysomelidae"),
                new EntityRef(new InsectsDomain(), InsectFamilyName.of("chrysomelidae")),
                "Identified via vision");

        var roundTripped = CitationAssociationJson.fromEntity(original).toEntity();

        assertThat(roundTripped).isEqualTo(original);
    }

    @Test
    void parseAllReadsTheCatalogFileShape() {
        var json = """
                [ {
                  "id" : "019f0001-a001-7001-8001-a00000000001",
                  "citationName" : "eol-lepidoptera-747",
                  "subjectDomain" : "insects",
                  "subjectRank" : "ORDER",
                  "subjectName" : "lepidoptera",
                  "note" : "EOL order page"
                } ]
                """;

        var entities = CitationAssociationJson.parseAll(json);

        assertThat(entities).hasSize(1);
        assertThat(entities.getFirst().citationName().value()).isEqualTo("eol-lepidoptera-747");
        assertThat(entities.getFirst().subject().name().value()).isEqualTo("lepidoptera");
    }
}
