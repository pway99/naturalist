package com.naturalist.usage;

/**
 * The time window a {@link UsageCounter} rule's {@code limit} is measured
 * over: a rolling calendar day ({@code CALENDAR_DAY}), or an open-ended
 * window starting at the rule's {@code since} instant ({@code SINCE}).
 */
public enum WindowKind {
    CALENDAR_DAY,
    SINCE
}
