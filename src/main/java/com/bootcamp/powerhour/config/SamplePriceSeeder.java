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
 * Fills made-up quarter hourly prices for the last 30 days, today and tomorrow, so the UI works
 * without Elering. Days that already have prices are left alone.
 * Turn it off with powerhour.sample-prices.enabled=false.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "powerhour.sample-prices.enabled", havingValue = "true")
public class SamplePriceSeeder implements CommandLineRunner {

    /** A typical day in €/kWh: cheap at night, peaks in the morning and evening. */
    private static final double[] QUARTER_HOURLY = {
            0.045, 0.044, 0.042, 0.041, 0.040, 0.039, 0.038, 0.037, 0.036, 0.035, 0.035, 0.035,
            0.034, 0.034, 0.035, 0.035, 0.035, 0.037, 0.039, 0.040, 0.042, 0.049, 0.056, 0.063,
            0.070, 0.080, 0.090, 0.100, 0.110, 0.115, 0.120, 0.125, 0.130, 0.124, 0.117, 0.111,
            0.105, 0.100, 0.095, 0.090, 0.085, 0.083, 0.080, 0.077, 0.075, 0.074, 0.073, 0.071,
            0.070, 0.070, 0.069, 0.069, 0.068, 0.069, 0.070, 0.071, 0.072, 0.075, 0.079, 0.082,
            0.085, 0.091, 0.098, 0.104, 0.110, 0.120, 0.130, 0.140, 0.150, 0.158, 0.165, 0.172,
            0.180, 0.176, 0.172, 0.169, 0.165, 0.156, 0.148, 0.139, 0.130, 0.121, 0.113, 0.104,
            0.095, 0.089, 0.083, 0.076, 0.070, 0.066, 0.062, 0.059, 0.055, 0.052, 0.050
    };
    private final ElectricityPriceRepository priceRepository;

    @Override
    public void run(String... args) {
        LocalDate today = PowerHourTime.today();
        int days = 0;
        for (BiddingZone zone : BiddingZone.values()) {
            for (LocalDate date = today.minusDays(30); !date.isAfter(today.plusDays(1)); date = date.plusDays(1)) {
                OffsetDateTime start = PowerHourTime.startOfDay(date);
                if (!priceRepository.existsInRange(
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
        for (int i = 0; i < QUARTER_HOURLY.length; i++) {
            ElectricityPrice price = new ElectricityPrice();
            price.setBiddingZone(zone);
            price.setStartsAt(start.plusMinutes(i * 15L));
            price.setPriceEurKwh(BigDecimal.valueOf(QUARTER_HOURLY [i] * dayFactor * zoneFactor)
                    .setScale(6, RoundingMode.HALF_UP));
            prices.add(price);
        }
        return prices;
    }
}