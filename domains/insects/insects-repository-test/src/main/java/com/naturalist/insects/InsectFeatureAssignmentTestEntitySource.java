package com.naturalist.insects;

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

public class InsectFeatureAssignmentTestEntitySource
        extends TestEntitySource<InsectFeatureAssignmentId,
                OrganismFeatureAssignment<InsectFeatureAssignmentId, InsectFeatureId, InsectRankName>> {

    private final ObjectMapper mapper = TestDataHelper.newBaseMapper()
            .setInjectableValues(new InjectableValues.Std()
                    .addValue(RankNameReconstructor.class, (RankNameReconstructor) InsectRankName::of));

    public InsectFeatureAssignmentTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("insects/insect-feature-assignments.json", this::parse);
    }

    private List<OrganismFeatureAssignment<InsectFeatureAssignmentId, InsectFeatureId, InsectRankName>> parse(String json) {
        try {
            JavaType t = mapper.getTypeFactory().constructParametricType(
                    OrganismFeatureAssignment.class,
                    InsectFeatureAssignmentId.class, InsectFeatureId.class, InsectRankName.class);
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
    protected List<UniqueConstraint<OrganismFeatureAssignment<InsectFeatureAssignmentId, InsectFeatureId, InsectRankName>>> uniqueConstraints() {
        return List.of(
                new UniqueConstraint<>() {
                    @Override
                    public String name() {
                        return "featureId+rankName";
                    }

                    @Override
                    public Function<OrganismFeatureAssignment<InsectFeatureAssignmentId, InsectFeatureId, InsectRankName>, ?> valueFunction() {
                        return a -> a.featureId().value() + ":"
                                + a.rankName().value();
                    }
                });
    }

    @Override
    protected List<ForeignKeyConstraint<OrganismFeatureAssignment<InsectFeatureAssignmentId, InsectFeatureId, InsectRankName>, ?>> foreignKeyConstraints() {
        return List.of(
                ForeignKeyConstraint.of(
                        "featureId",
                        OrganismFeatureAssignment::featureId,
                        InsectFeatureTestEntitySource.class),
                ForeignKeyConstraint.of(
                        "rankName (order)",
                        a -> a.rankName() instanceof InsectOrderName o ? o : null,
                        InsectOrderTestEntitySource.class),
                ForeignKeyConstraint.of(
                        "rankName (family)",
                        a -> a.rankName() instanceof InsectFamilyName f ? f : null,
                        InsectFamilyTestEntitySource.class),
                ForeignKeyConstraint.of(
                        "rankName (genus)",
                        a -> a.rankName() instanceof InsectGenusName g ? g : null,
                        InsectGenusTestEntitySource.class),
                ForeignKeyConstraint.of(
                        "rankName (species)",
                        a -> a.rankName() instanceof InsectSpeciesName s ? s : null,
                        InsectSpeciesTestEntitySource.class));
    }

    /**
     * Enables {@code save()} to reconcile a unique-constraint match on
     * {@code featureId+rankName}: the existing row's id is retained (its
     * {@code ordinal} is overwritten with the incoming value), the
     * caller-supplied id is discarded. A re-identification that reassigns the
     * same feature (its id resolved to the existing {@code InsectFeature} row
     * by {@code save()}'s own unique-constraint reconciliation, not by any
     * deduplication performed here) to the same rank hits this branch instead
     * of tripping the constraint — see {@code InsectCatalogIdentificationTransaction}.
     */
    @Override
    protected OrganismFeatureAssignment<InsectFeatureAssignmentId, InsectFeatureId, InsectRankName> withKey(
            OrganismFeatureAssignment<InsectFeatureAssignmentId, InsectFeatureId, InsectRankName> a,
            InsectFeatureAssignmentId key) {
        return OrganismFeatureAssignment.of(key, a.featureId(), a.rankName(), a.ordinal());
    }
}
