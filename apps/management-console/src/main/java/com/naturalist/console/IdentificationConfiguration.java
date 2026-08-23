package com.naturalist.console;

import com.naturalist.authority.ExternalAuthority;
import com.naturalist.authority.eol.EolClientMock;
import com.naturalist.data.NaturalistDatabase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composition-root wiring for the pre-real-EOL identification path. Publishes the
 * mock external authority as the sole {@link ExternalAuthority} bean, backed by the
 * shared {@link NaturalistDatabase}. Replaced by the real EOL client adapter when
 * external-authority Phase 4 lands.
 */
@Configuration
class IdentificationConfiguration {
    @Bean
    ExternalAuthority eolAuthority(NaturalistDatabase naturalistDatabase) {
        return new EolClientMock(naturalistDatabase);
    }
}
