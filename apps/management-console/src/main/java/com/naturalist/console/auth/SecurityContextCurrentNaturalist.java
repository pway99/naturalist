package com.naturalist.console.auth;

import com.naturalist.naturalist.Naturalist;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.naturalist.NaturalistQuery;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Function;

/**
 * Reads the {@link NaturalistPrincipal} off the {@link SecurityContextHolder}.
 * One of the three sanctioned SecurityContext readers.
 */
@Component
class SecurityContextCurrentNaturalist implements CurrentNaturalist {

    private final Function<NaturalistName, Optional<Naturalist>> byName;

    @org.springframework.beans.factory.annotation.Autowired
    SecurityContextCurrentNaturalist(NaturalistQuery naturalistQuery) {
        this(naturalistQuery::getByName);
    }

    // Test-friendly constructor: inject the lookup directly.
    SecurityContextCurrentNaturalist(Function<NaturalistName, Optional<Naturalist>> byName) {
        this.byName = byName;
    }

    @Override
    public Optional<NaturalistName> name() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof NaturalistPrincipal p) {
            return Optional.of(p.naturalistName());
        }
        return Optional.empty();
    }

    @Override
    public Optional<Naturalist> naturalist() {
        return name().flatMap(byName);
    }
}
