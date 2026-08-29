package com.naturalist.library;

import com.naturalist.authority.AuthorityReference;
import com.naturalist.authority.AuthoritySource;
import com.naturalist.authority.Citation;
import com.naturalist.authority.CitationName;
import com.naturalist.authority.OnlineSource;
import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;

import java.net.URI;
import java.time.Instant;
import java.util.function.Consumer;

/**
 * Flat persistence view of {@link Citation} — the sealed {@code NamedEntity} from
 * {@code kernels/authority}. Its single owned value object, the {@link AuthorityReference}, flattens
 * onto {@code authority_source_id} / {@code authority_source_display_name} / {@code authority_url};
 * the sealed type is discriminated by {@code kind} (the same {@code "ONLINE_SOURCE"} tag Jackson
 * uses) and reconstructed via {@link #toEntity()}'s exhaustive switch. The permit-specific fields
 * ({@code author}, {@code year}, {@code lastModified}) are nullable, present only on
 * {@link OnlineSource}. {@code lastModified} is an {@link Instant} → {@code TIMESTAMPTZ}
 * (instant-preserving via MyBatis's built-in handler). Fields are camelCase; MyBatis translates
 * snake_case across on read.
 */
@DboSchema(table = "citation", primaryKey = "id", unique = {"name"}, entity = Citation.class)
final class CitationDbo implements Dbo {
    Long id;              // numeric identity anchor (BIGINT IDENTITY); null before insert.
    String name;
    String kind;          // sealed-type discriminator; "ONLINE_SOURCE" today
    String title;
    String author;        // nullable — OnlineSource only
    Integer year;         // nullable — OnlineSource only
    Instant lastModified; // nullable — OnlineSource only
    String authoritySourceId;
    String authoritySourceDisplayName;
    String authorityUrl;

    static CitationDbo from(Citation c) {
        CitationDbo d = new CitationDbo();
        d.name = c.name().value();
        d.title = c.title();
        AuthorityReference ref = c.authorityReference();
        d.authoritySourceId = ref.source().id();
        d.authoritySourceDisplayName = ref.source().displayName();
        d.authorityUrl = ref.url() == null ? null : ref.url().toString();
        switch (c) {
            case OnlineSource os -> {
                d.kind = "ONLINE_SOURCE";
                d.author = os.author();
                d.year = os.year();
                d.lastModified = os.lastModified();
            }
        }
        Observer.forClass(CitationDbo.class)
                .arguments("from", i -> i.observable(d, "dbo"))
                .throwWhenInvalid();
        return d;
    }

    Citation toEntity() {
        AuthorityReference ref = new AuthorityReference(
                new AuthoritySource(authoritySourceId, authoritySourceDisplayName),
                URI.create(authorityUrl));
        return switch (kind) {
            case "ONLINE_SOURCE" -> new OnlineSource(
                    CitationName.of(name), ref, title, author, year, lastModified);
            default -> throw new IllegalStateException("Unknown citation kind: " + kind);
        };
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .notNull(name, "name").kebabFormat(name, "name").maxLength(name, 200, "name")
                .notBlank(kind, "kind").maxLength(kind, 32, "kind")
                .notBlank(title, "title")
                .notBlank(authoritySourceId, "authoritySourceId").maxLength(authoritySourceId, 64, "authoritySourceId")
                .notBlank(authoritySourceDisplayName, "authoritySourceDisplayName")
                    .maxLength(authoritySourceDisplayName, 128, "authoritySourceDisplayName")
                .notBlank(authorityUrl, "authorityUrl");
    }
}
