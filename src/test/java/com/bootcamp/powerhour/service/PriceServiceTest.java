package com.bootcamp.powerhour.service;

import com.bootcamp.powerhour.dto.PriceTick;
import com.bootcamp.powerhour.dto.SlotView;
import com.bootcamp.powerhour.entity.BiddingZone;
import com.bootcamp.powerhour.entity.ElectricityPrice;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


class PriceServiceTest {

    private static final OffsetDateTime DAY_START =
            ZonedDateTime.of(2026, 11, 10, 0, 0, 0, 0, PowerHourTime.ZONE).toOffsetDateTime();

    // The repository is not used by toSlots or axisTicks, so null is fine here.
    private final PriceService service = new PriceService(null);

    @Test
    void roundsTheTopUpToANiceStep() {
        List<ElectricityPrice> prices = hourly("0.03", "0.17", "0.08");

        List<PriceTick> ticks = service.axisTicks(prices);

        assertThat(ticks).extracting(PriceTick::label)
                .containsExactly("0.00", "0.05", "0.10", "0.15", "0.20");
        assertThat(ticks).extracting(PriceTick::percent)
                .containsExactly(0, 25, 50, 75, 100);
    }

    @Test
    void barsAreMeasuredAgainstTheTopLine() {
        List<ElectricityPrice> prices = hourly("0.10", "0.17");

        List<SlotView> slots = service.toSlots(prices, null, null);

        // the top line is 0.20, so 0.10 is half way up
        assertThat(slots.get(0).heightPercent()).isEqualTo(50);
        assertThat(slots.get(1).heightPercent()).isEqualTo(85);
    }

    @Test
    void smallPricesGetSmallSteps() {
        List<PriceTick> ticks = service.axisTicks(hourly("0.012", "0.018"));

        assertThat(ticks).extracting(PriceTick::label)
                .containsExactly("0.000", "0.005", "0.010", "0.015", "0.020");
    }

    @Test
    void negativeOrNoPricesStillDrawAZeroLine() {
        assertThat(service.axisTicks(hourly("-0.02", "-0.01")).get(0).label()).isEqualTo("0.000");
        assertThat(service.axisTicks(List.of())).isNotEmpty();
    }

    private List<ElectricityPrice> hourly(String... values) {
        List<ElectricityPrice> prices = new ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            ElectricityPrice p = new ElectricityPrice();
            p.setBiddingZone(BiddingZone.LV);
            p.setStartsAt(DAY_START.plusHours(i));
            p.setPriceEurKwh(new BigDecimal(values[i]));
            prices.add(p);
        }
        return prices;
    }
}