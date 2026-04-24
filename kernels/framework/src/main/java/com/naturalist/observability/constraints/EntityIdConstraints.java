package com.naturalist.observability.constraints;

import com.naturalist.ddd.EntityId;
import com.naturalist.observability.Constraint;

public interface EntityIdConstraints {

    record EntityIdConstraint<NAME extends EntityId>(
            NAME value,
            String name
    ) implements Constraint<NAME> {

        @Override
        public boolean isValid() {
            return EntityId.isValid(value);
        }

        public EntityIdConstraint<NAME> withName(String name) {
            return new EntityIdConstraint<>(value, name);
        }

        @Override
        public String errorMessage() {
            return EntityId.errorMessageFor(value);
        }
    }
}
