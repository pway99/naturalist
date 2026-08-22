package com.naturalist.data.count;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class SelectGateTest {

    private static AllowRepeatedSelect allow(String query, String select) {
        return new AllowRepeatedSelect() {
            @Override public Class<? extends Annotation> annotationType() { return AllowRepeatedSelect.class; }
            @Override public String query() { return query; }
            @Override public String select() { return select; }
        };
    }

    @Test
    void repeatedSelect_throwsNamingHeadAndSelectAndCount() {
        Map<String, Map<String, Integer>> snapshot =
                Map.of("InsectImageQueryImpl.forRankHierarchy", Map.of("InsectImageRepositoryMock.getByName", 14));

        assertThatExceptionOfType(RepeatedSelectException.class)
                .isThrownBy(() -> SelectGate.evaluate(snapshot, List.of()))
                .withMessageContaining("InsectImageQueryImpl.forRankHierarchy")
                .withMessageContaining("InsectImageRepositoryMock.getByName")
                .withMessageContaining("14");
    }

    @Test
    void singleSelect_passes() {
        Map<String, Map<String, Integer>> snapshot =
                Map.of("FooQueryImpl.loadOne", Map.of("FooRepositoryMock.getByName", 1));
        assertThatCode(() -> SelectGate.evaluate(snapshot, List.of())).doesNotThrowAnyException();
    }

    @Test
    void repeatedSelect_isSuppressedByMatchingAllowlistEntry() {
        Map<String, Map<String, Integer>> snapshot =
                Map.of("FooQueryImpl.loadAll", Map.of("FooRepositoryMock.getByName", 3));
        assertThatCode(() ->
                SelectGate.evaluate(snapshot, List.of(allow("FooQueryImpl.loadAll", "getByName"))))
                .doesNotThrowAnyException();
    }

    @Test
    void allowlistEntryForADifferentSelect_doesNotSuppress() {
        Map<String, Map<String, Integer>> snapshot =
                Map.of("FooQueryImpl.loadAll", Map.of("FooRepositoryMock.getByName", 3));
        assertThatExceptionOfType(RepeatedSelectException.class)
                .isThrownBy(() ->
                        SelectGate.evaluate(snapshot, List.of(allow("FooQueryImpl.loadAll", "getPage"))));
    }
}
