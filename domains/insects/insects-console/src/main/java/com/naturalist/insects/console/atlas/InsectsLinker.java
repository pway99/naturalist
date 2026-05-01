package com.naturalist.insects.console.atlas;

import com.naturalist.atlas.EntityRef;
import com.naturalist.atlas.EntityRefLinker;
import com.naturalist.insects.InsectSpeciesName;
import org.springframework.stereotype.Component;

/**
 * Insects-domain {@link EntityRefLinker}: maps insects-owned
 * {@code EntityName} types to detail-page URLs. The single place to look
 * when adding a new insects entity or moving an existing one.
 */
@Component
public class InsectsLinker implements EntityRefLinker {

    @Override
    public String linkFor(EntityRef ref) {
        return switch (ref.name()) {
            case InsectSpeciesName n -> "/insects/" + n.value();
            default -> null;
        };
    }
}
