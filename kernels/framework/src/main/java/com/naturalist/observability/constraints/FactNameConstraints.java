package com.naturalist.observability.constraints;

import com.naturalist.ddd.FactName;
import com.naturalist.observability.Constraint;

public interface FactNameConstraints {

    record FactNameConstraint<NAME extends FactName>(
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
