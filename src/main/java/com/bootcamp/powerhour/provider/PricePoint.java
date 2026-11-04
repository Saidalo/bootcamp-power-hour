package com.bootcamp.powerhour.provider;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PricePoint(OffsetDateTime startsAt, BigDecimal priceEurKwh) {
}
