package com.bootcamp.powerhour.repositories;

import com.bootcamp.powerhour.price.BiddingZone;
import com.bootcamp.powerhour.price.HourlyPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface HourlyPriceRepository extends JpaRepository<HourlyPrice, UUID> {
    List<HourlyPrice> findByZoneAndHourStartBetween (BiddingZone zone, Instant from, Instant to);
}
