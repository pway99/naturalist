package com.naturalist.spring;

import com.naturalist.infrastructure.DomainService;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.util.List;

/**
 * Registrar that discovers every class annotated with
 * {@link DomainService} on the classpath and registers it with the
 * {@link BeanDefinitionRegistry}. Imported by an app's composition root via
 * {@code @Import(DomainServiceScan.class)} on a {@code @Configuration}
 * class.
 *
 * <h2>Why a registrar and not {@code @ComponentScan}</h2>
 * Spring's {@code @ComponentScan} only matches stereotypes meta-annotated
 * with {@link org.springframework.stereotype.Component @Component}. The
 * kernel's {@link DomainService} carries no Spring meta-annotation by
 * design — domain {@code *-core} code must not import Spring. This
 * registrar therefore drives the scanner directly with an
 * {@link AnnotationTypeFilter} on the marker, side-stepping the stereotype
 * requirement.
 *
 * <h2>Scan scope</h2>
 * The base packages are listed in {@link #BASE_PACKAGES}. Post-M7 of the
 * runtime architecture refactor the scan covers the full
 * {@code com.naturalist} root: every domain that ships a {@code DomainId}
 * subtype, an {@code EntityRefLinker}, a {@link
 * com.naturalist.catalog.CatalogContribution}, or an {@link
 * com.naturalist.catalog.EntityReferences} provider with the
 * {@code @DomainService} marker is auto-discovered. When (or if) a
 * third-party plugin model lands the scope narrows to a {@code META-INF}
 * registration list — see the runtime architecture refactor plan's "Open
 * questions" section.
 */
public class DomainServiceScan implements ImportBeanDefinitionRegistrar {

    static final List<String> BASE_PACKAGES = List.of("com.naturalist");

    /**
     * Fully-qualified bean names, not the decapitalised short name. Under the
     * ADR-020 convention, standalone {@code @DomainService} adapters carry their
     * domain prefix — e.g. {@code InsectSpeciesQueryImpl} and
     * {@code PlantSpeciesQueryImpl} — so their simple names no longer collide.
     * The FQN generator does not lean on that convention, though: were two
     * domains ever to ship classes with the same simple name, short-name
     * generation would collide on {@code "speciesQueryImpl"} and the
     * duplicate-skip below would silently drop the second domain's bean, leaving
     * its query interface unsatisfiable at wiring time. The FQN keeps them
     * distinct regardless; injection is by type, so callers are unaffected.
     */
    private final BeanNameGenerator beanNameGenerator = new FullyQualifiedAnnotationBeanNameGenerator();

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
                                        BeanDefinitionRegistry registry) {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(DomainService.class));
        for (String basePackage : BASE_PACKAGES) {
            for (BeanDefinition candidate : scanner.findCandidateComponents(basePackage)) {
                if (!(candidate instanceof AbstractBeanDefinition definition)) {
                    continue;
                }
                String beanName = beanNameGenerator.generateBeanName(definition, registry);
                if (registry.containsBeanDefinition(beanName)) {
                    continue;
                }
                registry.registerBeanDefinition(beanName, definition);
            }
        }
    }
}
