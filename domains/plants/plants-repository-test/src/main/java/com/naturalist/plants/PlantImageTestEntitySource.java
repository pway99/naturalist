package com.naturalist.plants;

import com.naturalist.data.ForeignKeyConstraint;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;

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
        extends TestEntitySource<PlantImageId, PlantImage> {

    public PlantImageTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("plants/plant-images.json");
    }

    @Override
    protected List<ForeignKeyConstraint<PlantImage, ?>> foreignKeyConstraints() {
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
