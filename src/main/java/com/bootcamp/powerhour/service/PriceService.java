package com.bootcamp.powerhour.service;

import com.bootcamp.powerhour.dto.SlotView;
import com.bootcamp.powerhour.entity.BiddingZone;
import com.bootcamp.powerhour.entity.ElectricityPrice;
import com.bootcamp.powerhour.repository.ElectricityPriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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

//  Chart bars; allowed[i] and chosen[i] may be null to mean "all allowed" / "none chosen".
    public List<SlotView> toSlots(List<ElectricityPrice> prices, boolean[] allowed, boolean[] chosen) {
        BigDecimal max = prices.stream().map(ElectricityPrice::getPriceEurKwh)
                .max(Comparator.naturalOrder()).orElse(BigDecimal.ONE);
        List<SlotView> slots = new ArrayList<>();
        for (int i = 0; i < prices.size(); i++) {
            ElectricityPrice p = prices.get(i);
            String time = PowerHourTime.time(p.getStartsAt());
            int height = max.signum() <= 0 ? 2
                    : Math.max(2, p.getPriceEurKwh().max(BigDecimal.ZERO).multiply(BigDecimal.valueOf(100))
                    .divide(max, 0, java.math.RoundingMode.HALF_UP).intValue());
            slots.add(new SlotView(time, p.getPriceEurKwh(),
                    allowed == null || allowed[i],
                    chosen != null && chosen[i],
                    height,
                    time.endsWith(":00") ? time.substring(0, 2) : ""));
        }
        return slots;
    }
}