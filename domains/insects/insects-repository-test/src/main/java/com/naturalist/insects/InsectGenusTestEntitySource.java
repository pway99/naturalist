package com.naturalist.insects;

import com.naturalist.data.ForeignKeyConstraint;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;

import java.util.List;

public class InsectGenusTestEntitySource extends TestEntitySource<InsectGenusName, InsectGenusInfo> {

    public InsectGenusTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("insects/insect-genera.json");
    }

    @Override
    protected List<ForeignKeyConstraint<InsectGenusInfo, ?>> foreignKeyConstraints() {
        return List.of(
                ForeignKeyConstraint.of(
                        "familyName",
                        InsectGenusInfo::familyName,
                        InsectFamilyTestEntitySource.class));
    }
}
