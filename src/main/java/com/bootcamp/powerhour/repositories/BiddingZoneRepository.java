package com.bootcamp.powerhour.repositories;

import com.bootcamp.powerhour.price.BiddingZone;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BiddingZoneRepository extends JpaRepository<BiddingZone, UUID> {
    Optional <BiddingZone> findByCode(String code);
    List<BiddingZone> findAllByActiveTrue();
}
