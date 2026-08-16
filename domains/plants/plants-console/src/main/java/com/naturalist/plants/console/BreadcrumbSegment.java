package com.naturalist.plants.console;

/**
 * One segment of the plant taxonomic breadcrumb — the header row that traces a
 * record's Linnaean placement, Kingdom → Order → Family → Genus → Species.
 * <p>
 * A direct copy of the insects console's segment: plants and insects render the
 * same breadcrumb component ({@code plants/breadcrumb.jte} mirrors
 * {@code insects/breadcrumb.jte}), so a record's placement reads the same in
 * both consoles. {@code rank} is the Linnaean-rank label shown above the segment
 * name (e.g. "Order", "Species"); null when no rank label applies.
 */
public record BreadcrumbSegment(String label, String url, boolean currentPage, String rank) {

    public static BreadcrumbSegment link(String label, String url) {
        return new BreadcrumbSegment(label, url, false, null);
    }

    public static BreadcrumbSegment link(String label, String url, String rank) {
        return new BreadcrumbSegment(label, url, false, rank);
    }

    public static BreadcrumbSegment text(String label) {
        return new BreadcrumbSegment(label, null, false, null);
    }

    public static BreadcrumbSegment text(String label, String rank) {
        return new BreadcrumbSegment(label, null, false, rank);
    }

    public static BreadcrumbSegment current(String label) {
        return new BreadcrumbSegment(label, null, true, null);
    }

    public static BreadcrumbSegment current(String label, String rank) {
        return new BreadcrumbSegment(label, null, true, rank);
    }
}
