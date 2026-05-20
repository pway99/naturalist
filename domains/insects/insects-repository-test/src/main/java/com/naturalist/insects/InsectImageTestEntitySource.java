package com.naturalist.insects;

import com.naturalist.data.ForeignKeyConstraint;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;

import java.util.List;

public class InsectImageTestEntitySource extends TestEntitySource<InsectImageId, InsectImage> {

    public InsectImageTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("insects/insect-images.json");
    }

    @Override
    protected List<ForeignKeyConstraint<InsectImage, ?>> foreignKeyConstraints() {
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
