package com.naturalist.observation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.naturalist.data.FileName;
import com.naturalist.ddd.EntityId;
import com.naturalist.taxonomy.LinealRank;
import com.naturalist.taxonomy.RankName;
import com.naturalist.taxonomy.RankNameReconstructor;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrganismImageTest {

    static final class FakeImageId extends EntityId {
        private FakeImageId(UUID v) { super(v); }
        @JsonCreator static FakeImageId of(UUID v) { return new FakeImageId(v); }
        static FakeImageId create() { return new FakeImageId(EntityId.newUUID()); }
    }

    static final class FakeObsId extends EntityId {
        private FakeObsId(UUID v) { super(v); }
        @JsonCreator static FakeObsId of(UUID v) { return new FakeObsId(v); }
        static FakeObsId create() { return new FakeObsId(EntityId.newUUID()); }
    }

    record FakeGenusName(String value) implements RankName {
        @Override public LinealRank rank() { return LinealRank.GENUS; }
    }

    private static final RankNameReconstructor RECONSTRUCTOR = (slug, rank) -> new FakeGenusName(slug);

    private static ObjectMapper mapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .setInjectableValues(new InjectableValues.Std()
                        .addValue(RankNameReconstructor.class, RECONSTRUCTOR));
    }

    @Test
    void serializesParentNameAsSelfDescribingObjectAndRoundtrips() throws Exception {
        var img = new OrganismImage<FakeImageId, FakeObsId, FakeGenusName>(
                FakeImageId.create(), new FakeGenusName("empoasca"),
                Instant.parse("2026-04-16T00:00:00Z"), FileName.of("IMG_9047.HEIC"), null);

        ObjectMapper mapper = mapper();
        String json = mapper.writeValueAsString(img);
        assertThat(json).contains("\"rank\":\"GENUS\"").contains("\"value\":\"empoasca\"");

        var type = mapper.getTypeFactory().constructParametricType(
                OrganismImage.class, FakeImageId.class, FakeObsId.class, FakeGenusName.class);
        OrganismImage<FakeImageId, FakeObsId, FakeGenusName> decoded = mapper.readValue(json, type);
        assertThat(decoded.id()).isEqualTo(img.id());
        assertThat(decoded.parentName().value()).isEqualTo("empoasca");
        assertThat(decoded.observationId()).isNull();
    }

    @Test
    void nullObservationIdIsAllowed() {
        var img = new OrganismImage<FakeImageId, FakeObsId, FakeGenusName>(
                FakeImageId.create(), new FakeGenusName("empoasca"),
                Instant.now(), FileName.of("a.heic"), null);
        assertThat(img.observationId()).isNull();
    }
}
