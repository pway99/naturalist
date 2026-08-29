package com.naturalist.insects;

import com.naturalist.catalog.EntityRef;
import com.naturalist.ddd.EntityName;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.library.EntityRefResolver;
import com.naturalist.taxonomy.LinealRank;

/**
 * Production {@link EntityRefResolver} for insect-side citation subjects — the composition-root
 * equivalent of the reconstruction the library mock does inline (it may import insects; the
 * {@code library-repository-rdbms} adapter may not). Registered as a {@code @DomainService} bean so
 * the app auto-wires it into {@code CitationAssociationEntityRepositoryRdbms}.
 *
 * <p>Insects is the only domain that is a citation subject today; when a second one appears, this
 * grows into a per-domain fragment behind a composite (mirroring {@code EntityRefLinker}). The rank
 * discriminator lives in the concrete {@link InsectRankName} permit, so both directions dispatch on
 * it: {@link #rankOf} reads {@code rank()} off the typed name, {@link #resolve} rebuilds the permit
 * via {@link InsectRankName#of(String, LinealRank)}.
 */
@DomainService
public final class InsectsEntityRefResolver implements EntityRefResolver {

    private static final InsectsDomain INSECTS = new InsectsDomain();

    @Override
    public String rankOf(EntityRef subject) {
        EntityName name = subject.name();
        if (name instanceof InsectRankName rankName) {
            return rankName.rank().name();
        }
        throw new IllegalArgumentException(
                "Unsupported citation subject name type: " + (name == null ? "null" : name.getClass().getName()));
    }

    @Override
    public EntityRef resolve(String domain, String rank, String name) {
        if (!INSECTS.value().equals(domain)) {
            throw new IllegalArgumentException("Unknown citation subject domain: " + domain);
        }
        // InsectRankName is a sealed interface whose permits separately implement EntityName —
        // the conversion needs an explicit cast rather than a plain return.
        return new EntityRef(INSECTS, (EntityName) InsectRankName.of(name, LinealRank.valueOf(rank)));
    }
}
