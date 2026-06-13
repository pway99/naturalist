package com.naturalist.authority.eol;

import com.naturalist.authority.AuthorityReference;
import com.naturalist.authority.AuthoritySource;
import com.naturalist.authority.CitationName;
import com.naturalist.authority.OnlineSource;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.time.Instant;

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

    public static OnlineSource citation(
            CitationName name,
            EolPageId pageId,
            String title,
            @Nullable String author,
            @Nullable Integer year,
            @Nullable Instant lastModified) {
        return new OnlineSource(
                name,
                new AuthorityReference(SOURCE, deepLink(pageId)),
                title, author, year, lastModified);
    }

    private Eol() {
    }
}