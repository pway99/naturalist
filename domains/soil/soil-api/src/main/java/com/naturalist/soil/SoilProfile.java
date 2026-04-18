package com.naturalist.soil;

import com.naturalist.ddd.Aggregate;
import com.naturalist.observability.Constraints;
import com.naturalist.soil.event.AmendmentEvent;
import com.naturalist.soil.event.IrrigationEvent;
import com.naturalist.soil.event.PrecipitationEvent;
import com.naturalist.soil.event.TillageEvent;
import com.naturalist.soil.observation.LabAnalysis;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Aggregate root of the Soil bounded context.
 * <p>
 * A {@code SoilProfile} represents the complete, observable soil state of a managed
 * spatial unit at Oak Vista — its laboratory-measured chemistry, surface management
 * history, and all physical interventions that affect soil biological structure and
 * nutrient availability.
 * <p>
 * <b>Aggregate boundary:</b> SoilProfile owns its {@link SoilProfileInfo} root entity,
 * the lab analysis history ({@link LabAnalysis} list), the current surface layer
 * ({@link MulchLayer}), and the four event histories: amendments, irrigation, tillage,
 * and precipitation. Sensor readings are managed by the sensors domain, which depends
 * on soil-api — soil-api does not depend on sensors-api.
 * External modules reference soil data only through SoilProfile's curated public API.
 * <p>
 * <b>Relationship to Zone:</b> A SoilProfile is anchored to a Zone or SubZone by name
 * ({@link SoilProfileInfo#zoneName()} / {@link SoilProfileInfo#subZoneName()}).
 * The zone domain holds a reverse soft reference via {@code SubZone.soilProfileName}.
 * Neither module has a compile-time dependency on the other's entities — the
 * association is maintained at the application layer. This preserves the DAG
 * direction: soil-api → zone-api (valid); zone-api has no compile-time import of
 * soil-api (no cycle).
 * <p>
 * <b>Oak Vista soil profiles (April 2026):</b>
 * <ul>
 *   <li><b>box1</b> — Garden Box 1, zone-scoped. Worm casting raised bed, 48 sqft.
 *       FGL CH 2671853-001: N VERY_LOW, P VERY_HIGH, Ca-Sol VERY_LOW, Sulfate VERY_LOW,
 *       Boron VERY_LOW, pH 7.2. Active concern: nitrogen over-application (blood meal,
 *       April 2026). Pending: gypsum + boron per FGL recommendations.</li>
 *   <li><b>backyard-north</b> — Backyard north section (SubZone), ~33 sqft.
 *       Native clay amended, clay sublayer. FGL CH 2671853-002: similar deficiency
 *       pattern, limestone 2.9% (higher than Box 1 — more CaCO₃ for Thiobacillus
 *       conversion). Rototilled April 2, 2026 (6 inches). TSWV/thrips outbreak
 *       April 7, 2026 — all San Marzano starts removed, straw thinned, neem/soap
 *       treatment applied. WH51 sensor relocated ~April 9 after air gap formation.</li>
 *   <li><b>backyard-center</b> — Backyard center (SubZone), ~33 sqft. Nick's Italian
 *       Pear. Same substrate and FGL analysis as north. Straw retained at ~2 inches.</li>
 *   <li><b>backyard-south</b> — Backyard south (SubZone), ~33 sqft. Amish Paste
 *       transplants installed April 6, 2026. Same substrate as north.</li>
 * </ul>
 */
public record SoilProfile(
        SoilProfileInfo soilProfileInfo,
        List<LabAnalysis> labAnalyses,
        @Nullable MulchLayer currentMulchLayer,
        List<AmendmentEvent> amendmentEvents,
        List<IrrigationEvent> irrigationEvents,
        List<TillageEvent> tillageEvents,
        List<PrecipitationEvent> precipitationEvents
) implements Aggregate {

    // ── With methods ──────────────────────────────────────────────────────────

    public SoilProfile withSoilProfileInfo(SoilProfileInfo soilProfileInfo) {
        return new SoilProfile(soilProfileInfo, labAnalyses, currentMulchLayer,
                amendmentEvents, irrigationEvents, tillageEvents, precipitationEvents);
    }

    public SoilProfile withLabAnalyses(List<LabAnalysis> labAnalyses) {
        return new SoilProfile(soilProfileInfo, labAnalyses, currentMulchLayer,
                amendmentEvents, irrigationEvents, tillageEvents, precipitationEvents);
    }

    public SoilProfile withCurrentMulchLayer(@Nullable MulchLayer currentMulchLayer) {
        return new SoilProfile(soilProfileInfo, labAnalyses, currentMulchLayer,
                amendmentEvents, irrigationEvents, tillageEvents, precipitationEvents);
    }

    public SoilProfile withAmendmentEvents(List<AmendmentEvent> amendmentEvents) {
        return new SoilProfile(soilProfileInfo, labAnalyses, currentMulchLayer,
                amendmentEvents, irrigationEvents, tillageEvents, precipitationEvents);
    }

    public SoilProfile withIrrigationEvents(List<IrrigationEvent> irrigationEvents) {
        return new SoilProfile(soilProfileInfo, labAnalyses, currentMulchLayer,
                amendmentEvents, irrigationEvents, tillageEvents, precipitationEvents);
    }

    public SoilProfile withTillageEvents(List<TillageEvent> tillageEvents) {
        return new SoilProfile(soilProfileInfo, labAnalyses, currentMulchLayer,
                amendmentEvents, irrigationEvents, tillageEvents, precipitationEvents);
    }

    public SoilProfile withPrecipitationEvents(List<PrecipitationEvent> precipitationEvents) {
        return new SoilProfile(soilProfileInfo, labAnalyses, currentMulchLayer,
                amendmentEvents, irrigationEvents, tillageEvents, precipitationEvents);
    }

    // ── Identity delegates ────────────────────────────────────────────────────

    /**
     * The natural key of this SoilProfile.
     *
     * @return the SoilProfileName slug
     */
    public SoilProfileName soilProfileName() {
        return soilProfileInfo.name();
    }

    // ── Observation queries ───────────────────────────────────────────────────

    /**
     * The most recent lab analysis for this soil profile, if any has been recorded.
     *
     * @return the most recent {@link LabAnalysis}, or empty if no analyses exist
     */
    public Optional<LabAnalysis> latestLabAnalysis() {
        if (labAnalyses.isEmpty()) return Optional.empty();
        return Optional.of(labAnalyses.get(labAnalyses.size() - 1));
    }

    /**
     * Whether any lab analysis on record indicates a BER (blossom end rot) risk.
     * <p>
     * Uses the most recent analysis as the current indicator. BER risk is driven by
     * Ca-Sol and boron deficiency, both present at Oak Vista as of March 2026.
     *
     * @return {@code true} if the most recent analysis indicates BER risk
     */
    public boolean hasBerRisk() {
        return latestLabAnalysis().map(LabAnalysis::indicatesBerRisk).orElse(false);
    }

    /**
     * Whether any lab analysis on record shows limestone content above the 0.5%
     * threshold that makes Thiobacillus-mediated sulfur oxidation agronomically
     * meaningful.
     *
     * @return {@code true} if the most recent analysis has amenable limestone content
     */
    public boolean hasThiobacillusAmenableLimestone() {
        return latestLabAnalysis()
                .map(LabAnalysis::hasThiobacillusAmenableLimestone)
                .orElse(false);
    }

    /**
     * The most recent tillage event, if any has been recorded.
     * <p>
     * Used by the sensor analysis layer to contextualise drainage recovery readings —
     * a post-tillage reading that shows slow drainage is expected and should not
     * trigger a DISRUPTED soil health finding without accounting for the expected
     * recovery timeline.
     *
     * @return the most recent {@link TillageEvent}, or empty if none recorded
     */
    public Optional<TillageEvent> latestTillageEvent() {
        if (tillageEvents.isEmpty()) return Optional.empty();
        return Optional.of(tillageEvents.get(tillageEvents.size() - 1));
    }

    /**
     * The most recent precipitation event, if any has been recorded.
     *
     * @return the most recent {@link PrecipitationEvent}, or empty if none recorded
     */
    public Optional<PrecipitationEvent> latestPrecipitationEvent() {
        if (precipitationEvents.isEmpty()) return Optional.empty();
        return Optional.of(precipitationEvents.get(precipitationEvents.size() - 1));
    }

    // ── Aggregate mutations ───────────────────────────────────────────────────

    /**
     * Returns a new SoilProfile with the given lab analysis appended to the history.
     */
    public SoilProfile withAddedLabAnalysis(LabAnalysis analysis) {
        var updated = new ArrayList<>(labAnalyses);
        updated.add(analysis);
        return withLabAnalyses(List.copyOf(updated));
    }

    /**
     * Returns a new SoilProfile with the given amendment event appended to the history.
     */
    public SoilProfile withAddedAmendmentEvent(AmendmentEvent event) {
        var updated = new ArrayList<>(amendmentEvents);
        updated.add(event);
        return withAmendmentEvents(List.copyOf(updated));
    }

    /**
     * Returns a new SoilProfile with the given irrigation event appended to the history.
     */
    public SoilProfile withAddedIrrigationEvent(IrrigationEvent event) {
        var updated = new ArrayList<>(irrigationEvents);
        updated.add(event);
        return withIrrigationEvents(List.copyOf(updated));
    }

    /**
     * Returns a new SoilProfile with the given tillage event appended to the history.
     */
    public SoilProfile withAddedTillageEvent(TillageEvent event) {
        var updated = new ArrayList<>(tillageEvents);
        updated.add(event);
        return withTillageEvents(List.copyOf(updated));
    }

    /**
     * Returns a new SoilProfile with the given precipitation event appended to the history.
     */
    public SoilProfile withAddedPrecipitationEvent(PrecipitationEvent event) {
        var updated = new ArrayList<>(precipitationEvents);
        updated.add(event);
        return withPrecipitationEvents(List.copyOf(updated));
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entity(this, SoilProfile::soilProfileInfo, "soilProfileInfo")
                .notNull(this, SoilProfile::labAnalyses, "labAnalyses")
                .notNull(this, SoilProfile::amendmentEvents, "amendmentEvents")
                .notNull(this, SoilProfile::irrigationEvents, "irrigationEvents")
                .notNull(this, SoilProfile::tillageEvents, "tillageEvents")
                .notNull(this, SoilProfile::precipitationEvents, "precipitationEvents");
    }
}
