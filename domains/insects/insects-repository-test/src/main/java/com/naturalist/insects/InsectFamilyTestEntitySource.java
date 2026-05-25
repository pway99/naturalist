package com.naturalist.insects;

import com.naturalist.data.ForeignKeyConstraint;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;

import java.util.List;

public class InsectFamilyTestEntitySource extends TestEntitySource<InsectFamilyName, InsectFamily> {

    public InsectFamilyTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("insects/insect-families.json");
    }

    @Override
    protected List<ForeignKeyConstraint<InsectFamily, ?>> foreignKeyConstraints() {
        return List.of(ForeignKeyConstraint.of(
                "orderName",
                InsectFamily::orderName,
                InsectOrderTestEntitySource.class));
    }
}
