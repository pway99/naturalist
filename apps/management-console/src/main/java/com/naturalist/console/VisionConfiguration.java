package com.naturalist.console;

import com.naturalist.vision.NoOpVisionService;
import com.naturalist.vision.VisionService;
import com.naturalist.vision.anthropic.AnthropicVisionConfig;
import com.naturalist.vision.anthropic.AnthropicVisionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class VisionConfiguration {

    @Bean
    VisionService visionService() {
        var apiKey = System.getenv("ANTHROPIC_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            return new NoOpVisionService();
        }
        return new AnthropicVisionService(AnthropicVisionConfig.defaults());
    }
}
