package com.naturalist.insects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.naturalist.data.ForeignKeyConstraint;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestDataHelper;
import com.naturalist.data.TestEntitySource;
import com.naturalist.observation.OrganismImage;
import com.naturalist.taxonomy.RankNameReconstructor;

import java.io.UncheckedIOException;
import java.util.List;

public class InsectImageTestEntitySource
        extends TestEntitySource<InsectImageId, OrganismImage<InsectImageId, InsectObservationId, InsectRankName>> {

    private final ObjectMapper mapper = TestDataHelper.newBaseMapper()
            .setInjectableValues(new InjectableValues.Std()
                    .addValue(RankNameReconstructor.class, (RankNameReconstructor) InsectRankName::of));

    public InsectImageTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("insects/insect-images.json", this::parse);
    }

    private List<OrganismImage<InsectImageId, InsectObservationId, InsectRankName>> parse(String json) {
        try {
            JavaType t = mapper.getTypeFactory().constructParametricType(
                    OrganismImage.class, InsectImageId.class, InsectObservationId.class, InsectRankName.class);
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
        return OrganismImage.class;
    }

    @Override
    protected List<ForeignKeyConstraint<OrganismImage<InsectImageId, InsectObservationId, InsectRankName>, ?>> foreignKeyConstraints() {
        return List.of(
                ForeignKeyConstraint.of(
                        "parentName (family)",
                        image -> image.parentName() instanceof InsectFamilyName f ? f : null,
                        InsectFamilyTestEntitySource.class),
                ForeignKeyConstraint.of(
                        "parentName (genus)",
                        image -> image.parentName() instanceof InsectGenusName g ? g : null,
                        InsectGenusTestEntitySource.class),
                ForeignKeyConstraint.of(
                        "parentName (species)",
                        image -> image.parentName() instanceof InsectSpeciesName s ? s : null,
                        InsectSpeciesTestEntitySource.class));
    }
}
