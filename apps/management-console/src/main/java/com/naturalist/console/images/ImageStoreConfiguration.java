package com.naturalist.console.images;

import com.naturalist.observation.ImageStore;
import com.naturalist.observation.filesystem.FilesystemImageStore;
import com.naturalist.resilience.Resilience;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

/**
 * Provides the single {@link ImageStore} bean: a {@link FilesystemImageStore} rooted at
 * {@code naturalist.images.root} and guarded by the {@code image.store} resilience strategy
 * registered in {@link com.naturalist.console.resilience.ResilienceConfiguration}.
 */
@Configuration
@EnableConfigurationProperties(ImageStoreProperties.class)
class ImageStoreConfiguration {

    @Bean
    ImageStore imageStore(ImageStoreProperties properties, Resilience resilience) {
        return new FilesystemImageStore(Path.of(properties.root()), resilience);
    }
}
