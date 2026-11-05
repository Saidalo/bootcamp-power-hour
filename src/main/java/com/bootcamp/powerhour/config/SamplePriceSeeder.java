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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Fills made-up hourly prices for the last 30 days, today and tomorrow, so the UI works
 * without Elering. Days that already have prices are left alone.
 * Turn it off with powerhour.sample-prices.enabled=false.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "powerhour.sample-prices.enabled", havingValue = "true")
public class SamplePriceSeeder implements CommandLineRunner {

    /** A typical day in €/kWh: cheap at night, peaks in the morning and evening. */
    private static final double[] HOURLY = {
            0.045, 0.040, 0.036, 0.034, 0.035, 0.042, 0.070, 0.110, 0.130, 0.105, 0.085, 0.075,
            0.070, 0.068, 0.072, 0.085, 0.110, 0.150, 0.180, 0.165, 0.130, 0.095, 0.070, 0.055
    };

    private final ElectricityPriceRepository priceRepository;

    @Override
    public void run(String... args) {
        LocalDate today = PowerHourTime.today();
        int days = 0;
        for (BiddingZone zone : BiddingZone.values()) {
            for (LocalDate date = today.minusDays(30); !date.isAfter(today.plusDays(1)); date = date.plusDays(1)) {
                OffsetDateTime start = PowerHourTime.startOfDay(date);
                if (!priceRepository.existsByBiddingZoneAndStartsAtGreaterThanEqualAndStartsAtLessThan(
                        zone, start, PowerHourTime.startOfDay(date.plusDays(1)))) {
                    priceRepository.saveAll(dayOf(zone, date, start));
                    days++;
                }
            }
        }
        if (days > 0) {
            log.info("Seeded sample prices for {} zone-days", days);
        }
    }

    private List<ElectricityPrice> dayOf(BiddingZone zone, LocalDate date, OffsetDateTime start) {
        double dayFactor = 1 + ((date.getDayOfYear() * 37) % 21 - 10) / 100.0;
        double zoneFactor = switch (zone) {
            case EE -> 0.97;
            case LV -> 1.00;
            case LT -> 1.02;
        };
        List<ElectricityPrice> prices = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            ElectricityPrice price = new ElectricityPrice();
            price.setBiddingZone(zone);
            price.setStartsAt(start.plusHours(hour));
            price.setPriceEurKwh(BigDecimal.valueOf(HOURLY[hour] * dayFactor * zoneFactor)
                    .setScale(6, RoundingMode.HALF_UP));
            prices.add(price);
        }
        return prices;
    }
}