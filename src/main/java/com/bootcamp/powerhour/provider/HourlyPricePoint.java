package com.bootcamp.powerhour.provider;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record HourlyPricePoint(OffsetDateTime startsAt, BigDecimal priceEurKwh) {
}
