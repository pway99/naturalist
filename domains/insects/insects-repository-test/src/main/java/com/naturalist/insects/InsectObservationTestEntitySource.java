package com.naturalist.insects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.naturalist.data.ForeignKeyConstraint;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestDataHelper;
import com.naturalist.data.TestEntitySource;
import com.naturalist.observation.OrganismObservation;
import com.naturalist.taxonomy.RankNameReconstructor;

import java.io.UncheckedIOException;
import java.util.List;

public class InsectObservationTestEntitySource
        extends TestEntitySource<InsectObservationId, OrganismObservation<InsectObservationId, InsectRankName>> {

    private final ObjectMapper mapper = TestDataHelper.newBaseMapper()
            .setInjectableValues(new InjectableValues.Std()
                    .addValue(RankNameReconstructor.class, (RankNameReconstructor) InsectRankName::of));

    public InsectObservationTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("insects/field-observations.json", this::parse);
    }

    private List<OrganismObservation<InsectObservationId, InsectRankName>> parse(String json) {
        try {
            JavaType t = mapper.getTypeFactory().constructParametricType(
                    OrganismObservation.class, InsectObservationId.class, InsectRankName.class);
            JavaType listT = mapper.getTypeFactory().constructCollectionType(List.class, t);
            return mapper.readValue(json, listT);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    protected ObjectMapper mapper() {
        return mapper;
    }

    @Override
    protected Class<?> writableClass() {
        return OrganismObservation.class;
    }

    @Override
    protected List<ForeignKeyConstraint<OrganismObservation<InsectObservationId, InsectRankName>, ?>> foreignKeyConstraints() {
        return List.of(
                ForeignKeyConstraint.of(
                        "subject (family)",
                        o -> o.subject() instanceof InsectFamilyName f ? f : null,
                        InsectFamilyTestEntitySource.class),
                ForeignKeyConstraint.of(
                        "subject (genus)",
                        o -> o.subject() instanceof InsectGenusName g ? g : null,
                        InsectGenusTestEntitySource.class),
                ForeignKeyConstraint.of(
                        "subject (species)",
                        o -> o.subject() instanceof InsectSpeciesName s ? s : null,
                        InsectSpeciesTestEntitySource.class));
    }
}
