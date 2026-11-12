package com.bootcamp.powerhour.config;

import com.bootcamp.powerhour.entity.BiddingZone;
import com.bootcamp.powerhour.entity.ElectricityPrice;
import com.bootcamp.powerhour.repository.ElectricityPriceRepository;
import com.bootcamp.powerhour.service.PowerHourTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Fills made-up 15-minute prices for the last 30 days, today and tomorrow, so the UI works
 * without Elering. Days that already have a full set of 15-minute prices are left alone.
 * Days with fewer slots (old hourly sample data) are replaced.
 * Turn it off with powerhour.sample-prices.enabled=false.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "powerhour.sample-prices.enabled", havingValue = "true")
public class SamplePriceSeeder implements CommandLineRunner {

    // Nord Pool has used 15-minute prices since October 2025.
    static final int SLOT_MINUTES = 15;

    // A typical day in €/kWh: cheap at night, peaks in the morning and evening.
    private static final double[] HOURLY = {
            0.045, 0.040, 0.036, 0.034, 0.035, 0.042, 0.070, 0.110, 0.130, 0.105, 0.085, 0.075,
            0.070, 0.068, 0.072, 0.085, 0.110, 0.150, 0.180, 0.165, 0.130, 0.095, 0.070, 0.055
    };

    private final ElectricityPriceRepository priceRepository;

    @Override
    @Transactional
    public void run(String... args) {
        LocalDate today = PowerHourTime.today();
        int days = 0;
        for (BiddingZone zone : BiddingZone.values()) {
            for (LocalDate date = today.minusDays(30); !date.isAfter(today.plusDays(1)); date = date.plusDays(1)) {
                OffsetDateTime start = PowerHourTime.startOfDay(date);
                OffsetDateTime end = PowerHourTime.startOfDay(date.plusDays(1));
                long slotsInDay = Duration.between(start, end).toMinutes() / SLOT_MINUTES;   // 92 or 100 on DST days
                long existing = priceRepository.countInRange(zone, start, end);
                if (existing >= slotsInDay) {
                    continue;
                }
                if (existing > 0) {
                    // Old hourly sample data: replace it with 15-minute slots
                    priceRepository.deleteInRange(zone, start, end);
                    priceRepository.flush();
                }
                priceRepository.saveAll(dayOf(zone, date, start, end));
                days++;
            }
        }
        if (days > 0) {
            log.info("Seeded 15-minute sample prices for {} zone-days", days);
        }
    }

    List<ElectricityPrice> dayOf(BiddingZone zone, LocalDate date, OffsetDateTime start, OffsetDateTime end) {
        double dayFactor = 1 + ((date.getDayOfYear() * 37) % 21 - 10) / 100.0;
        double zoneFactor = switch (zone) {
            case EE -> 0.97;
            case LV -> 1.00;
            case LT -> 1.02;
        };
        // Same zone and day always give the same prices, so restarts don't change the chart
        Random random = new Random(date.toEpochDay() * 31 + zone.ordinal());

        List<ElectricityPrice> prices = new ArrayList<>();
        for (OffsetDateTime slot = start; slot.isBefore(end); slot = slot.plusMinutes(SLOT_MINUTES)) {
            ElectricityPrice price = new ElectricityPrice();
            price.setBiddingZone(zone);
            price.setStartsAt(slot);
            double value = typicalPriceAt(slot) * dayFactor * zoneFactor * (0.9 + random.nextDouble() * 0.2);
            price.setPriceEurKwh(BigDecimal.valueOf(value).setScale(6, RoundingMode.HALF_UP));
            prices.add(price);
        }
        return prices;
    }

    // The hourly curve, smoothed so the four quarters of an hour step towards the next hour.
    private static double typicalPriceAt(OffsetDateTime slot) {
        var local = slot.atZoneSameInstant(PowerHourTime.ZONE);
        int hour = local.getHour();
        double fraction = local.getMinute() / 60.0;
        return HOURLY[hour] + (HOURLY[(hour + 1) % 24] - HOURLY[hour]) * fraction;
    }
}
