package com.naturalist.account;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecureTokensTest {

    private final SecureTokens tokens = SecureTokens.jdk();

    @Test
    void mint_returnsRawValueAndItsHash() {
        SecureTokens.MintedToken minted = tokens.mint();

        assertThat(minted.rawValue()).isNotBlank();
        assertThat(minted.hash()).isEqualTo(tokens.hash(minted.rawValue()));
    }

    @Test
    void mint_producesDistinctSecrets() {
        assertThat(tokens.mint().rawValue()).isNotEqualTo(tokens.mint().rawValue());
    }

    @Test
    void hash_isDeterministic() {
        assertThat(tokens.hash("a-raw-token")).isEqualTo(tokens.hash("a-raw-token"));
    }

    @Test
    void hash_differsForDifferentInput() {
        assertThat(tokens.hash("a-raw-token")).isNotEqualTo(tokens.hash("another-token"));
    }
}
