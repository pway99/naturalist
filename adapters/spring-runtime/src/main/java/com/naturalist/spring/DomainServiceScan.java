package com.naturalist.spring;

import com.naturalist.infrastructure.DomainService;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
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
 * <h2>Pilot scope</h2>
 * The base packages are listed in {@link #BASE_PACKAGES}. M6 of the
 * runtime architecture refactor wires only the plants pilot through
 * Spring; M7 broadens the scope across every domain that ships a
 * {@code DomainId} subtype and its catalog wiring.
 */
public class DomainServiceScan implements ImportBeanDefinitionRegistrar {

    static final List<String> BASE_PACKAGES = List.of(
            "com.naturalist.catalog",
            "com.naturalist.plants"
    );

    private final BeanNameGenerator beanNameGenerator = new AnnotationBeanNameGenerator();

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
