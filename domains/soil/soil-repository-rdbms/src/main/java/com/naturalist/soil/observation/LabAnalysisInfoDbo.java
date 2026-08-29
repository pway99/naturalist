package com.naturalist.soil.observation;

import com.naturalist.garden.CropTypeName;
import com.naturalist.measurements.DepthInches;
import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;
import com.naturalist.soil.SoilProfileName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Flat persistence view of {@link LabAnalysisInfo} — a surrogate-UUID {@code Entity}. Its uuid
 * {@code id} travels as text with a {@code ::uuid} cast; {@code sampleDate} uses MyBatis's built-in
 * {@code LocalDate} handler. Both cross-entity references are SOFT slugs with no FK:
 * {@code soilProfileName} is a plain slug column, matching the domain's own no-FK contract.
 * <p>
 * The nullable {@link SamplingProtocol} value object flattens onto three columns
 * ({@code sampling_subsample_count}, {@code sampling_tool}, {@code sampling_compositing_method}),
 * present exactly when {@code sampling_tool} is non-null. {@code sampleDepth} is a nullable
 * {@link DepthInches} stored as its raw {@code BigDecimal}, round-tripping at its exact scale.
 */
@DboSchema(table = "lab_analysis_info", primaryKey = "id", entity = LabAnalysisInfo.class)
final class LabAnalysisInfoDbo implements Dbo {
    String id;
    String soilProfileName;
    String cropType;
    LocalDate sampleDate;
    String labId;
    String labSampleId;
    BigDecimal sampleDepthInches;         // nullable
    String notes;                         // nullable
    // SamplingProtocol (nullable group: present iff samplingTool non-null)
    Integer samplingSubsampleCount;       // nullable
    String samplingTool;                  // nullable
    String samplingCompositingMethod;     // nullable

    static LabAnalysisInfoDbo from(LabAnalysisInfo a) {
        LabAnalysisInfoDbo d = new LabAnalysisInfoDbo();
        d.id = a.id().value().toString();
        d.soilProfileName = a.soilProfileName().value();
        d.cropType = a.cropType().value();
        d.sampleDate = a.sampleDate();
        d.labId = a.labId();
        d.labSampleId = a.labSampleId();
        d.sampleDepthInches = a.sampleDepth() == null ? null : a.sampleDepth().value();
        d.notes = a.notes();
        SamplingProtocol sp = a.samplingProtocol();
        if (sp != null) {
            d.samplingSubsampleCount = sp.subsampleCount();
            d.samplingTool = sp.tool().name();
            d.samplingCompositingMethod = sp.compositingMethod().name();
        }
        Observer.forClass(LabAnalysisInfoDbo.class)
                .arguments("from", i -> i.observable(d, "dbo"))
                .throwWhenInvalid();
        return d;
    }

    LabAnalysisInfo toEntity() {
        return new LabAnalysisInfo(
                LabAnalysisId.of(UUID.fromString(id)),
                SoilProfileName.of(soilProfileName),
                CropTypeName.of(cropType),
                sampleDate,
                labId,
                labSampleId,
                sampleDepthInches == null ? null : DepthInches.of(sampleDepthInches),
                samplingTool == null ? null : new SamplingProtocol(
                        samplingSubsampleCount,
                        SamplingProtocol.SamplingTool.valueOf(samplingTool),
                        SamplingProtocol.CompositingMethod.valueOf(samplingCompositingMethod)),
                notes);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .notBlank(id, "id")
                .notNull(soilProfileName, "soilProfileName").kebabFormat(soilProfileName, "soilProfileName")
                .maxLength(soilProfileName, 64, "soilProfileName")
                .notNull(cropType, "cropType").kebabFormat(cropType, "cropType").maxLength(cropType, 48, "cropType")
                .notNull(sampleDate, "sampleDate")
                .notBlank(labId, "labId")
                .notBlank(labSampleId, "labSampleId")
                .maxLength(samplingTool, 16, "samplingTool")
                .maxLength(samplingCompositingMethod, 16, "samplingCompositingMethod");
    }
}
