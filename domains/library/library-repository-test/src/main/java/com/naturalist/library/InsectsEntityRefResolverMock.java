package com.naturalist.library;

import com.naturalist.catalog.DomainId;
import com.naturalist.catalog.EntityRef;
import com.naturalist.ddd.EntityName;
import com.naturalist.insects.InsectRankName;
import com.naturalist.insects.InsectsDomain;
import com.naturalist.taxonomy.LinealRank;

/**
 * In-memory {@link EntityRefResolver} for insect-side citation subjects — the
 * mock-stack counterpart of {@code InsectsEntityRefResolver} in
 * {@code insects-core}.
 *
 * <h2>Why two implementations exist</h2>
 * The DAG puts them out of each other's reach. The production resolver is a
 * {@code @DomainService} in {@code insects-core}, and rule 2 confines a core to
 * another domain's <em>api</em> — so {@code library-repository-test} cannot
 * import it. This one is the api-only equivalent: {@code InsectRankName} comes
 * from {@code identifiers}, and {@code InsectsDomain} from {@code insects-api}.
 *
 * <h2>Why the insects-api dependency stays</h2>
 * It is irreducible while {@code DomainId} remains an open interface with no
 * generic implementation — by design: "the kernel knows the names of no
 * domains." Rebuilding a typed {@link EntityRef} has to name a concrete
 * {@code DomainId}, and the only one for insect subjects lives in
 * {@code insects-api}.
 *
 * <p>What moving behind the port bought is confinement, not removal: that
 * knowledge now sits in this one class instead of inline in
 * {@link CitationAssociationJson}, where it had made library's catalog file
 * insects-only. A second subject domain becomes a sibling resolver (or a
 * composite over several), with no edit to library's serialization.
 */
final class InsectsEntityRefResolverMock implements EntityRefResolver {

    private static final InsectsDomain INSECTS = new InsectsDomain();

    @Override
    public DomainId domain() {
        return INSECTS;
    }

    @Override
    public String rankOf(EntityRef subject) {
        EntityName name = subject.name();
        if (name instanceof InsectRankName rankName) {
            return rankName.rank().name();
        }
        throw new IllegalArgumentException(
                "Unsupported citation subject name type: "
                        + (name == null ? "null" : name.getClass().getName()));
    }

    @Override
    public EntityRef resolve(String domain, String rank, String name) {
        if (!INSECTS.value().equals(domain)) {
            throw new IllegalArgumentException("Unknown citation subject domain: " + domain);
        }
        // InsectRankName is a sealed interface whose permits separately implement
        // EntityName - the conversion needs an explicit cast rather than a plain return.
        return new EntityRef(INSECTS, (EntityName) InsectRankName.of(name, LinealRank.valueOf(rank)));
    }
}
