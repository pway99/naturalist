package com.naturalist.plants.console.render;

import com.naturalist.atlas.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Transforms a {@code Description}-level string into HTML with internal
 * landmarks — paragraph splits at semantic cues, italicised scientific
 * binomials, lifted numbered enumerations, and (when present) a typographic
 * header chip carrying the leading taxonomic line.
 *
 * <p>The binomial-italics pass is also an atlas-resolution pass: when an
 * {@link Atlas} and {@link LinkResolver} are supplied, italicised binomials
 * whose surface form resolves to a single unambiguous entity through the
 * atlas's search index <em>and</em> whose target's
 * {@link com.naturalist.ddd.EntityName} subclass is known to the resolver are
 * additionally wrapped in an {@code <a>} tag pointing at the target's console
 * page. Ambiguous resolutions (multiple equally-strong hits) and prefix-only
 * matches stay italics-only — the renderer never guesses which of two
 * {@code Trifolium} species the prose meant.
 *
 * <h2>Pipeline</h2>
 * <ol>
 *   <li><b>HTML escape</b> — the input is treated as plain text and is
 *       fully escaped before any structural transformation. The output's
 *       only HTML markup is the markup this class emits.</li>
 *   <li><b>Header extraction</b> — when the input starts with a recognisable
 *       taxonomic header (a binomial, optional authority, an em-dash, and a
 *       Family[: Subfamily[: Tribe]] line ending with a period), that span is
 *       lifted out of the body and rendered as a {@code <header>} chip.
 *       Inputs without a leading header pass through unchanged.</li>
 *   <li><b>Paragraph splitting</b> — sentence breaks immediately followed by
 *       any cue phrase from {@link #PARAGRAPH_CUES} become paragraph breaks.
 *       The cue list is a documented closed set; new entries are added as
 *       new authoring patterns emerge in the catalog.</li>
 *   <li><b>Numbered enumeration lifting</b> — within a paragraph, a sequence
 *       of {@code (1) ... (2) ...} markers (separated by {@code ;} or {@code .})
 *       is lifted into an {@code <ol>}. The lifted list inherits its
 *       containing paragraph's wrapping; trailing prose after the final item
 *       is rendered in its own paragraph.</li>
 *   <li><b>Binomial italics &amp; resolution</b> — full binomials
 *       ({@code Genus species}, optionally followed by
 *       {@code bv./var./subsp./ssp. epithet}) and abbreviated binomials
 *       ({@code G. species}) are wrapped in {@code <em>}. When an atlas and
 *       link resolver are configured, binomials whose surface form resolves
 *       are additionally wrapped in {@code <a>}.</li>
 * </ol>
 *
 * <p>The pipeline is content-conserving: every character of the original
 * input survives into the output (modulo HTML-escaping of {@code &}, {@code <},
 * {@code >}). The renderer never throws on real input.
 *
 * <h2>Graceful degradation</h2>
 * The no-arg constructor produces a renderer with no atlas wiring — the
 * binomial pass becomes pure italicisation. This is the shape used by unit
 * tests that do not exercise atlas behaviour and by any caller that wants the
 * legibility transforms without the cross-domain navigation surface.
 *
 * <h2>Thread-safety</h2>
 * The renderer is stateless apart from its immutable atlas/resolver
 * collaborators; a single instance is safe for concurrent use.
 */
public final class DescriptionRenderer {

    /**
     * Sentence-boundary cue phrases that introduce a new paragraph when they
     * appear immediately after a sentence break. This list is the renderer's
     * one piece of editorial judgement — adding a phrase here is the way a
     * naturalist surfaces a new authoring pattern that the eye treats as a
     * paragraph but the keyboard typed without one.
     *
     * <p>Order is irrelevant; matching is by exact prefix at a sentence
     * boundary.
     */
    private static final List<String> PARAGRAPH_CUES = List.of(
            "At Oak Vista",
            "Management constraint",
            "Practical significance",
            "Critical timing",
            "Bloom period at",
            "Bloom time at",
            "Florets are",
            "Annual;",
            "Endophyte associations",
            "Nitrogen fixation",
            "Root nodule symbiont",
            "The low growth form",
            "Management at non-standard"
    );

    /**
     * Leading-header pattern. Captures a binomial-shaped opening, optional
     * authority and parenthetical synonym, an em-dash, and one to three
     * colon-separated taxonomic ranks ending with a period. Tested against
     * authentic catalog entries — see the test class for the worked
     * examples.
     */
    private static final Pattern HEADER_PATTERN = Pattern.compile(
            "^([A-Z][a-z]+\\s+[a-z]+(?:\\s+(?:L\\.|Schreb\\.|var\\.|subsp\\.|ssp\\.))?(?:\\s+\\([^)]+\\))?)"
                    + "\\s+—\\s+"
                    + "([A-Z][a-zA-Z]+(?::\\s+[A-Z][a-zA-Z]+){0,2})\\.\\s+");

    /** Full binomial: {@code Genus species [bv|var|subsp|ssp. epithet]}. */
    private static final Pattern BINOMIAL_PATTERN = Pattern.compile(
            "\\b([A-Z][a-z]{2,})\\s+([a-z]{3,})(\\s+(?:bv|var|subsp|ssp)\\.\\s+[a-z]+)?\\b");

    /** Abbreviated binomial: {@code G. species}. */
    private static final Pattern ABBREVIATED_BINOMIAL_PATTERN = Pattern.compile(
            "\\b([A-Z])\\.\\s+([a-z]{3,})\\b");

    /**
     * Numbered enumeration entry inside a paragraph. The body class allows
     * one level of balanced inner parentheses so that items carrying
     * parenthetical asides — common in university-register prose
     * (e.g. {@code "Colias eurytheme (Orange Sulphur)"}) — are captured
     * intact rather than truncated at the first {@code '('}.
     */
    private static final Pattern NUMBERED_ITEM = Pattern.compile(
            "\\((\\d+)\\)\\s+((?:[^()]|\\([^()]*\\))+?)(?=(?:[;.]\\s+\\(\\d+\\))|(?:\\.\\s|$))");

    private final Atlas atlas;
    private final LinkResolver linkResolver;

    /**
     * Renderer with no atlas wiring — the binomial pass produces italics only.
     * Used by unit tests and callers that do not need cross-domain linking.
     */
    public DescriptionRenderer() {
        this.atlas = null;
        this.linkResolver = null;
    }

    /**
     * Renderer wired to an {@link Atlas} and a {@link LinkResolver}. A
     * binomial whose surface form resolves through {@code atlas} and whose
     * resulting {@link EntityRef} carries an {@link com.naturalist.ddd.EntityName}
     * subclass known to {@code linkResolver} is wrapped in an {@code <a>}
     * tag. Misses on either side fall through to italics-only.
     */
    public DescriptionRenderer(Atlas atlas, LinkResolver linkResolver) {
        this.atlas = atlas;
        this.linkResolver = linkResolver;
    }

    /**
     * Render the given description-level string as HTML. Returns an empty
     * string when the input is null or blank — the caller can emit the
     * output unconditionally.
     */
    public String render(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String escaped = htmlEscape(text);

        StringBuilder out = new StringBuilder();
        String body = extractHeader(escaped, out);
        renderBody(body, out);
        return out.toString();
    }

    // ── Header extraction ────────────────────────────────────────────────

    private String extractHeader(String escaped, StringBuilder out) {
        Matcher m = HEADER_PATTERN.matcher(escaped);
        if (!m.lookingAt()) {
            return escaped;
        }
        String binomial = m.group(1);
        String taxonomy = m.group(2);
        out.append("<header class=\"description-taxonomy\">")
                .append("<span class=\"binomial\">").append(decorateBinomials(binomial)).append("</span>")
                .append(" <span class=\"taxonomy\">").append(taxonomy).append("</span>")
                .append("</header>\n");
        return escaped.substring(m.end());
    }

    // ── Body rendering ───────────────────────────────────────────────────

    private void renderBody(String body, StringBuilder out) {
        for (String paragraph : splitParagraphs(body)) {
            renderParagraph(paragraph, out);
        }
    }

    private List<String> splitParagraphs(String body) {
        List<String> paragraphs = new ArrayList<>();
        int start = 0;
        int i = 0;
        while (i < body.length()) {
            if (body.charAt(i) == '.' && i + 2 < body.length() && body.charAt(i + 1) == ' ') {
                String tail = body.substring(i + 2);
                if (matchesCue(tail)) {
                    paragraphs.add(body.substring(start, i + 1).trim());
                    start = i + 2;
                }
            }
            i++;
        }
        String last = body.substring(start).trim();
        if (!last.isEmpty()) {
            paragraphs.add(last);
        }
        return paragraphs;
    }

    private boolean matchesCue(String tail) {
        for (String cue : PARAGRAPH_CUES) {
            if (tail.startsWith(cue)) {
                return true;
            }
        }
        return false;
    }

    private void renderParagraph(String paragraph, StringBuilder out) {
        Matcher first = NUMBERED_ITEM.matcher(paragraph);
        if (!first.find() || !first.group(1).equals("1")) {
            out.append("<p>").append(decorateBinomials(paragraph)).append("</p>\n");
            return;
        }
        int listStart = first.start();
        String preface = paragraph.substring(0, listStart).trim();
        if (preface.endsWith(":") || preface.endsWith(";") || preface.endsWith(",") || preface.endsWith(".")) {
            preface = preface.substring(0, preface.length() - 1).trim();
        }
        if (!preface.isEmpty()) {
            out.append("<p>").append(decorateBinomials(preface)).append("</p>\n");
        }

        out.append("<ol>\n");
        Matcher m = NUMBERED_ITEM.matcher(paragraph);
        m.region(listStart, paragraph.length());
        int afterLast = listStart;
        while (m.find()) {
            String item = m.group(2).trim();
            if (item.endsWith(";") || item.endsWith(",")) {
                item = item.substring(0, item.length() - 1).trim();
            }
            out.append("  <li>").append(decorateBinomials(item)).append("</li>\n");
            afterLast = m.end();
        }
        out.append("</ol>\n");

        String tail = paragraph.substring(afterLast).trim();
        if (tail.startsWith(".") || tail.startsWith(";")) {
            tail = tail.substring(1).trim();
        }
        if (!tail.isEmpty()) {
            out.append("<p>").append(decorateBinomials(tail)).append("</p>\n");
        }
    }

    // ── Binomial italics & atlas resolution ──────────────────────────────

    private String decorateBinomials(String text) {
        String afterFull = BINOMIAL_PATTERN.matcher(text).replaceAll(matchResult -> {
            String genus = matchResult.group(1);
            String species = matchResult.group(2);
            String suffix = matchResult.group(3);
            String inner = genus + " " + species + (suffix == null ? "" : suffix);
            return Matcher.quoteReplacement(decorate(inner));
        });
        return ABBREVIATED_BINOMIAL_PATTERN.matcher(afterFull).replaceAll(matchResult -> {
            String genus = matchResult.group(1);
            String species = matchResult.group(2);
            return Matcher.quoteReplacement(decorate(genus + ". " + species));
        });
    }

    private String decorate(String surfaceForm) {
        String italics = "<em>" + surfaceForm + "</em>";
        Optional<String> url = urlFor(surfaceForm);
        return url.map(href -> "<a href=\"" + attributeEscape(href) + "\">" + italics + "</a>").orElse(italics);
    }

    private Optional<String> urlFor(String surfaceForm) {
        if (atlas == null || linkResolver == null) {
            return Optional.empty();
        }
        SearchResults results = atlas.search(surfaceForm);
        // The renderer auto-links only when the surface form resolves to a
        // single unambiguous entity. Multiple equally-strong hits (e.g. a
        // shared genus token across two species) and prefix-only matches stay
        // italics-only — the prose author wrote the binomial; we don't want
        // the renderer guessing which Trifolium they meant.
        List<SearchHit> exact = results.stream()
                .filter(h -> h.kind() == MatchKind.EXACT_SLUG || h.kind() == MatchKind.EXACT_TOKEN)
                .toList();
        if (exact.size() != 1) {
            return Optional.empty();
        }
        EntityRef ref = exact.get(0).target();
        return linkResolver.urlFor(ref);
    }

    // ── HTML escape ──────────────────────────────────────────────────────

    private static String htmlEscape(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }

    private static String attributeEscape(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&' -> out.append("&amp;");
                case '"' -> out.append("&quot;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }
}
