package com.naturalist.insects;

import com.naturalist.data.ForeignKeyConstraint;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;

import java.util.List;

public class InsectFamilyTestEntitySource extends TestEntitySource<InsectFamilyName, InsectFamilyInfo> {

    public InsectFamilyTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("insects/insect-families.json");
    }

    @Override
    protected List<ForeignKeyConstraint<InsectFamilyInfo, ?>> foreignKeyConstraints() {
        return List.of(ForeignKeyConstraint.of(
                "orderName",
                InsectFamilyInfo::orderName,
                InsectOrderTestEntitySource.class));
    }
}
