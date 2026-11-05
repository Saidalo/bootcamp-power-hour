package com.bootcamp.powerhour.service;

import com.bootcamp.powerhour.dto.CheapestWindow;
import com.bootcamp.powerhour.entity.Appliance;
import com.bootcamp.powerhour.entity.ElectricityPrice;
import com.bootcamp.powerhour.exception.NoWindowAvailableException;
import com.bootcamp.powerhour.exception.PricesNotAvailableException;
import com.bootcamp.powerhour.repository.ElectricityPriceRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Finds the cheapest block of consecutive price slots an appliance can run in,
 * inside the time window its owner allows.
 *
 * @author Karyna Litvinova
 */
@Service
@RequiredArgsConstructor
public class CheapestWindowService {

    private static final Logger log = LoggerFactory.getLogger(CheapestWindowService.class);

    private static final int MINUTES_PER_HOUR = 60;
    private static final int ENERGY_SCALE = 4;
    private static final int COST_SCALE = 4;

    private final ElectricityPriceRepository electricityPriceRepository;

    public CheapestWindow findCheapestWindow(Appliance appliance,
                                             OffsetDateTime searchFrom,
                                             OffsetDateTime searchTo) {

        List<ElectricityPrice> slots = electricityPriceRepository
                .findByBiddingZoneAndStartsAtBetween(appliance.getBiddingZone(), searchFrom, searchTo)
                .stream()
                .sorted(Comparator.comparing(ElectricityPrice::getStartsAt))
                .toList();

        if (slots.size() < 2) {
            throw new PricesNotAvailableException(
                    "No prices imported for zone " + appliance.getBiddingZone()
                            + " between " + searchFrom + " and " + searchTo);
        }

        int slotMinutes = slotMinutesOf(slots);
        int slotsNeeded = (int) Math.ceil((double) appliance.getDurationMinutes() / slotMinutes);
        BigDecimal energyKwh = energyKwhOf(appliance);

        CheapestWindow cheapest = null;

        for (int first = 0; first + slotsNeeded <= slots.size(); first++) {
            if (!isContiguous(slots, first, slotsNeeded, slotMinutes)) {
                continue;
            }

            OffsetDateTime startsAt = slots.get(first).getStartsAt();
            OffsetDateTime endsAt = startsAt.plusMinutes(appliance.getDurationMinutes());

            if (!isInsideAllowedWindow(startsAt, endsAt, appliance.getAllowedFrom(), appliance.getAllowedTo())) {
                continue;
            }

            BigDecimal cost = costOf(slots, first, appliance, slotMinutes);

            if (cheapest == null || cost.compareTo(cheapest.estimatedCost()) < 0) {
                cheapest = new CheapestWindow(startsAt, endsAt, energyKwh, cost);
            }
        }

        if (cheapest == null) {
            throw new NoWindowAvailableException(
                    "No window of " + appliance.getDurationMinutes()
                            + " minutes fits between " + appliance.getAllowedFrom()
                            + " and " + appliance.getAllowedTo());
        }

        log.info("Cheapest window for appliance {} is {} - {} at {} EUR",
                appliance.getId(), cheapest.startsAt(), cheapest.endsAt(), cheapest.estimatedCost());
        return cheapest;
    }

    private int slotMinutesOf(List<ElectricityPrice> slots) {
        long shortest = Long.MAX_VALUE;
        for (int i = 1; i < slots.size(); i++) {
            long minutes = Duration.between(
                    slots.get(i - 1).getStartsAt(), slots.get(i).getStartsAt()).toMinutes();
            if (minutes > 0 && minutes < shortest) {
                shortest = minutes;
            }
        }
        return shortest == Long.MAX_VALUE ? MINUTES_PER_HOUR : (int) shortest;
    }

    private boolean isContiguous(List<ElectricityPrice> slots, int first, int count, int slotMinutes) {
        for (int i = 1; i < count; i++) {
            OffsetDateTime expected = slots.get(first + i - 1).getStartsAt().plusMinutes(slotMinutes);
            if (!slots.get(first + i).getStartsAt().isEqual(expected)) {
                return false;
            }
        }
        return true;
    }

    private boolean isInsideAllowedWindow(OffsetDateTime startsAt, OffsetDateTime endsAt,
                                          LocalTime allowedFrom, LocalTime allowedTo) {
        if (allowedFrom.equals(allowedTo)) {
            return true;
        }
        for (OffsetDateTime moment = startsAt; moment.isBefore(endsAt); moment = moment.plusMinutes(1)) {
            if (!isAllowedTime(moment.toLocalTime(), allowedFrom, allowedTo)) {
                return false;
            }
        }
        return true;
    }

    private boolean isAllowedTime(LocalTime moment, LocalTime allowedFrom, LocalTime allowedTo) {
        if (allowedFrom.isBefore(allowedTo)) {
            return !moment.isBefore(allowedFrom) && moment.isBefore(allowedTo);
        }
        return !moment.isBefore(allowedFrom) || moment.isBefore(allowedTo);
    }

    private BigDecimal energyKwhOf(Appliance appliance) {
        return appliance.getPowerKw()
                .multiply(BigDecimal.valueOf(appliance.getDurationMinutes()))
                .divide(BigDecimal.valueOf(MINUTES_PER_HOUR), ENERGY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal costOf(List<ElectricityPrice> slots, int first, Appliance appliance, int slotMinutes) {
        BigDecimal cost = BigDecimal.ZERO;
        int remaining = appliance.getDurationMinutes();

        for (int i = first; remaining > 0; i++) {
            int minutesUsed = Math.min(slotMinutes, remaining);
            BigDecimal kwh = appliance.getPowerKw()
                    .multiply(BigDecimal.valueOf(minutesUsed))
                    .divide(BigDecimal.valueOf(MINUTES_PER_HOUR), ENERGY_SCALE, RoundingMode.HALF_UP);
            cost = cost.add(slots.get(i).getPriceEurKwh().multiply(kwh));
            remaining -= minutesUsed;
        }
        return cost.setScale(COST_SCALE, RoundingMode.HALF_UP);
    }
}