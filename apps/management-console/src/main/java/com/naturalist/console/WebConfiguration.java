package com.naturalist.console;

import com.naturalist.console.auth.NaturalistHeaderInterceptor;
import com.naturalist.spring.NamedValueConversion;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
class WebConfiguration implements WebMvcConfigurer {

    private final NaturalistHeaderInterceptor headerInterceptor;

    WebConfiguration(NaturalistHeaderInterceptor headerInterceptor) {
        this.headerInterceptor = headerInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(headerInterceptor);
    }

    /** Path variables and request parameters read named values through the strategy JSON reads use. */
    @Override
    public void addFormatters(FormatterRegistry registry) {
        NamedValueConversion.register(registry);
    }
}
