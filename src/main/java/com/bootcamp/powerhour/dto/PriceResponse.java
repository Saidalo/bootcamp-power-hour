package com.bootcamp.powerhour.dto;

import com.bootcamp.powerhour.entity.ElectricityPrice;
import com.bootcamp.powerhour.service.PowerHourTime;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Schema(description = "One hourly price slot")
public record PriceResponse(
        @Schema(description = "Start of the hour, local time", example = "2026-11-11T03:00:00+02:00")
        OffsetDateTime startsAt,
        @Schema(description = "Price in euros per kWh", example = "0.0340")
        BigDecimal priceEurKwh) {

    public static PriceResponse from(ElectricityPrice price) {
        return new PriceResponse(
                price.getStartsAt().atZoneSameInstant(PowerHourTime.ZONE).toOffsetDateTime(),
                price.getPriceEurKwh());
    }
}