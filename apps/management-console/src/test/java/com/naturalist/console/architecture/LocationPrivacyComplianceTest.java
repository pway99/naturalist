package com.naturalist.console.architecture;

import com.naturalist.ddd.Aggregate;
import com.naturalist.ddd.Entity;
import com.naturalist.ddd.NamedEntity;
import com.naturalist.ddd.ReadModel;
import com.naturalist.observation.Coordinates;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

/**
 * Build-time enforcement of the photo location-privacy design (2026-11-08): photos are stripped of
 * metadata, and only a coarse {@code GridCell} may be stored. {@link Coordinates} is the exact,
 * request-scoped point read from an upload or posted by the browser; it must never become a
 * component of anything that is persisted.
 *
 * <p>A persisted domain type is any {@link Entity}, {@link NamedEntity}, {@link Aggregate} or
 * {@link ReadModel}. Record components compile to private final fields, so a field of raw type
 * {@code Coordinates} on such a class is exactly a persisted-component leak. Value objects and
 * plain request-scoped carriers (for example {@code ImageUpload}) may hold one.
 *
 * <p>Test classes are excluded, matching the sibling compliance tests.
 */
@AnalyzeClasses(
        packages = "com.naturalist",
        importOptions = ImportOption.DoNotIncludeTests.class)
class LocationPrivacyComplianceTest {

    @ArchTest
    static final ArchRule coordinatesNeverPersist =
            noFields()
                    .that().haveRawType(Coordinates.class)
                    .should().beDeclaredInClassesThat(persistedDomainType())
                    .because("exact coordinates are request-scoped; only a GridCell may be stored "
                            + "(design 2026-11-08)")
                    // No production class declares a Coordinates field today (it only flows through
                    // method signatures and locals), so the rule is vacuous until one does.
                    .allowEmptyShould(true);

    private static DescribedPredicate<JavaClass> persistedDomainType() {
        return assignableTo(Entity.class)
                .or(assignableTo(NamedEntity.class))
                .or(assignableTo(Aggregate.class))
                .or(assignableTo(ReadModel.class))
                .as("an Entity, NamedEntity, Aggregate or ReadModel");
    }
}
