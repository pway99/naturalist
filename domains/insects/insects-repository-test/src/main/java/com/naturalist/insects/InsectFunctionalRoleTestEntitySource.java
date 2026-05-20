package com.naturalist.insects;

import com.naturalist.data.ForeignKeyConstraint;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;
import com.naturalist.data.UniqueConstraint;

import java.util.List;
import java.util.function.Function;

public class InsectFunctionalRoleTestEntitySource extends TestEntitySource<InsectFunctionalRoleId, InsectFunctionalRole> {

    public InsectFunctionalRoleTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("insects/insect-functional-roles.json");
    }

    @Override
    protected List<UniqueConstraint<InsectFunctionalRole>> uniqueConstraints() {
        return List.of(
                new UniqueConstraint<>() {
                    @Override
                    public String name() {
                        return "parentName";
                    }

                    @Override
                    public Function<InsectFunctionalRole, ?> valueFunction() {
                        return InsectFunctionalRole::parentName;
                    }
                });
    }

    @Override
    protected List<ForeignKeyConstraint<InsectFunctionalRole, ?>> foreignKeyConstraints() {
        return List.of(
                ForeignKeyConstraint.of(
                        "parentName (family)",
                        role -> role.parentName() instanceof InsectFamilyName f ? f : null,
                        InsectFamilyTestEntitySource.class),
                ForeignKeyConstraint.of(
                        "parentName (genus)",
                        role -> role.parentName() instanceof InsectGenusName g ? g : null,
                        InsectGenusTestEntitySource.class),
                ForeignKeyConstraint.of(
                        "parentName (species)",
                        role -> role.parentName() instanceof InsectSpeciesName s ? s : null,
                        InsectSpeciesTestEntitySource.class));
    }
}
