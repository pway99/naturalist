package com.naturalist.zone.subzone;

/**
 * The position of a SubZone within its parent Zone, expressed relative to the Zone's
 * long axis or cardinal orientation.
 * <p>
 * Relative position provides spatial context for pest pressure history, sensor correlation,
 * and crop rotation planning without requiring precise coordinate geometry. It captures
 * the management-relevant question: <em>which part of this bed?</em>
 * <p>
 * At Oak Vista, the backyard garden bed runs approximately north–south (100 sqft total).
 * The three sub-zones that emerged from the April 7, 2026 TSWV management event map
 * naturally to {@code NORTH_END}, {@code CENTER}, and {@code SOUTH_END}:
 * <ul>
 *   <li>{@code NORTH_END} — infected San Marzano section, plants pulled, straw thinned.</li>
 *   <li>{@code CENTER} — Nick's Italian Pear, highest protection priority.</li>
 *   <li>{@code SOUTH_END} — Amish Paste transplants, installed April 6, under observation.</li>
 * </ul>
 * <p>
 * For raised beds with a rectangular footprint, {@code NORTH_END} / {@code SOUTH_END} /
 * {@code EAST_END} / {@code WEST_END} describe the terminal sections along each axis.
 * {@code CENTER} describes the interior section not at any terminal.
 * The diagonal values ({@code NORTHWEST} etc.) apply to larger irregular zones or
 * orchard blocks with meaningful quadrant divisions.
 */
public enum RelativePosition {

    /**
     * The northernmost section of the Zone.
     */
    NORTH_END,

    /**
     * The southernmost section of the Zone.
     */
    SOUTH_END,

    /**
     * The easternmost section of the Zone.
     */
    EAST_END,

    /**
     * The westernmost section of the Zone.
     */
    WEST_END,

    /**
     * The interior section, not at any terminal edge.
     * <p>
     * In a three-part division of a linear bed (north, center, south), this is the middle
     * section. Nick's Italian Pear occupies the center of the backyard bed —
     * flanked by higher-risk sections on both sides — making its CENTER position
     * a spatial management fact, not merely a label.
     */
    CENTER,

    /**
     * Northwest quadrant — for larger zones with meaningful diagonal divisions.
     */
    NORTHWEST,

    /**
     * Northeast quadrant.
     */
    NORTHEAST,

    /**
     * Southwest quadrant.
     */
    SOUTHWEST,

    /**
     * Southeast quadrant.
     */
    SOUTHEAST
}
