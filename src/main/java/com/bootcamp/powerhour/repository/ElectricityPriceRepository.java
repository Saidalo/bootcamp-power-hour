package com.bootcamp.powerhour.repository;

import com.bootcamp.powerhour.entity.BiddingZone;
import com.bootcamp.powerhour.entity.ElectricityPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

public interface ElectricityPriceRepository extends JpaRepository<ElectricityPrice, Long> {
    List<ElectricityPrice> findByBiddingZoneAndStartsAtBetween(BiddingZone biddingZone, OffsetDateTime from, OffsetDateTime to);
    boolean existsByBiddingZoneAndStartsAt (BiddingZone biddingZone, OffsetDateTime startsAt);

    List<ElectricityPrice> findByBiddingZoneAndStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAt(
            BiddingZone biddingZone, OffsetDateTime from, OffsetDateTime to);

    boolean existsByBiddingZoneAndStartsAtGreaterThanEqualAndStartsAtLessThan(
            BiddingZone biddingZone, OffsetDateTime from, OffsetDateTime to);

    @Transactional
    void deleteByBiddingZoneAndStartsAtGreaterThanEqualAndStartsAtLessThan(
            BiddingZone biddingZone, OffsetDateTime from, OffsetDateTime to);
}