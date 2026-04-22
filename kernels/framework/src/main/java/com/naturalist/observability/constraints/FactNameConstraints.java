package com.naturalist.observability.constraints;

import com.naturalist.ddd.EntityId;
import com.naturalist.observability.Constraint;

public interface FactNameConstraints {

    record FactNameConstraint<NAME extends EntityId>(
            NAME value,
            String name
    ) implements Constraint<NAME> {

        @Override
        public boolean isValid() {
            return value != null && value.isValid();
        }

        public FactNameConstraint<NAME> withName(String name) {
            return new FactNameConstraint<>(value, name);
        }
    }
}
