package com.naturalist.plants;

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

/**
 * Plant images. The {@code parentName} is a {@link PlantRankName}, so referential integrity
 * is declared per-rank: each constraint extracts the parent only when it is that rank (null
 * otherwise, which the FK enforcer treats as a pass), so a species-rank parent is checked
 * against the species source, a genus parent against the genus source, and so on. Mirrors
 * {@code InsectImageTestEntitySource}, extended with the order rung the plant rank chain
 * carries.
 */
public class PlantImageTestEntitySource
        extends TestEntitySource<PlantImageId, OrganismImage<PlantImageId, PlantObservationId, PlantRankName>> {

    private final ObjectMapper mapper = TestDataHelper.newBaseMapper()
            .setInjectableValues(new InjectableValues.Std()
                    .addValue(RankNameReconstructor.class, (RankNameReconstructor) PlantRankName::of));

    public PlantImageTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("plants/plant-images.json", this::parse);
    }

    private List<OrganismImage<PlantImageId, PlantObservationId, PlantRankName>> parse(String json) {
        try {
            JavaType t = mapper.getTypeFactory().constructParametricType(
                    OrganismImage.class, PlantImageId.class, PlantObservationId.class, PlantRankName.class);
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
    protected List<ForeignKeyConstraint<OrganismImage<PlantImageId, PlantObservationId, PlantRankName>, ?>> foreignKeyConstraints() {
        return List.of(
                ForeignKeyConstraint.of("parentName (order)",
                        image -> image.parentName() instanceof PlantOrderName x ? x : null,
                        PlantOrderTestEntitySource.class),
                ForeignKeyConstraint.of("parentName (family)",
                        image -> image.parentName() instanceof PlantFamilyName x ? x : null,
                        PlantFamilyTestEntitySource.class),
                ForeignKeyConstraint.of("parentName (genus)",
                        image -> image.parentName() instanceof PlantGenusName x ? x : null,
                        PlantGenusTestEntitySource.class),
                ForeignKeyConstraint.of("parentName (species)",
                        image -> image.parentName() instanceof PlantSpeciesName x ? x : null,
                        PlantSpeciesTestEntitySource.class));
    }
}
