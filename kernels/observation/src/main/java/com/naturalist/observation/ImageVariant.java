package com.naturalist.observation;

/**
 * Which stored rendition of an image {@link ImageStore#read} returns: the full-size {@link #PHOTO}
 * (longest edge at most 1024 px) or the {@link #TILE} (longest edge at most 640 px, always JPEG)
 * shown in grids.
 */
public enum ImageVariant {
    PHOTO,
    TILE
}
