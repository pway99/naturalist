package com.naturalist.plants;

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

/**
 * Plant field observations. The {@code subject} is a {@link PlantRankName}, so referential
 * integrity is declared per-rank: each constraint extracts the subject only when it is that
 * rank (null otherwise, which the FK enforcer treats as a pass), so a species-rank subject
 * is checked against the species source, a genus subject against the genus source, and so on.
 */
public class PlantObservationTestEntitySource
        extends TestEntitySource<PlantObservationId, OrganismObservation<PlantObservationId, PlantRankName>> {

    private final ObjectMapper mapper = TestDataHelper.newBaseMapper()
            .setInjectableValues(new InjectableValues.Std()
                    .addValue(RankNameReconstructor.class, (RankNameReconstructor) PlantRankName::of));

    public PlantObservationTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("plants/field-observations.json", this::parse);
    }

    private List<OrganismObservation<PlantObservationId, PlantRankName>> parse(String json) {
        try {
            JavaType t = mapper.getTypeFactory().constructParametricType(
                    OrganismObservation.class, PlantObservationId.class, PlantRankName.class);
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
    protected List<ForeignKeyConstraint<OrganismObservation<PlantObservationId, PlantRankName>, ?>> foreignKeyConstraints() {
        return List.of(
                ForeignKeyConstraint.of("subject (order)",
                        o -> o.subject() instanceof PlantOrderName x ? x : null,
                        PlantOrderTestEntitySource.class),
                ForeignKeyConstraint.of("subject (family)",
                        o -> o.subject() instanceof PlantFamilyName x ? x : null,
                        PlantFamilyTestEntitySource.class),
                ForeignKeyConstraint.of("subject (genus)",
                        o -> o.subject() instanceof PlantGenusName x ? x : null,
                        PlantGenusTestEntitySource.class),
                ForeignKeyConstraint.of("subject (species)",
                        o -> o.subject() instanceof PlantSpeciesName x ? x : null,
                        PlantSpeciesTestEntitySource.class));
    }
}
