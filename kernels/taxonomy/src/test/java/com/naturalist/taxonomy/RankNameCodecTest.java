package com.naturalist.taxonomy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RankNameCodecTest {

    /** A stand-in typed permit — models a domain rank name for the codec test. */
    record FakeSpeciesName(String value) implements RankName {
        @JsonCreator static FakeSpeciesName of(String v) { return new FakeSpeciesName(v); }
        @Override public LinealRank rank() { return LinealRank.SPECIES; }
    }

    record Holder(
            @JsonSerialize(using = RankNameSerializer.class)
            @JsonDeserialize(using = RankNameDeserializer.class)
            RankName subject) {}

    private static final RankNameReconstructor RECONSTRUCTOR =
            (slug, rank) -> new FakeSpeciesName(slug);

    private static ObjectMapper mapperWithReconstructor() {
        return new ObjectMapper().setInjectableValues(
                new InjectableValues.Std().addValue(RankNameReconstructor.class, RECONSTRUCTOR));
    }

    @Test
    void subjectRoundtripsAsSelfDescribingObjectAndReconstructsTypedPermit() throws Exception {
        ObjectMapper mapper = mapperWithReconstructor();
        Holder original = new Holder(new FakeSpeciesName("battus-philenor"));

        String json = mapper.writeValueAsString(original);
        Holder decoded = mapper.readValue(json, Holder.class);

        assertThat(json).contains("\"rank\":\"SPECIES\"");
        assertThat(json).contains("\"value\":\"battus-philenor\"");
        assertThat(decoded.subject()).isInstanceOf(FakeSpeciesName.class);
        assertThat(decoded.subject().value()).isEqualTo("battus-philenor");
        assertThat(decoded.subject().rank()).isEqualTo(LinealRank.SPECIES);
    }
}
