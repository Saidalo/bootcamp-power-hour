package com.bootcamp.powerhour.repository;

import com.bootcamp.powerhour.entity.BiddingZone;
import com.bootcamp.powerhour.entity.ElectricityPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

public interface ElectricityPriceRepository extends JpaRepository<ElectricityPrice, Long> {
    List<ElectricityPrice> findByBiddingZoneAndStartsAtBetween(BiddingZone biddingZone, OffsetDateTime from, OffsetDateTime to);

    @Query("""
        SELECT p FROM ElectricityPrice p
        WHERE p.biddingZone = :zone
        AND p.startsAt >= :from
        AND p.startsAt < :to
        ORDER BY p.startsAt ASC 
     """
    )
    List<ElectricityPrice> findInRange(@Param("zone") BiddingZone biddingZone, @Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    @Query("""
        SELECT COUNT(p) > 0 FROM ElectricityPrice p
        WHERE p.biddingZone = :zone
          AND p.startsAt >= :from
          AND p.startsAt < :to
    """)
    boolean existsInRange(@Param("zone") BiddingZone biddingZone, @Param("from") OffsetDateTime from, @Param ("to") OffsetDateTime to);
}