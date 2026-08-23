package com.naturalist.library;

import org.springframework.web.util.HtmlUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Wraps occurrences of glossary terms in free text with an inline definition
 * popover — a presentation helper shared by every domain console that surfaces
 * naturalist vernacular (insect Field Marks, soil chemistry labels, …).
 *
 * <p>Each matched term renders as a {@code <button>} styled as a dotted-underline
 * link that toggles a native HTML {@code popover} holding the whole entry —
 * definition, the usage example when there is one, and an optional link through
 * to the glossary page. This is the same no-JavaScript mechanism as
 * {@code components/infoPopover.jte} (the breadcrumb "?" popovers) — the button
 * is required because {@code popovertarget} only works on buttons, and it keeps
 * the reader on the page instead of navigating away.
 *
 * <p>{@link #linkHtml(String, String)} always HTML-escapes its input, so the
 * result is safe to emit raw ({@code $unsafe}); the only markup it introduces is
 * the elements it controls. Matching is whole-word, case-insensitive, and exact
 * — "dorsum" matches but "dorsal" does not; longer terms win over the words
 * inside them ("field mark" over "mark"); and each term is linked at most once
 * per text.
 *
 * <p>Vocabulary comes from the library glossary via a console's existing
 * {@code LibraryTestContext}. Lives in {@code library-console} because the
 * library domain owns the glossary; consumers depend on this module for the
 * helper rather than duplicating it.
 */
public final class GlossaryLinker {

    private record Entry(String term, String slug, String definition, String example) {
    }

    private static final GlossaryLinker NONE = new GlossaryLinker(List.of());

    private final List<Entry> entries;
    private final Pattern pattern;

    private GlossaryLinker(List<Entry> entries) {
        this.entries = entries;
        this.pattern = entries.isEmpty() ? null : buildPattern(entries);
    }

    /** A linker that links nothing — still escapes, so it is a safe default. */
    public static GlossaryLinker none() {
        return NONE;
    }

    public static GlossaryLinker of(Iterable<GlossaryTerm> terms) {
        var list = new ArrayList<Entry>();
        for (var term : terms) {
            if (term.term() != null && !term.term().isBlank()) {
                var definition = term.definition() == null ? "" : term.definition();
                var example = term.example() == null ? "" : term.example();
                list.add(new Entry(term.term().strip(), term.name().value(), definition, example));
            }
        }
        // Longest term first so a multi-word term wins over the words inside it.
        list.sort(Comparator.comparingInt((Entry e) -> e.term().length()).reversed());
        return list.isEmpty() ? NONE : new GlossaryLinker(List.copyOf(list));
    }

    private static Pattern buildPattern(List<Entry> entries) {
        var alternation = new StringBuilder();
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) {
                alternation.append('|');
            }
            alternation.append(Pattern.quote(entries.get(i).term()));
        }
        return Pattern.compile("\\b(?:" + alternation + ")\\b", Pattern.CASE_INSENSITIVE);
    }

    /**
     * Returns {@code text} HTML-escaped, with the first occurrence of each known
     * glossary term wrapped in a definition popover. {@code idSeed} must be
     * unique per text block on the page (e.g. rank slug + mark index) so the
     * generated popover ids do not collide.
     */
    public String linkHtml(String text, String idSeed) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        if (pattern == null) {
            return HtmlUtils.htmlEscape(text);
        }
        var out = new StringBuilder();
        var linked = new HashSet<String>();
        var matcher = pattern.matcher(text);
        int last = 0;
        while (matcher.find()) {
            Entry entry = lookup(matcher.group());
            if (entry == null || linked.contains(entry.slug())) {
                continue; // already linked (or unknown) — leave it as plain escaped text
            }
            out.append(HtmlUtils.htmlEscape(text.substring(last, matcher.start())));
            appendPopover(out, entry, matcher.group(), idSeed);
            last = matcher.end();
            linked.add(entry.slug());
        }
        out.append(HtmlUtils.htmlEscape(text.substring(last)));
        return out.toString();
    }

    private void appendPopover(StringBuilder out, Entry entry, String matched, String idSeed) {
        String id = HtmlUtils.htmlEscape("glossary-" + idSeed + "-" + entry.slug());
        String label = HtmlUtils.htmlEscape(matched);
        out.append("<span class=\"glossary-term\">")
                .append("<button type=\"button\" class=\"glossary-link\" popovertarget=\"").append(id)
                .append("\" aria-label=\"Definition of ").append(label).append("\">")
                .append(label).append("</button>")
                .append("<span id=\"").append(id).append("\" popover class=\"info-popover-box glossary-popover\">")
                .append("<span class=\"glossary-definition\">").append(HtmlUtils.htmlEscape(entry.definition()))
                .append("</span>");
        if (!entry.example().isBlank()) {
            out.append("<em class=\"glossary-example\">").append(HtmlUtils.htmlEscape(entry.example()))
                    .append("</em>");
        }
        out.append("<a href=\"/glossary/").append(HtmlUtils.htmlEscape(entry.slug()))
                .append("\">Open in glossary →</a>")
                .append("</span></span>");
    }

    private Entry lookup(String matchedText) {
        for (var entry : entries) {
            if (entry.term().equalsIgnoreCase(matchedText)) {
                return entry;
            }
        }
        return null;
    }
}
