package com.naturalist.plants;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.naturalist.data.ForeignKeyConstraint;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestDataHelper;
import com.naturalist.data.TestEntitySource;
import com.naturalist.data.UniqueConstraint;
import com.naturalist.taxonomy.OrganismFeatureAssignment;
import com.naturalist.taxonomy.RankNameReconstructor;

import java.io.UncheckedIOException;
import java.util.List;
import java.util.function.Function;

/**
 * Mirrors {@code InsectFeatureAssignmentTestEntitySource} — the plant feature assignment is
 * now the generic {@link OrganismFeatureAssignment} parameterised on the plant identifiers,
 * carrying surrogate ({@link PlantFeatureAssignmentId}) identity. {@code rankName}
 * deserialises through the shared {@code {"rank":…,"value":…}} codec, so the mapper injects a
 * {@link RankNameReconstructor} ({@code PlantRankName::of}) to rebuild the concrete permit.
 * Only four rank foreign keys — plants has no {@code PlantSubspeciesName}.
 */
public class PlantFeatureAssignmentTestEntitySource
        extends TestEntitySource<PlantFeatureAssignmentId,
                OrganismFeatureAssignment<PlantFeatureAssignmentId, PlantFeatureId, PlantRankName>> {

    private final ObjectMapper mapper = TestDataHelper.newBaseMapper()
            .setInjectableValues(new InjectableValues.Std()
                    .addValue(RankNameReconstructor.class, (RankNameReconstructor) PlantRankName::of));

    public PlantFeatureAssignmentTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("plants/plant-feature-assignments.json", this::parse);
    }

    private List<OrganismFeatureAssignment<PlantFeatureAssignmentId, PlantFeatureId, PlantRankName>> parse(String json) {
        try {
            JavaType t = mapper.getTypeFactory().constructParametricType(
                    OrganismFeatureAssignment.class,
                    PlantFeatureAssignmentId.class, PlantFeatureId.class, PlantRankName.class);
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
        return OrganismFeatureAssignment.class;
    }

    @Override
    protected List<UniqueConstraint<OrganismFeatureAssignment<PlantFeatureAssignmentId, PlantFeatureId, PlantRankName>>> uniqueConstraints() {
        return List.of(
                new UniqueConstraint<>() {
                    @Override
                    public String name() {
                        return "featureId+rankName";
                    }

                    @Override
                    public Function<OrganismFeatureAssignment<PlantFeatureAssignmentId, PlantFeatureId, PlantRankName>, ?> valueFunction() {
                        return a -> a.featureId().value() + ":"
                                + a.rankName().value();
                    }
                });
    }

    @Override
    protected List<ForeignKeyConstraint<OrganismFeatureAssignment<PlantFeatureAssignmentId, PlantFeatureId, PlantRankName>, ?>> foreignKeyConstraints() {
        return List.of(
                ForeignKeyConstraint.of(
                        "featureId",
                        OrganismFeatureAssignment::featureId,
                        PlantFeatureTestEntitySource.class),
                ForeignKeyConstraint.of(
                        "rankName (order)",
                        a -> a.rankName() instanceof PlantOrderName o ? o : null,
                        PlantOrderTestEntitySource.class),
                ForeignKeyConstraint.of(
                        "rankName (family)",
                        a -> a.rankName() instanceof PlantFamilyName f ? f : null,
                        PlantFamilyTestEntitySource.class),
                ForeignKeyConstraint.of(
                        "rankName (genus)",
                        a -> a.rankName() instanceof PlantGenusName g ? g : null,
                        PlantGenusTestEntitySource.class),
                ForeignKeyConstraint.of(
                        "rankName (species)",
                        a -> a.rankName() instanceof PlantSpeciesName s ? s : null,
                        PlantSpeciesTestEntitySource.class));
    }

    /**
     * Enables {@code save()} to reconcile a unique-constraint match on
     * {@code featureId+rankName}: the existing row's id is retained (its
     * {@code ordinal} is overwritten with the incoming value), the
     * caller-supplied id is discarded. Mirrors {@code InsectFeatureAssignmentTestEntitySource}.
     */
    @Override
    protected OrganismFeatureAssignment<PlantFeatureAssignmentId, PlantFeatureId, PlantRankName> withKey(
            OrganismFeatureAssignment<PlantFeatureAssignmentId, PlantFeatureId, PlantRankName> a,
            PlantFeatureAssignmentId key) {
        return OrganismFeatureAssignment.of(key, a.featureId(), a.rankName(), a.ordinal());
    }
}
