package com.naturalist.soil;

import com.naturalist.ddd.AggregateRoot;
import com.naturalist.ddd.CatalogEntity;
import com.naturalist.observability.Constraints;
import com.naturalist.zone.ZoneName;
import com.naturalist.zone.subzone.SubZoneName;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * The root entity of the {@link SoilProfile} aggregate — the identity and spatial
 * association of a managed soil unit at Oak Vista.
 * <p>
 * A {@code SoilProfileInfo} anchors a soil profile to a specific spatial unit in the
 * zone domain. The anchoring is expressed as a {@link ZoneName} (always present) and
 * an optional {@link SubZoneName} (present when the profile covers a sub-zone subdivision
 * rather than an entire Zone).
 * <p>
 * <b>Spatial association rules:</b>
 * <ul>
 *   <li>If {@code subZoneName} is {@code null}, the profile covers the entire Zone
 *       identified by {@code zoneName} — e.g. Garden Box 1 before SubZones were defined.</li>
 *   <li>If {@code subZoneName} is set, the profile covers a specific SubZone within
 *       the Zone — e.g. the backyard garden north section after the April 7, 2026
 *       TSWV management event introduced sub-zone granularity.</li>
 * </ul>
 * <p>
 * <b>Oak Vista soil profiles:</b>
 * <ul>
 *   <li>{@code "box1"} — Garden Box 1, zone-level (no SubZones). Raised bed, worm
 *       casting blend, FGL analysed March 3, 2026 (CH 2671853-001). Biological
 *       amplification factor 1.8 for nitrogen mineralisation.</li>
 *   <li>{@code "backyard-north"} — Backyard garden north section (SubZone). Native
 *       clay amended. FGL analysed March 3, 2026 (CH 2671853-002). TSWV/thrips
 *       outbreak April 7, 2026. Clay sublayer restricts drainage — post-rototill
 *       recovery 14–28 days.</li>
 *   <li>{@code "backyard-center"} — Backyard garden center (SubZone). Nick's Italian
 *       Pear primary planting. Same substrate and lab analysis as north.</li>
 *   <li>{@code "backyard-south"} — Backyard garden south section (SubZone). Amish
 *       Paste starts installed April 6, 2026. Same substrate as north.</li>
 * </ul>
 * <p>
 * The reference from SubZone to SoilProfile is stored as {@link SoilProfileName}
 * on {@code SubZone.soilProfileName} — a soft reference maintained by the application
 * layer, not a compile-time import from zone-api into soil-api.
 */
@AggregateRoot
public record SoilProfileInfo(
        SoilProfileId id,
        SoilProfileName name,
        ZoneName zoneName,
        @Nullable SubZoneName subZoneName
) implements CatalogEntity<SoilProfileId, SoilProfileName> {

    @Override
    public SoilProfileInfo withId(SoilProfileId id) {
        return new SoilProfileInfo(id, name, zoneName, subZoneName);
    }

    /**
     * Whether this soil profile covers a sub-zone subdivision rather than an entire Zone.
     *
     * @return {@code true} if {@code subZoneName} is set
     */
    public boolean isSubZoneScoped() {
        return subZoneName != null;
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityId(id, "id")
                .entityName(name, "name")
                .notNull(this, SoilProfileInfo::zoneName, "zoneName");
    }
}
