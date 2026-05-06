package com.naturalist.data;

import com.naturalist.observability.Observable;

import java.util.function.Function;

/**
 * Declarative referential-integrity check on a {@link TestEntitySource}.
 * Each constraint names one component on {@code ENTITY}, extracts its
 * foreign-name value, and asserts that the value resolves to an entity in
 * the foreign source identified by {@link #foreignSourceClass()}.
 *
 * <p>The shape mirrors a relational FK in DDL — {@code <column> REFERENCES
 * <table>(<name>)} — so the eventual rdbms adapter can translate each
 * {@code ForeignKeyConstraint} mechanically. The kernel's in-memory enforcer
 * runs on every {@code insert} / {@code update} via
 * {@link TestEntitySource#preSaveChecks(Observable, Object)}.
 *
 * <h2>Cross-domain limitation</h2>
 * Resolution goes through {@link NaturalistDatabase#getNamed} on a
 * {@link TestEntitySource} class — both peers must therefore be reachable
 * from the same module's classpath. That naturally restricts FK declarations
 * to <em>intra-domain</em> references: a plants source cannot declare an FK
 * into the chemistry source without crossing the {@code <domain>-repository-test}
 * module boundary, which the project's DAG forbids. Cross-domain referential
 * integrity remains a service-layer concern, matching the modular-monolith
 * RDBMS shape this kernel is preparing for.
 *
 * <h2>Null FK values</h2>
 * A {@code null} foreign-name value is treated as a pass. Whether {@code null}
 * is permitted at all is the record-invariant layer's responsibility — a
 * required FK should be enforced via {@code entityName(...)} on the entity's
 * {@code invariants()}; this constraint enforces only that a non-null value
 * resolves.
 *
 * @param <ENTITY>  the entity type being saved
 * @param <FK_NAME> the {@link com.naturalist.ddd.EntityName} subtype of the
 *                  foreign source's primary key
 */
public interface ForeignKeyConstraint<ENTITY extends Observable, FK_NAME> {

    String name();

    Function<ENTITY, FK_NAME> valueFunction();

    Class<? extends TestEntitySource<FK_NAME, ?>> foreignSourceClass();

    default FK_NAME value(ENTITY entity) {
        return valueFunction().apply(entity);
    }

    /**
     * One-line factory for the common shape — a constraint named {@code name}
     * that pulls {@code FK_NAME} from {@code valueFunction} and resolves it
     * against {@code foreignSourceClass}.
     */
    static <ENTITY extends Observable, FK_NAME> ForeignKeyConstraint<ENTITY, FK_NAME> of(
            String name,
            Function<ENTITY, FK_NAME> valueFunction,
            Class<? extends TestEntitySource<FK_NAME, ?>> foreignSourceClass) {
        return new ForeignKeyConstraint<>() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public Function<ENTITY, FK_NAME> valueFunction() {
                return valueFunction;
            }

            @Override
            public Class<? extends TestEntitySource<FK_NAME, ?>> foreignSourceClass() {
                return foreignSourceClass;
            }
        };
    }
}
