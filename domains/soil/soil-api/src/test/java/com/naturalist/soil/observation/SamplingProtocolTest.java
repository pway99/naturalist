package com.naturalist.soil.observation;

import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SamplingProtocolTest {

    private static final Observer observer = Observer.forClass(SamplingProtocolTest.class);

    /** The protocol intended for the August 2026 sampling: eight probe cores, mixed evenly. */
    @Test
    void evenCompositeOfEightCoresHasNoViolations() {
        var mo = observer.forMethod("evenCompositeOfEightCoresHasNoViolations");
        var protocol = new SamplingProtocol(8, SamplingProtocol.SamplingTool.SOIL_PROBE,
                SamplingProtocol.CompositingMethod.EVEN_COMPOSITE);

        InvariantObservation result = mo.observable(protocol, "protocol");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void singleCoreOfOneSubsampleHasNoViolations() {
        var mo = observer.forMethod("singleCoreOfOneSubsampleHasNoViolations");
        var protocol = new SamplingProtocol(1, SamplingProtocol.SamplingTool.AUGER,
                SamplingProtocol.CompositingMethod.SINGLE_CORE);

        InvariantObservation result = mo.observable(protocol, "protocol");

        assertThat(result.violations()).isEmpty();
    }

    /**
     * A single core made of eight subsamples is not a description of anything that happened. The
     * pair has to agree, or the protocol misreports how representative the sample is.
     */
    @Test
    void singleCoreWithSeveralSubsamplesIsRejected() {
        var mo = observer.forMethod("singleCoreWithSeveralSubsamplesIsRejected");
        var protocol = new SamplingProtocol(8, SamplingProtocol.SamplingTool.AUGER,
                SamplingProtocol.CompositingMethod.SINGLE_CORE);

        InvariantObservation result = mo.observable(protocol, "protocol");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactly(".protocol.singleCoreImpliesOneSubsample");
    }

    @Test
    void emptyProtocolProducesOneViolationPerField() {
        var mo = observer.forMethod("emptyProtocolProducesOneViolationPerField");
        var protocol = new SamplingProtocol(0, null, null);

        InvariantObservation result = mo.observable(protocol, "protocol");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(
                        ".protocol.subsampleCount",
                        ".protocol.tool",
                        ".protocol.compositingMethod");
    }
}
