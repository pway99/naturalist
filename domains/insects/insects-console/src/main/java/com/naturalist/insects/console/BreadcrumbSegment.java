package com.naturalist.insects.console;

public record BreadcrumbSegment(String label, String url, boolean currentPage) {

    public static BreadcrumbSegment link(String label, String url) {
        return new BreadcrumbSegment(label, url, false);
    }

    public static BreadcrumbSegment text(String label) {
        return new BreadcrumbSegment(label, null, false);
    }

    public static BreadcrumbSegment current(String label) {
        return new BreadcrumbSegment(label, null, true);
    }
}
