package com.naturalist.usage;

public record UsageLimits(int perUserDaily, int globalRatePerMinute,
                           int globalDaily, int globalMonthly, int warningPercent) {
}
