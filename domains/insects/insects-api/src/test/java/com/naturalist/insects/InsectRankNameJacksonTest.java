package com.naturalist.insects;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verification that {@link InsectRankName} polymorphic dispatch via
 * field-level {@code @JsonTypeInfo} produces a clean round-trip when the
 * permit classes carry the {@code @JsonValue}-on-{@code value()} contract
 * from {@code EntityName}.
 *
 * <p>The first iteration of this slice attempted dispatch at the
 * <em>interface</em> level (annotation on {@code InsectRankName} itself).
 * Jackson resolves polymorphic annotations from the runtime class
 * hierarchy, so the envelope leaked into every leaf-class serialization
 * site — including every {@code InsectSpecies.name} in the catalog, breaking
 * the JSON schema globally. The fix: keep the sealed interface bare and
 * declare the polymorphic dispatch on the <em>consumer field</em>. The
 * envelope then applies only where opted in.
 *
 * <p>This test class models the consumer pattern with a throwaway
 * {@code ParentHolder} record annotated the way {@code InsectImage} will
 * be annotated in Step 2.
 */
class InsectRankNameJacksonTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    record ParentHolder(
            @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                          property = "parentRank",
                          include = JsonTypeInfo.As.EXTERNAL_PROPERTY)
            @JsonSubTypes({
                    @JsonSubTypes.Type(value = InsectFamilyName.class, name = "FAMILY"),
                    @JsonSubTypes.Type(value = InsectGenusName.class, name = "GENUS"),
                    @JsonSubTypes.Type(value = InsectSpeciesName.class, name = "SPECIES"),
                    @JsonSubTypes.Type(value = InsectSubspeciesName.class, name = "SUBSPECIES")
            })
            InsectRankName parentName) {
    }

    @Test
    void speciesPermitRoundtripsThroughTheExternalPropertyEnvelope() throws Exception {
        ParentHolder original = new ParentHolder(InsectSpeciesName.of("battus-philenor"));

        String json = mapper.writeValueAsString(original);
        ParentHolder decoded = mapper.readValue(json, ParentHolder.class);

        assertThat(json).contains("\"parentRank\":\"SPECIES\"");
        assertThat(json).contains("\"parentName\":\"battus-philenor\"");
        assertThat(decoded.parentName()).isInstanceOf(InsectSpeciesName.class);
        assertThat(decoded.parentName()).isEqualTo(InsectSpeciesName.of("battus-philenor"));
    }

    @Test
    void genusPermitRoundtripsThroughTheExternalPropertyEnvelope() throws Exception {
        ParentHolder original = new ParentHolder(InsectGenusName.of("empoasca"));

        String json = mapper.writeValueAsString(original);
        ParentHolder decoded = mapper.readValue(json, ParentHolder.class);

        assertThat(json).contains("\"parentRank\":\"GENUS\"");
        assertThat(json).contains("\"parentName\":\"empoasca\"");
        assertThat(decoded.parentName()).isInstanceOf(InsectGenusName.class);
        assertThat(decoded.parentName()).isEqualTo(InsectGenusName.of("empoasca"));
    }

    @Test
    void familyPermitRoundtripsThroughTheExternalPropertyEnvelope() throws Exception {
        ParentHolder original = new ParentHolder(InsectFamilyName.of("papilionidae"));

        String json = mapper.writeValueAsString(original);
        ParentHolder decoded = mapper.readValue(json, ParentHolder.class);

        assertThat(json).contains("\"parentRank\":\"FAMILY\"");
        assertThat(json).contains("\"parentName\":\"papilionidae\"");
        assertThat(decoded.parentName()).isInstanceOf(InsectFamilyName.class);
        assertThat(decoded.parentName()).isEqualTo(InsectFamilyName.of("papilionidae"));
    }

    @Test
    void subspeciesPermitRoundtripsThroughTheExternalPropertyEnvelope() throws Exception {
        ParentHolder original = new ParentHolder(
                InsectSubspeciesName.of("battus-philenor-hirsuta"));

        String json = mapper.writeValueAsString(original);
        ParentHolder decoded = mapper.readValue(json, ParentHolder.class);

        assertThat(json).contains("\"parentRank\":\"SUBSPECIES\"");
        assertThat(json).contains("\"parentName\":\"battus-philenor-hirsuta\"");
        assertThat(decoded.parentName()).isInstanceOf(InsectSubspeciesName.class);
        assertThat(decoded.parentName())
                .isEqualTo(InsectSubspeciesName.of("battus-philenor-hirsuta"));
    }

    @Test
    void crossPermitEqualityIsClassQualified() {
        InsectRankName asGenus = InsectGenusName.of("battus");
        InsectRankName asSpecies = InsectSpeciesName.of("battus");

        assertThat(asGenus).isNotEqualTo(asSpecies);
    }

    @Test
    void directLeafTypeUseStillSerializesAsPlainString() throws Exception {
        InsectSpeciesName direct = InsectSpeciesName.of("battus-philenor");

        String json = mapper.writeValueAsString(direct);
        InsectSpeciesName decoded = mapper.readValue(json, InsectSpeciesName.class);

        assertThat(json).isEqualTo("\"battus-philenor\"");
        assertThat(decoded).isEqualTo(direct);
    }
}
