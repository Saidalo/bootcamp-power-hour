package com.bootcamp.powerhour.provider;

import com.bootcamp.powerhour.entity.BiddingZone;

import java.time.LocalDate;
import java.util.List;

public interface PriceProvider {
    List<HourlyPricePoint> fetchDayAhead(BiddingZone zone, LocalDate date);
}
