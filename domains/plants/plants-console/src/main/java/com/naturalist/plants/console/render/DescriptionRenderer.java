package com.naturalist.plants.console.render;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Transforms a {@code Description}-level string into HTML with internal
 * landmarks &mdash; paragraph splits at semantic cues, italicised scientific
 * binomials wrapped as catalog-search affordances, lifted numbered
 * enumerations, and (when present) a typographic header chip carrying the
 * leading taxonomic line.
 *
 * <p>Every detected binomial &mdash; full ({@code Genus species}) or
 * abbreviated ({@code G. species}) &mdash; is italicised <em>and</em> wrapped
 * in an {@code <a href="/search?q={url-encoded-term}" class="discover">} tag
 * regardless of whether the catalog currently knows the term. The empty-state
 * lives on the search results page; the renderer never makes a routing
 * decision. The {@code discover} class lets the layout style the affordance
 * distinctly from regular links.
 *
 * <h2>Pipeline</h2>
 * <ol>
 *   <li><b>HTML escape</b> &mdash; the input is treated as plain text and is
 *       fully escaped before any structural transformation. The output's
 *       only HTML markup is the markup this class emits.</li>
 *   <li><b>Header extraction</b> &mdash; when the input starts with a
 *       recognisable taxonomic header (a binomial, optional authority, an
 *       em-dash, and a Family[: Subfamily[: Tribe]] line ending with a
 *       period), that span is lifted out of the body and rendered as a
 *       {@code <header>} chip. Inputs without a leading header pass through
 *       unchanged.</li>
 *   <li><b>Paragraph splitting</b> &mdash; sentence breaks immediately
 *       followed by any cue phrase from {@link #PARAGRAPH_CUES} become
 *       paragraph breaks.</li>
 *   <li><b>Numbered enumeration lifting</b> &mdash; within a paragraph, a
 *       sequence of {@code (1) ... (2) ...} markers (separated by {@code ;}
 *       or {@code .}) is lifted into an {@code <ol>}.</li>
 *   <li><b>Binomial italics &amp; search wrap</b> &mdash; full binomials
 *       ({@code Genus species}, optionally followed by
 *       {@code bv./var./subsp./ssp. epithet}) and abbreviated binomials
 *       ({@code G. species}) are italicised and wrapped in a search
 *       anchor.</li>
 * </ol>
 *
 * <p>The pipeline is content-conserving: every character of the original
 * input survives into the output (modulo HTML-escaping of {@code &}, {@code <},
 * {@code >}). The renderer never throws on real input.
 *
 * <h2>Thread-safety</h2>
 * The renderer is stateless; a single instance is safe for concurrent use.
 */
public final class DescriptionRenderer {

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

    private static final Pattern HEADER_PATTERN = Pattern.compile(
            "^([A-Z][a-z]+\\s+[a-z]+(?:\\s+(?:L\\.|Schreb\\.|var\\.|subsp\\.|ssp\\.))?(?:\\s+\\([^)]+\\))?)"
                    + "\\s+—\\s+"
                    + "([A-Z][a-zA-Z]+(?::\\s+[A-Z][a-zA-Z]+){0,2})\\.\\s+");

    private static final Pattern BINOMIAL_PATTERN = Pattern.compile(
            "\\b([A-Z][a-z]{2,})\\s+([a-z]{3,})(\\s+(?:bv|var|subsp|ssp)\\.\\s+[a-z]+)?\\b");

    private static final Pattern ABBREVIATED_BINOMIAL_PATTERN = Pattern.compile(
            "\\b([A-Z])\\.\\s+([a-z]{3,})\\b");

    private static final Pattern NUMBERED_ITEM = Pattern.compile(
            "\\((\\d+)\\)\\s+((?:[^()]|\\([^()]*\\))+?)(?=(?:[;.]\\s+\\(\\d+\\))|(?:\\.\\s|$))");

    public DescriptionRenderer() {
    }

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

    // ── Binomial italics & search wrap ───────────────────────────────────

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
        String encoded = URLEncoder.encode(surfaceForm, StandardCharsets.UTF_8);
        return "<a href=\"/search?q=" + encoded + "\" class=\"discover\">"
                + "<em>" + surfaceForm + "</em></a>";
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
}
