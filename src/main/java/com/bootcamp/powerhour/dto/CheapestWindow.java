package com.bootcamp.powerhour.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * The cheapest block of consecutive price slots an appliance fits into.
 *
 * @author Karyna Litvinova
 */
public record CheapestWindow(
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        BigDecimal energyKwh,
        BigDecimal estimatedCost) {
}