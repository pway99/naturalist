package com.naturalist.console.images;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Property-bound location of the image store under {@code naturalist.images.*}. {@code root} is the
 * filesystem directory holding one {@code <space>/} subdirectory per domain; it defaults to
 * {@code data/images} when unset or blank.
 */
@ConfigurationProperties("naturalist.images")
public record ImageStoreProperties(String root) {

    public ImageStoreProperties {
        if (root == null || root.isBlank()) {
            root = "data/images";
        }
    }
}
