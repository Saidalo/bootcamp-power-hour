package com.bootcamp.powerhour.dto;

import java.math.BigDecimal;

public record SavingsSummary(BigDecimal paid,
                             BigDecimal atAverage,
                             BigDecimal atPeak,
                             BigDecimal saved,
                             int completedRuns,
                             BigDecimal energyKwh,
                             int runsWithoutPrices) {
}