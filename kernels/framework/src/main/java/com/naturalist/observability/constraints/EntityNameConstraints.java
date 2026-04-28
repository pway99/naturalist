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
            return EntityName.isValid(value);
        }

        public EntityNameConstraint<NAME> withName(String name) {
            return new EntityNameConstraint<>(value, name);
        }

        @Override
        public String errorMessage() {
            return EntityName.errorMessageFor(value);
        }
    }

    /**
     * Null-tolerant variant of {@link EntityNameConstraint}. A null value passes
     * (the field is optional); a non-null value must satisfy
     * {@link EntityName#isValid()} — kebab-case format and the concrete subtype's
     * {@code maxLength()}. Use for {@code @Nullable EntityName} record components
     * where the reference is genuinely optional but, when present, must still be
     * a valid slug.
     */
    record EntityNameOrNullConstraint<NAME extends EntityName>(
            NAME value,
            String name
    ) implements Constraint<NAME> {

        @Override
        public boolean isValid() {
            return value == null || value.isValid();
        }

        public EntityNameOrNullConstraint<NAME> withName(String name) {
            return new EntityNameOrNullConstraint<>(value, name);
        }

        @Override
        public String errorMessage() {
            return value == null ? "" : value.errorMessage();
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
