package com.naturalist.spring.data;

import com.naturalist.data.MockDomainService;
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
 * Registrar that discovers every {@link MockDomainService}-annotated in-memory test double on the
 * classpath and registers it as a bean — the test-side mirror of {@code DomainServiceScan}.
 *
 * <p>The mocks are package-private and constructor-inject the shared
 * {@link com.naturalist.data.NaturalistDatabase} bean published by {@link TestDataConfiguration};
 * Spring instantiates them reflectively and consumers inject them by repository interface, exactly
 * as they inject the real adapters in production.
 *
 * <p>Reached through {@link MockDataConfiguration}, which the app's component scan picks up
 * whenever this jar is on the classpath — at {@code <scope>test</scope>}, so never in a
 * deployment. An app wiring the mocks in must also keep the {@code *-repository-rdbms} jars off
 * its test classpath (Surefire {@code classpathDependencyExcludes}); registering both halves would
 * leave every repository interface with two candidate beans and fail wiring.
 */
public class MockDomainServiceScan implements ImportBeanDefinitionRegistrar {

    static final List<String> BASE_PACKAGES = List.of("com.naturalist");

    /** Fully-qualified, for the same collision reasons documented on {@code DomainServiceScan}. */
    private final BeanNameGenerator beanNameGenerator = new FullyQualifiedAnnotationBeanNameGenerator();

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
                                        BeanDefinitionRegistry registry) {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(MockDomainService.class));
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
