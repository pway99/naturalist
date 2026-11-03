package com.naturalist.console.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Sibling of {@code ResilienceComplianceTest} for ADR-028: the vendor SDK, and therefore the
 * credential it is handed, is confined to the two vendor adapters. The {@code System.getenv}
 * and credential-name rules are repo-wide OpenRewrite recipes instead, because this classpath
 * does not carry every module.
 */
@AnalyzeClasses(packages = "com.naturalist", importOptions = ImportOption.DoNotIncludeTests.class)
class SecretsComplianceTest {

    @ArchTest
    static final ArchRule onlyTheVendorAdaptersTouchTheSdk =
            noClasses()
                    .that().resideOutsideOfPackages(
                            "com.naturalist.vision.anthropic", "com.naturalist.textgeneration.anthropic")
                    .should().dependOnClassesThat().resideInAPackage("com.anthropic..")
                    .because("the vendor SDK, and therefore the credential, is confined to adapters/anthropic-* (ADR-028)");
}
