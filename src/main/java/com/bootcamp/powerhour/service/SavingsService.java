package com.bootcamp.powerhour.service;

import com.bootcamp.powerhour.dto.SavingsSummary;
import com.bootcamp.powerhour.entity.ElectricityPrice;
import com.bootcamp.powerhour.entity.PlannedRun;
import com.bootcamp.powerhour.entity.PlannedRunStatus;
import com.bootcamp.powerhour.repository.PlannedRunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

/**
 * For each completed run in a month: what it cost, what it would have cost at that
 * day's average and peak price, and the difference to the average.
 */
@Service
@RequiredArgsConstructor
public class SavingsService {
    private final PlannedRunRepository plannedRunRepository;
    private final PriceService priceService;

    @Transactional(readOnly = true)
    public SavingsSummary forMonth(long userId, YearMonth month) {
        List<PlannedRun> runs = plannedRunRepository
                .findByApplianceUserIdAndStatusAndStartsAtGreaterThanEqualAndStartsAtLessThan(
                        userId, PlannedRunStatus.COMPLETED,
                        PowerHourTime.startOfDay(month.atDay(1)),
                        PowerHourTime.startOfDay(month.plusMonths(1).atDay(1)));

        BigDecimal paid = BigDecimal.ZERO;
        BigDecimal atAverage = BigDecimal.ZERO;
        BigDecimal atPeak = BigDecimal.ZERO;
        BigDecimal energy = BigDecimal.ZERO;
        int counted = 0;
        int withoutPrices = 0;

        for (PlannedRun run : runs) {
            // zone and energy are read from the run, so editing the appliance later can't change history
            List<ElectricityPrice> prices = priceService.pricesForDay(
                    run.getBiddingZone(), PowerHourTime.localDate(run.getStartsAt()));
            if (prices.isEmpty()) {
                withoutPrices++;
                continue;
            }
            BigDecimal kwh = run.getEnergyKwh();
            BigDecimal average = prices.stream().map(ElectricityPrice::getPriceEurKwh)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(prices.size()), 6, RoundingMode.HALF_UP);
            BigDecimal peak = prices.stream().map(ElectricityPrice::getPriceEurKwh)
                    .max(Comparator.naturalOrder()).orElseThrow();

            paid = paid.add(run.getEstimatedCost());
            atAverage = atAverage.add(kwh.multiply(average));
            atPeak = atPeak.add(kwh.multiply(peak));
            energy = energy.add(kwh);
            counted++;
        }

        return new SavingsSummary(money(paid), money(atAverage), money(atPeak),
                money(atAverage.subtract(paid)), counted,
                energy.setScale(1, RoundingMode.HALF_UP), withoutPrices);
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}