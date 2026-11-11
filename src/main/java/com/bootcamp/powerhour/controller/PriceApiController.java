package com.bootcamp.powerhour.controller;

import com.bootcamp.powerhour.dto.PriceResponse;
import com.bootcamp.powerhour.entity.BiddingZone;
import com.bootcamp.powerhour.entity.ElectricityPrice;
import com.bootcamp.powerhour.exception.NotFoundException;
import com.bootcamp.powerhour.service.PowerHourTime;
import com.bootcamp.powerhour.service.PriceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Prices", description = "Day-ahead electricity prices")
@RestController
@RequestMapping("/api/prices")
@RequiredArgsConstructor
public class PriceApiController {
    private final PriceService priceService;

    @Operation(summary = "All hourly prices of one day")
    @GetMapping
    public List<PriceResponse> pricesForDay(
            @Parameter(description = "Bidding zone", example = "LT") @RequestParam BiddingZone zone,
            @Parameter(description = "Day, local time. Default: today", example = "2026-11-11")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return priceService.pricesForDay(zone, dayOrToday(date)).stream()
                .map(PriceResponse::from)
                .toList();
    }

    @Operation(summary = "The cheapest hour of one day")
    @ApiResponse(responseCode = "200", description = "Cheapest hour found")
    @ApiResponse(responseCode = "404", description = "No prices imported for that day")
    @GetMapping("/cheapest")
    public PriceResponse cheapestHour(
            @Parameter(description = "Bidding zone", example = "LT") @RequestParam BiddingZone zone,
            @Parameter(description = "Day, local time. Default: today", example = "2026-11-11")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate day = dayOrToday(date);
        List<ElectricityPrice> prices = priceService.pricesForDay(zone, day);
        return priceService.cheapest(prices)
                .map(PriceResponse::from)
                .orElseThrow(() -> new NotFoundException("No prices for " + zone + " on " + day));
    }

    private static LocalDate dayOrToday(LocalDate date) {
        return date != null ? date : PowerHourTime.today();
    }
}