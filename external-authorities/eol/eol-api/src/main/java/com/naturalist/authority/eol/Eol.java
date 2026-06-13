package com.naturalist.authority.eol;

import com.naturalist.authority.AuthoritySource;

import java.net.URI;

/**
 * The EOL provider contract shared by all EOL clients (mock + real):
 * the {@code eol} {@link AuthoritySource} and EOL's deep-link URL
 * pattern. EOL knowledge stays here, out of the kernel.
 */
public final class Eol {

    public static final AuthoritySource SOURCE =
            new AuthoritySource("eol", "Encyclopedia of Life");

    public static URI deepLink(EolPageId id) {
        return URI.create("https://eol.org/pages/" + id.value());
    }

    private Eol() {
    }
}