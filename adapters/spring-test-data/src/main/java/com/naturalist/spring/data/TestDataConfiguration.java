package com.naturalist.spring.data;

import com.naturalist.data.NaturalistDatabase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring-side bridge that publishes a single shared
 * {@link NaturalistDatabase} as a bean. Bundled as an adapter so neither
 * {@code kernels/framework-test} nor any {@code <domain>-repository-test}
 * module gains a Spring dependency — only this module imports
 * {@code org.springframework.*}.
 *
 * <h2>Why a single shared bean</h2>
 * The pre-RDBMS data registry is intentionally process-wide: a plant
 * referencing a chemistry compound by slug, an inverse provider walking
 * a constituent index, and a repository contract test all need to see
 * the same lazy-instantiated {@code TestEntitySource} graph. Each
 * domain wiring its own database would fork the registry and break
 * cross-domain referential integrity.
 *
 * <h2>Test vs. production</h2>
 * This adapter ships on the classpath only while the app is pre-RDBMS.
 * When the production data adapter lands, this module is removed from
 * the app's dependency list (or excluded by profile). The class then
 * disappears from the classpath, classpath scanning never sees it, and
 * {@link NaturalistDatabase} simply does not exist as a bean — which is
 * exactly the contract a production deployment wants. Per-domain
 * {@code TestEntitySource} beans remain useful for repository contract
 * tests regardless of the production adapter, so they stay defined in
 * each domain's {@code *DataConfiguration}.
 */
@Configuration
public class TestDataConfiguration {

    @Bean
    NaturalistDatabase naturalistDatabase() {
        return NaturalistDatabase.create();
    }
}
