package com.naturalist.naturalist;

import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NaturalistCredentialDboTest {

    @Test void roundTrip_recoversNameAndHash() {
        NaturalistCredential c = new NaturalistCredential(
                NaturalistName.of("amir-hassan"), "{bcrypt}$2a$10$abc");
        NaturalistCredentialDbo dbo = NaturalistCredentialDbo.from(c);
        assertThat(dbo.toEntity()).usingRecursiveComparison().isEqualTo(c);
    }

    @Test void invariants_flagOverLongPasswordHash() {
        NaturalistCredential c = new NaturalistCredential(
                NaturalistName.of("amir-hassan"), "{bcrypt}$2a$10$abc");
        NaturalistCredentialDbo dbo = NaturalistCredentialDbo.from(c);
        dbo.passwordHash = "x".repeat(81); // exceeds VARCHAR(80)
        var violations = Observer.forClass(NaturalistCredentialDboTest.class)
                .arguments("t", i -> i.observable(dbo, "dbo")).violations();
        assertThat(violations).isNotEmpty();
    }
}
