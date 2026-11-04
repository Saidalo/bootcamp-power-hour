package com.bootcamp.powerhour.provider;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

final class HourlyPriceAggregator {
    private static final BigDecimal KWH_PER_MWH = BigDecimal.valueOf(1000);

    private HourlyPriceAggregator(){
    }
    static List<HourlyPricePoint> toHourly(List<EleringResponse.PricePoint> points){
        Map<Instant, List<BigDecimal>> byHour = new TreeMap<>();
        for (EleringResponse.PricePoint point : points) {
            Instant hour = Instant.ofEpochSecond(point.timestamp()).truncatedTo(ChronoUnit.HOURS);
            byHour.computeIfAbsent(hour, h -> new ArrayList<>()).add(point.price());
        }

        List<HourlyPricePoint> result = new ArrayList<>();
        byHour.forEach((hour, prices) -> {
            BigDecimal sum = prices.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal averageKwh = sum.divide(KWH_PER_MWH.multiply(BigDecimal.valueOf(prices.size())), 6, RoundingMode.HALF_UP);
            result.add(new HourlyPricePoint(OffsetDateTime.ofInstant(hour, ZoneOffset.UTC), averageKwh));
        });
return result;

    }
}
