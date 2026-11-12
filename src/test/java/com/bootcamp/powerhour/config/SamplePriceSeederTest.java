package com.bootcamp.powerhour.config;

import com.bootcamp.powerhour.entity.BiddingZone;
import com.bootcamp.powerhour.entity.ElectricityPrice;
import com.bootcamp.powerhour.service.PowerHourTime;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SamplePriceSeederTest {

    private final SamplePriceSeeder seeder = new SamplePriceSeeder(null);

    private List<ElectricityPrice> dayOf(LocalDate date) {
        OffsetDateTime start = PowerHourTime.startOfDay(date);
        return seeder.dayOf(BiddingZone.LV, date, start, PowerHourTime.startOfDay(date.plusDays(1)));
    }

    @Test
    void aNormalDayHas96QuarterHourSlots() {
        LocalDate date = LocalDate.of(2026, 11, 12);
        List<ElectricityPrice> prices = dayOf(date);

        assertThat(prices).hasSize(96);
        assertThat(prices.getFirst().getStartsAt()).isEqualTo(PowerHourTime.startOfDay(date));
        for (int i = 1; i < prices.size(); i++) {
            assertThat(Duration.between(prices.get(i - 1).getStartsAt(), prices.get(i).getStartsAt()))
                    .isEqualTo(Duration.ofMinutes(15));
        }
    }

    @Test
    void dstDaysHaveOneHourMoreOrLess() {
        assertThat(dayOf(LocalDate.of(2026, 3, 29))).hasSize(92);    // clocks go forward
        assertThat(dayOf(LocalDate.of(2026, 10, 25))).hasSize(100);  // clocks go back
    }

    @Test
    void theSameDayAlwaysGetsTheSamePrices() {
        LocalDate date = LocalDate.of(2026, 11, 12);
        assertThat(dayOf(date)).extracting(ElectricityPrice::getPriceEurKwh)
                .isEqualTo(dayOf(date).stream().map(ElectricityPrice::getPriceEurKwh).toList());
    }

    @Test
    void nightIsCheaperThanTheEveningPeak() {
        List<ElectricityPrice> prices = dayOf(LocalDate.of(2026, 11, 12));
        // 03:00 local is slot 12, 18:00 local is slot 72
        assertThat(prices.get(12).getPriceEurKwh()).isLessThan(prices.get(72).getPriceEurKwh());
    }
}
