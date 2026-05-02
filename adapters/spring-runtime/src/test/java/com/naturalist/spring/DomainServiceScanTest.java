package com.naturalist.spring;

import com.naturalist.plants.spring.MarkedService;
import com.naturalist.plants.spring.UnmarkedService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link DomainServiceScan} discovers and registers classes
 * carrying {@link com.naturalist.infrastructure.DomainService @DomainService}
 * inside its pilot base packages, and ignores classes that lack the marker.
 *
 * <p>Test fixtures live under {@code com.naturalist.plants.spring} so the
 * scanner's pilot scope ({@code com.naturalist.plants}) picks them up
 * without dragging a production plants module onto this adapter's
 * classpath.
 */
class DomainServiceScanTest {

    @Test
    void registersMarkerAnnotatedClassesInPilotScope() {
        try (var ctx = new AnnotationConfigApplicationContext()) {
            ctx.register(ImportingConfig.class);
            ctx.refresh();

            assertThat(ctx.getBeansOfType(MarkedService.class))
                    .as("scanner should register classes annotated with @DomainService in the pilot scope")
                    .hasSize(1);
            assertThat(ctx.getBeansOfType(UnmarkedService.class))
                    .as("scanner should not register classes that lack @DomainService")
                    .isEmpty();
        }
    }

    @Configuration
    @Import(DomainServiceScan.class)
    static class ImportingConfig {
    }
}
