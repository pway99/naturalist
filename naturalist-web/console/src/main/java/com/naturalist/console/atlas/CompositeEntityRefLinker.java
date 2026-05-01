package com.naturalist.console.atlas;

import com.naturalist.atlas.EntityRef;
import com.naturalist.atlas.EntityRefLinker;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * App-shell composite that walks every per-domain {@link EntityRefLinker}
 * Spring discovered and returns the first non-null URL. Returns {@code null}
 * when no linker recognises the ref — consumers render that as "no link"
 * (the panel drops the entry, the renderer falls back to plain text).
 * <p>
 * The walking order is registration order, which Spring derives from
 * component scan order. No linker should overlap another's name types; if a
 * future cross-cutting URL ever needs to win over a domain-local one, that
 * will be a deliberate {@code @Order}-annotated decision, not an
 * accident-of-scan-order one.
 */
@Component
@Primary
public class CompositeEntityRefLinker implements EntityRefLinker {

    private final List<EntityRefLinker> delegates;

    CompositeEntityRefLinker(List<EntityRefLinker> delegates) {
        this.delegates = delegates.stream()
                .filter(l -> l != this)
                .toList();
    }

    @Override
    public String linkFor(EntityRef ref) {
        if (ref == null) {
            return null;
        }
        for (EntityRefLinker delegate : delegates) {
            String url = delegate.linkFor(ref);
            if (url != null) {
                return url;
            }
        }
        return null;
    }
}
