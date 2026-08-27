package com.naturalist.usage;

import java.util.List;

public record UsageSnapshot(int dailyUsed, int dailyLimit, int monthlyUsed, int monthlyLimit,
                             List<UserUsage> users) {

    public record UserUsage(String naturalist, int used, int limit) {
    }
}
