package com.naturalist.zone.subzone;

import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.function.Consumer;

/**
 * An immutable record of observed pest or disease pressure in a SubZone during a season.
 * <p>
 * {@code PestPressureRecord} is an append-only historical fact. Once recorded it is never
 * modified — a resolution is recorded by creating a new record referencing the same
 * pathogen with a {@code resolvedDate}. This preserves the full timeline of pressure events.
 * <p>
 * First instance in the domain: April 7, 2026 — TSWV (SEVERE) and THRIPS (HIGH) pressure
 * recorded in the north section of the Oak Vista backyard garden row following confirmed
 * outbreak diagnosis.
 */
public record PestPressureRecord(
        Pathogen pathogen,
        PestSeverity severity,
        LocalDate detectedDate,
        @Nullable LocalDate resolvedDate,
        int season,
        @Nullable String notes
) implements ValueObject {

    /**
     * Whether this pressure record has been resolved.
     *
     * @return {@code true} if a {@code resolvedDate} has been recorded
     */
    public boolean resolved() {
        return resolvedDate != null;
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .notNull(this, PestPressureRecord::pathogen, "pathogen")
                .notNull(this, PestPressureRecord::severity, "severity")
                .notNull(this, PestPressureRecord::detectedDate, "detectedDate");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Pathogen — the organism or condition causing pressure
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * The pest or pathogen responsible for observed pressure in a SubZone.
     */
    public enum Pathogen {
        /** Tomato Spotted Wilt Virus — transmitted by western flower thrips. No cure. */
        TSWV,
        /** Western flower thrips — {@code Frankliniella occidentalis}. Primary TSWV vector. */
        THRIPS,
        /** Early blight — {@code Alternaria solani}. */
        EARLY_BLIGHT,
        /** Late blight — {@code Phytophthora infestans}. */
        LATE_BLIGHT,
        /** Fusarium crown and root rot — soil-persistent. Multi-season rotation required. */
        FUSARIUM_WILT,
        /** Broad mite — {@code Polyphagotarsonemus latus}. */
        BROAD_MITE,
        /** Twospotted spider mite — {@code Tetranychus urticae}. */
        SPIDER_MITE,
        /** Aphid species — multiple genera. */
        APHID,
        /** Cutworm — larvae of various noctuid moths. */
        CUTWORM,
        /**
         * Small hive beetle — {@code Aethina tumida}.
         * <p>
         * <b>Critical species distinction:</b> only {@code Heterorhabditis indica} nematodes
         * are effective against SHB soil pupae. {@code Steinernema feltiae} does NOT control SHB.
         */
        SMALL_HIVE_BEETLE
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PestSeverity — intensity of observed pressure
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * The intensity of pest or disease pressure observed at time of detection.
     */
    public enum PestSeverity {
        TRACE, LOW, MODERATE, HIGH,
        /**
         * Complete loss of sub-zone — all plants symptomatic or removed.
         * Applied to the north section of the Oak Vista backyard bed, April 7, 2026.
         */
        SEVERE
    }
}
