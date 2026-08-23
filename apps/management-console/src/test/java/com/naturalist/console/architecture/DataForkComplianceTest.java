package com.naturalist.console.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaConstructorCall;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Main-code enforcement of the pre-RDBMS data-graph discipline. The app runs on the
 * single shared NaturalistDatabase bean (adapters/spring-test-data). No main-code class
 * may fork a private graph via NaturalistDatabase.create(), and none may construct a
 * TestEntitySource directly — getNamed is the only sanctioned path (ADR-001). Test-code
 * enforcement of the same rules lives per-module (DataForkArchTest in the affected modules).
 */
@AnalyzeClasses(packages = "com.naturalist", importOptions = ImportOption.DoNotIncludeTests.class)
class DataForkComplianceTest {

    /** The only legitimate main-code callers of NaturalistDatabase.create(). */
    private static final Set<String> CREATE_ALLOWED = Set.of(
            "com.naturalist.data.TestEntitySourceTest",
            "com.naturalist.spring.data.TestDataConfiguration");

    @ArchTest
    static final ArchRule noPrivateGraphFork =
            noClasses()
                    .that(isNotASanctionedCreateCaller())
                    .should().callMethodWhere(callsNaturalistDatabaseCreate())
                    .because("the app runs on the shared NaturalistDatabase bean; "
                            + "forking a private graph bypasses Spring — see "
                            + "docs/plans/2026-08-22-controller-defork-archunit-plan.md")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule noDirectTestEntitySourceConstruction =
            noClasses()
                    .should().callConstructorWhere(constructsATestEntitySource())
                    .because("acquire a TestEntitySource via NaturalistDatabase#getNamed, "
                            + "never `new` — see ADR-001")
                    .allowEmptyShould(true);

    private static DescribedPredicate<JavaClass> isNotASanctionedCreateCaller() {
        return new DescribedPredicate<>("are not sanctioned NaturalistDatabase.create() callers") {
            @Override
            public boolean test(JavaClass javaClass) {
                return !CREATE_ALLOWED.contains(javaClass.getFullName());
            }
        };
    }

    private static DescribedPredicate<JavaMethodCall> callsNaturalistDatabaseCreate() {
        return new DescribedPredicate<>("call NaturalistDatabase.create()") {
            @Override
            public boolean test(JavaMethodCall call) {
                return call.getTargetOwner().getFullName().equals("com.naturalist.data.NaturalistDatabase")
                        && call.getTarget().getName().equals("create");
            }
        };
    }

    private static DescribedPredicate<JavaConstructorCall> constructsATestEntitySource() {
        return new DescribedPredicate<>("construct a TestEntitySource") {
            @Override
            public boolean test(JavaConstructorCall call) {
                String simpleName = call.getTargetOwner().getSimpleName();
                // Exclude the abstract base type itself: every concrete
                // "<X>TestEntitySource" subclass legitimately calls
                // super(database) from its own constructor, which is a
                // constructor call whose target owner is exactly
                // "TestEntitySource" — not a `new <X>TestEntitySource(...)`
                // call site, which is what this rule targets.
                return !simpleName.equals("TestEntitySource") && simpleName.endsWith("TestEntitySource");
            }
        };
    }
}
