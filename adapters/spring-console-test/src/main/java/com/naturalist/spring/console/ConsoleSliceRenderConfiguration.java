package com.naturalist.spring.console;

import gg.jte.TemplateEngine;
import gg.jte.springframework.boot.autoconfigure.JteProperties;
import gg.jte.springframework.boot.autoconfigure.JteViewResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.ViewResolver;

/**
 * Reusable JTE render wiring for console {@code @WebMvcTest} slices. A console's
 * test {@code @SpringBootConfiguration} {@code @Import}s this to get full-page
 * rendering (shared app layout + the domain's templates) via the filesystem
 * {@link ConsoleSliceTemplates} engine and gg.jte's Spring {@link JteViewResolver}
 * — the same resolver the app wires through the jte-spring-boot starter. The
 * console config then only declares its own controller bean and its
 * {@code NaturalistTestExtension} database.
 */
@Configuration
public class ConsoleSliceRenderConfiguration {

    @Bean
    public TemplateEngine jteTemplateEngine() {
        return ConsoleSliceTemplates.create();
    }

    @Bean
    public ViewResolver jteViewResolver(TemplateEngine engine) {
        JteProperties properties = new JteProperties();
        properties.setTemplateSuffix(".jte");
        return new JteViewResolver(engine, properties);
    }
}
