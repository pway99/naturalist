package com.naturalist.fieldnotes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class CommonNameTest {

    private static final Observer observer = Observer.forClass(CommonNameTest.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    @Test
    void wellFormedNamePassesInvariants() {
        CommonName name = CommonName.of("pipevine swallowtail");

        InvariantObservation result = observer.forMethod("wellFormedNamePassesInvariants")
                .observable(name, "name");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void wellFormedNameWithExplicitLocalePassesInvariants() {
        CommonName name = CommonName.of("borraja", Locale.forLanguageTag("es"));

        InvariantObservation result = observer.forMethod("wellFormedNameWithExplicitLocalePassesInvariants")
                .observable(name, "name");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void singleArgFactoryDefaultsToEnglish() {
        CommonName name = CommonName.of("pipevine swallowtail");

        assertThat(name.locale()).isEqualTo(Locale.ENGLISH);
    }

    @Test
    void nullLabelViolatesInvariants() {
        CommonName name = new CommonName(null, Locale.ENGLISH);

        InvariantObservation result = observer.forMethod("nullLabelViolatesInvariants")
                .observable(name, "name");

        assertThat(result.violationNames())
                .anyMatch(n -> n.endsWith(".label"));
    }

    @Test
    void blankLabelViolatesInvariants() {
        CommonName name = new CommonName("   ", Locale.ENGLISH);

        InvariantObservation result = observer.forMethod("blankLabelViolatesInvariants")
                .observable(name, "name");

        assertThat(result.violationNames())
                .anyMatch(n -> n.endsWith(".label"));
    }

    @Test
    void emptyLabelViolatesInvariants() {
        CommonName name = new CommonName("", Locale.ENGLISH);

        InvariantObservation result = observer.forMethod("emptyLabelViolatesInvariants")
                .observable(name, "name");

        assertThat(result.violationNames())
                .anyMatch(n -> n.endsWith(".label"));
    }

    @Test
    void nullLocaleViolatesInvariants() {
        CommonName name = new CommonName("pipevine", null);

        InvariantObservation result = observer.forMethod("nullLocaleViolatesInvariants")
                .observable(name, "name");

        assertThat(result.violationNames())
                .anyMatch(n -> n.endsWith(".locale"));
    }

    @Test
    void labelDoesNotTrimAuthoredWhitespaceWithinValidLabels() {
        CommonName name = CommonName.of("  pipevine swallowtail  ");

        assertThat(name.label()).isEqualTo("  pipevine swallowtail  ");
    }

    @Test
    void valueEqualityIsByContent() {
        CommonName a = CommonName.of("pipevine");
        CommonName b = CommonName.of("pipevine");
        CommonName c = CommonName.of("pipevine", Locale.forLanguageTag("en-GB"));
        CommonName d = CommonName.of("clover");

        assertThat(a).isEqualTo(b);
        assertThat(a).isNotEqualTo(c);
        assertThat(a).isNotEqualTo(d);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void roundTripsThroughJsonAsObject() throws Exception {
        CommonName original = CommonName.of("California Dutchman's pipe", Locale.ENGLISH);

        String json = mapper.writeValueAsString(original);
        CommonName parsed = mapper.readValue(json, CommonName.class);

        assertThat(parsed).isEqualTo(original);
    }

    @Test
    void deserializesBcp47LocaleTag() throws Exception {
        CommonName parsed = mapper.readValue(
                "{\"label\":\"borraja\",\"locale\":\"es-MX\"}", CommonName.class);

        assertThat(parsed.label()).isEqualTo("borraja");
        assertThat(parsed.locale()).isEqualTo(Locale.forLanguageTag("es-MX"));
    }
}
