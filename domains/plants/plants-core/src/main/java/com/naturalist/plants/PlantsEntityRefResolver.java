package com.naturalist.plants;

import com.naturalist.catalog.DomainId;
import com.naturalist.catalog.EntityRef;
import com.naturalist.ddd.EntityName;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.library.EntityRefResolver;
import com.naturalist.taxonomy.LinealRank;

/**
 * Production {@link EntityRefResolver} for plant-side citation subjects — the plants sibling of
 * {@code InsectsEntityRefResolver}. Registered as a {@code @DomainService} bean so the app can
 * composite it with the other subject domains' resolvers.
 *
 * <p>The rank discriminator lives in the concrete {@link PlantRankName} permit, so both directions
 * dispatch on it: {@link #rankOf} reads {@code rank()} off the typed name, {@link #resolve} rebuilds
 * the permit via {@link PlantRankName#of(String, LinealRank)}.
 *
 * <h2>Why the stored rank matters more here than for insects</h2>
 * Plants' catalog has genuinely carried the same slug at two ranks — a genus {@code citrus} and a
 * species {@code citrus} are different subjects. {@code EntityName} equality is class-qualified, so
 * the two never compare equal in memory, but the stored form is three flat slugs: dropping or
 * guessing {@code subject_rank} would silently alias them. Round-tripping a plant subject is
 * therefore only correct while the rank column is carried faithfully.
 *
 * <h2>Four ranks, no subspecies</h2>
 * {@link PlantRankName#of} admits ORDER, FAMILY, GENUS and SPECIES only — plants expresses
 * infraspecific detail as a {@code Cultivar}, a separate axis, not as a rank. A stored
 * {@code SUBSPECIES} row is therefore rejected here where the insects resolver would accept it.
 * That asymmetry is the domains' rank ladders differing, not a defect: see
 * {@code docs/plans/organism-domain-blueprint.md} section D.
 *
 * <p>Note that a {@code Cultivar} is consequently <em>not</em> addressable as a citation subject
 * through this resolver. Citing one would mean carrying the cultivar name as a fourth stored
 * component, which the {@code CitationAssociation} shape does not have.
 */
@DomainService
public final class PlantsEntityRefResolver implements EntityRefResolver {

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
