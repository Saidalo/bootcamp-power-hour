package com.bootcamp.powerhour.repository;

import com.bootcamp.powerhour.entity.BiddingZone;
import com.bootcamp.powerhour.entity.ElectricityPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.OffsetDateTime;
import java.util.List;

public interface ElectricityPriceRepository extends JpaRepository<ElectricityPrice, Long> {
    List<ElectricityPrice> findByBiddingZoneAndStartsAtBetween(BiddingZone biddingZone, OffsetDateTime from, OffsetDateTime to);
    boolean existsByBiddingZoneAndStartsAt (BiddingZone biddingZone, OffsetDateTime startsAt);
}