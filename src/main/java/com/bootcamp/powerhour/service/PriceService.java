package com.bootcamp.powerhour.service;

import com.bootcamp.powerhour.dto.SlotView;
import com.bootcamp.powerhour.dto.PriceTick;
import com.bootcamp.powerhour.entity.BiddingZone;
import com.bootcamp.powerhour.entity.ElectricityPrice;
import com.bootcamp.powerhour.repository.ElectricityPriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PriceService {
    private final ElectricityPriceRepository priceRepository;

//  All price slots of one local day, earliest first.
    public List<ElectricityPrice> pricesForDay(BiddingZone zone, LocalDate date) {
        return priceRepository.findInRange(
                zone, PowerHourTime.startOfDay(date), PowerHourTime.startOfDay(date.plusDays(1)));
    }

    public Optional<ElectricityPrice> cheapest(List<ElectricityPrice> prices) {
        return prices.stream().min(Comparator.comparing(ElectricityPrice::getPriceEurKwh));
    }

    //  Steps the price axis may use, smallest first. We pick the first one that needs at most 5 lines.
    private static final List<BigDecimal> AXIS_STEPS = List.of(
            new BigDecimal("0.005"), new BigDecimal("0.01"), new BigDecimal("0.02"),
            new BigDecimal("0.025"), new BigDecimal("0.05"), new BigDecimal("0.1"),
            new BigDecimal("0.2"), new BigDecimal("0.25"), new BigDecimal("0.5"),
            new BigDecimal("1"), new BigDecimal("2"), new BigDecimal("5"));
    private static final int MAX_LINES = 5;

    //  Chart bars; allowed[i] and chosen[i] may be null to mean "all allowed" / "none chosen".
    public List<SlotView> toSlots(List<ElectricityPrice> prices, boolean[] allowed, boolean[] chosen) {
        BigDecimal top = axisTop(prices);
        List<SlotView> slots = new ArrayList<>();
        for (int i = 0; i < prices.size(); i++) {
            ElectricityPrice p = prices.get(i);
            String time = PowerHourTime.time(p.getStartsAt());
            int height = Math.max(2, percentOf(p.getPriceEurKwh().max(BigDecimal.ZERO), top));
            slots.add(new SlotView(time, p.getPriceEurKwh(),
                    allowed == null || allowed[i],
                    chosen != null && chosen[i],
                    height,
                    time.endsWith(":00") ? time.substring(0, 2) : ""));
        }
        return slots;
    }

    //  Horizontal lines for the price axis: 0, step, 2*step ... up to the top of the chart.
    public List<PriceTick> axisTicks(List<ElectricityPrice> prices) {
        BigDecimal step = axisStep(prices);
        BigDecimal top = axisTop(prices);
        int decimals = Math.max(0, step.stripTrailingZeros().scale());   // 0.05 -> 2, so labels read 0.10, 0.15
        List<PriceTick> ticks = new ArrayList<>();
        for (BigDecimal value = BigDecimal.ZERO; value.compareTo(top) <= 0; value = value.add(step)) {
            String label = value.setScale(decimals, RoundingMode.HALF_UP).toPlainString();
            ticks.add(new PriceTick(label, percentOf(value, top)));
        }
        return ticks;
    }

    //  The price at the very top of the chart: the highest price rounded up to a whole step.
    private BigDecimal axisTop(List<ElectricityPrice> prices) {
        BigDecimal step = axisStep(prices);
        BigDecimal steps = maxPrice(prices).divide(step, 0, RoundingMode.CEILING).max(BigDecimal.ONE);
        return step.multiply(steps);
    }

    private BigDecimal axisStep(List<ElectricityPrice> prices) {
        BigDecimal max = maxPrice(prices);
        for (BigDecimal step : AXIS_STEPS) {
            if (max.divide(step, 0, RoundingMode.CEILING).intValue() <= MAX_LINES) {
                return step;
            }
        }
        // Prices above 25 €/kWh: fall back to five equal steps.
        return max.divide(BigDecimal.valueOf(MAX_LINES), 2, RoundingMode.CEILING);
    }

    //  Highest price of the day; negative or missing prices count as zero.
    private BigDecimal maxPrice(List<ElectricityPrice> prices) {
        return prices.stream().map(ElectricityPrice::getPriceEurKwh)
                .max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO).max(BigDecimal.ZERO);
    }

    private int percentOf(BigDecimal value, BigDecimal top) {
        return value.multiply(BigDecimal.valueOf(100)).divide(top, 0, RoundingMode.HALF_UP).intValue();
    }
}