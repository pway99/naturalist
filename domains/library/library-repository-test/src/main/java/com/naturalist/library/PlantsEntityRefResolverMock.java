package com.naturalist.library;

import com.naturalist.catalog.DomainId;
import com.naturalist.catalog.EntityRef;
import com.naturalist.ddd.EntityName;
import com.naturalist.plants.PlantRankName;
import com.naturalist.plants.PlantsDomain;
import com.naturalist.taxonomy.LinealRank;

/**
 * In-memory {@link EntityRefResolver} for plant-side citation subjects — the mock-stack
 * counterpart of {@code PlantsEntityRefResolver} in {@code plants-core}, and the plants sibling
 * of {@link InsectsEntityRefResolverMock}.
 *
 * <p>Same reason for existing as the insects mock: the production resolver is a
 * {@code @DomainService} in a {@code *-core}, which DAG rule 2 puts out of this module's reach,
 * so the mock stack ships an api-only equivalent. {@code PlantRankName} comes from
 * {@code identifiers}; only {@code PlantsDomain} needs {@code plants-api}.
 *
 * <p>Adding this is what the {@link EntityRefResolver#domain()} discriminator was for: two
 * resolvers now exist, and {@link CompositeEntityRefResolverMock} dispatches between them on the
 * domain slug rather than trying each and catching the rejection.
 *
 * <h2>Rejects SUBSPECIES, unlike the insects mock</h2>
 * {@code PlantRankName.of} admits ORDER, FAMILY, GENUS and SPECIES only — plants carries
 * infraspecific detail as a {@code Cultivar}, a separate axis. A stored {@code SUBSPECIES} row is
 * therefore an {@code IllegalArgumentException} here where the insects mock would accept it. The
 * domains' rank ladders genuinely differ; this is not an omission.
 */
final class PlantsEntityRefResolverMock implements EntityRefResolver {

    private static final PlantsDomain PLANTS = new PlantsDomain();

    @Override
    public DomainId domain() {
        return PLANTS;
    }

    @Override
    public String rankOf(EntityRef subject) {
        EntityName name = subject.name();
        if (name instanceof PlantRankName rankName) {
            return rankName.rank().name();
        }
        throw new IllegalArgumentException(
                "Unsupported citation subject name type: "
                        + (name == null ? "null" : name.getClass().getName()));
    }

    @Override
    public EntityRef resolve(String domain, String rank, String name) {
        if (!PLANTS.value().equals(domain)) {
            throw new IllegalArgumentException("Unknown citation subject domain: " + domain);
        }
        // PlantRankName is a sealed interface whose permits separately implement EntityName —
        // the conversion needs an explicit cast rather than a plain return.
        return new EntityRef(PLANTS, (EntityName) PlantRankName.of(name, LinealRank.valueOf(rank)));
    }
}
