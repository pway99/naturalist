package com.naturalist.insects.console;

/**
 * One segment of the taxonomic breadcrumb. {@code rank} is the Linnaean-rank
 * label rendered above the segment name in the breadcrumb (e.g. "Kingdom",
 * "Order", "Species"); null when no rank label applies to this segment.
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
