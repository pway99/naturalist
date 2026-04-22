package com.naturalist.observability.constraints;

import com.naturalist.ddd.EntityName;
import com.naturalist.ddd.EntityNameSet;
import com.naturalist.observability.Constraint;

import java.util.Collection;

public interface EntityNameConstraints {

    record EntityNameConstraint<NAME extends EntityName>(
            NAME value,
            String name
    ) implements Constraint<NAME> {

        @Override
        public boolean isValid() {
            return value != null && value.isValid();
        }

        public EntityNameConstraint<NAME> withName(String name) {
            return new EntityNameConstraint<>(value, name);
        }
    }

    record EntityNameCollectionConstraint<NAME extends EntityName>(
            Collection<NAME> value,
            String name
    ) implements Constraint<Collection<NAME>> {

        @Override
        public boolean isValid() {
            if (value == null) {
                return false;
            }
            for (NAME n : value) {
                if (n == null || n.isNotValid()) {
                    return false;
                }
            }
            return true;
        }

        public EntityNameCollectionConstraint<NAME> withName(String name) {
            return new EntityNameCollectionConstraint<>(value, name);
        }
    }

    record EntityNameSetConstraint<NAME extends EntityName>(
            EntityNameSet<NAME> value,
            String name
    ) implements Constraint<EntityNameSet<NAME>> {

        @Override
        public boolean isValid() {
            if (value == null) {
                return false;
            }
            return value.stream().allMatch(n -> n != null && n.isValid());
        }

        public EntityNameSetConstraint<NAME> withName(String name) {
            return new EntityNameSetConstraint<>(value, name);
        }
    }
}
