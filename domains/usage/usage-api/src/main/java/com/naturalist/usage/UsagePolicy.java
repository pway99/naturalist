package com.naturalist.usage;

public final class UsagePolicy {

    private UsagePolicy() {
    }

    public static int warningThreshold(int limit, int warningPercent) {
        return (int) Math.ceil(limit * (double) warningPercent / 100);
    }

    public static String alertMessage(AlertScope scope, AlertKind kind, int used, int limit) {
        return "%s identification budget %s: %d/%d used".formatted(scope.name().toLowerCase(), kind, used, limit);
    }

    public static AlertScope scopeOf(LimitKind limitKind) {
        return switch (limitKind) {
            case DAILY -> AlertScope.DAILY;
            case MONTHLY -> AlertScope.MONTHLY;
            case PER_USER, RATE -> throw new IllegalArgumentException(
                    "No alert scope for limit kind: " + limitKind);
        };
    }
}
