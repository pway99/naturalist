package com.naturalist.soil;

import com.naturalist.ddd.AggregateRoot;
import com.naturalist.ddd.NamedEntity;
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
 *       identified by {@code zoneName} — e.g. Garden Box 1, and the back yard bed.</li>
 *   <li>If {@code subZoneName} is set, the profile covers a specific SubZone within
 *       the Zone. Reserved for a sub-zone that is <em>separately sampled</em>; no
 *       Oak Vista profile is sub-zone scoped today. The front garden is five boxes in
 *       one zone and is the expected first user, once the fall 2026 samples establish
 *       which boxes differ enough to hold their own profile.</li>
 * </ul>
 * <p>
 * <b>The grain is the sampled soil unit, not the planted area.</b> A profile exists per
 * physical lab sample. The backyard north / center / south sub-zones are crop rows over one
 * bed and one composite sample, so they share the {@code "backyard"} profile rather than
 * holding three copies of the same chemistry; their {@code SubZone.soilProfileName} is null.
 * Split a profile out only when a sub-zone is sampled on its own.
 * <p>
 * <b>Oak Vista soil profiles:</b>
 * <ul>
 *   <li>{@code "box1"} — Garden Box 1, zone-level. Raised bed, worm casting blend, FGL
 *       analysed March 3, 2026 (CH 2671853-001). Biological amplification factor 1.8 for
 *       nitrogen mineralisation.</li>
 *   <li>{@code "backyard"} — the back yard bed, zone-level. Native clay amended. FGL
 *       analysed March 3, 2026 (CH 2671853-002). TSWV/thrips outbreak April 7, 2026 in the
 *       north rows. Clay sublayer restricts drainage — post-rototill recovery 14–28 days.</li>
 * </ul>
 * <p>
 * The reference from SubZone to SoilProfile is stored as {@link SoilProfileName}
 * on {@code SubZone.soilProfileName} — a soft reference maintained by the application
 * layer, not a compile-time import from zone-api into soil-api.
 */
@AggregateRoot
public record SoilProfileInfo(
        SoilProfileName name,
        ZoneName zoneName,
        @Nullable SubZoneName subZoneName
) implements NamedEntity<SoilProfileName> {

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
                .entityName(name, "name")
                .entityName(zoneName, "zoneName")
                .entityNameOrNull(subZoneName, "subZoneName");
    }
}
