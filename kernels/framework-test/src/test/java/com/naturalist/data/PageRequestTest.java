package com.naturalist.data;

import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;

import static com.naturalist.data.PageRequest.DEFAULT_CONSOLE_PAGE_SIZE;
import static com.naturalist.data.PageRequest.DEFAULT_LOOKAHEAD;
import static com.naturalist.data.PageRequest.MAX_LOOKAHEAD;
import static com.naturalist.data.PageRequest.MAX_PAGE_SIZE;
import static org.assertj.core.api.Assertions.assertThat;

class PageRequestTest {

    private static final Observer observer = Observer.forClass(PageRequestTest.class);

    @Test
    void wellFormedRequestPassesInvariants() {
        InvariantObservation result = observer.forMethod("wellFormedRequestPassesInvariants")
                .observable(PageRequest.of(2, 25, 5), "request");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void zeroLookaheadIsValid() {
        InvariantObservation result = observer.forMethod("zeroLookaheadIsValid")
                .observable(PageRequest.of(0, 25), "request");

        assertThat(result.violations()).isEmpty();
        assertThat(PageRequest.of(0, 25).lookahead()).isZero();
    }

    @Test
    void negativePageNumberViolatesInvariants() {
        InvariantObservation result = observer.forMethod("negativePageNumberViolatesInvariants")
                .observable(new PageRequest(-1, 25, 0), "request");

        assertThat(result.violationNames())
                .anyMatch(n -> n.endsWith(".pageNumber"));
    }

    @Test
    void zeroPageSizeViolatesInvariants() {
        InvariantObservation result = observer.forMethod("zeroPageSizeViolatesInvariants")
                .observable(new PageRequest(0, 0, 0), "request");

        assertThat(result.violationNames())
                .anyMatch(n -> n.endsWith(".pageSize"));
    }

    @Test
    void oversizedPageSizeViolatesInvariants() {
        InvariantObservation result = observer.forMethod("oversizedPageSizeViolatesInvariants")
                .observable(new PageRequest(0, MAX_PAGE_SIZE + 1, 0), "request");

        assertThat(result.violationNames())
                .anyMatch(n -> n.endsWith(".pageSize"));
    }

    @Test
    void negativeLookaheadViolatesInvariants() {
        InvariantObservation result = observer.forMethod("negativeLookaheadViolatesInvariants")
                .observable(new PageRequest(0, 25, -1), "request");

        assertThat(result.violationNames())
                .anyMatch(n -> n.endsWith(".lookahead"));
    }

    @Test
    void oversizedLookaheadViolatesInvariants() {
        InvariantObservation result = observer.forMethod("oversizedLookaheadViolatesInvariants")
                .observable(new PageRequest(0, 25, MAX_LOOKAHEAD + 1), "request");

        assertThat(result.violationNames())
                .anyMatch(n -> n.endsWith(".lookahead"));
    }

    @Test
    void offsetIsPageNumberTimesPageSize() {
        assertThat(PageRequest.of(0, 25).offset()).isZero();
        assertThat(PageRequest.of(3, 25).offset()).isEqualTo(75);
        assertThat(PageRequest.of(7, 100).offset()).isEqualTo(700);
    }

    @Test
    void twoArgFactoryDefaultsLookaheadToZero() {
        PageRequest request = PageRequest.of(4, 50);

        assertThat(request.pageNumber()).isEqualTo(4);
        assertThat(request.pageSize()).isEqualTo(50);
        assertThat(request.lookahead()).isZero();
    }

    @Test
    void firstFactoryReturnsPageZeroWithNoLookahead() {
        PageRequest request = PageRequest.first(100);

        assertThat(request.pageNumber()).isZero();
        assertThat(request.pageSize()).isEqualTo(100);
        assertThat(request.lookahead()).isZero();
    }

    @Test
    void consoleFactoryUsesDefaultsAndLookahead() {
        PageRequest request = PageRequest.console(2);

        assertThat(request.pageNumber()).isEqualTo(2);
        assertThat(request.pageSize()).isEqualTo(DEFAULT_CONSOLE_PAGE_SIZE);
        assertThat(request.lookahead()).isEqualTo(DEFAULT_LOOKAHEAD);
    }
}
