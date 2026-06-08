package com.naturalist.insects;

import com.naturalist.data.ForeignKeyConstraint;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;

import java.util.List;

public class InsectGenusTestEntitySource extends TestEntitySource<InsectGenusName, InsectGenus> {

    public InsectGenusTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("insects/insect-genera.json");
    }

    @Override
    protected List<ForeignKeyConstraint<InsectGenus, ?>> foreignKeyConstraints() {
        return List.of(
                ForeignKeyConstraint.of(
                        "familyName",
                        InsectGenus::familyName,
                        InsectFamilyTestEntitySource.class));
    }
}
