package com.bootcamp.powerhour.repository;

import com.bootcamp.powerhour.entity.Appliance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplianceRepository extends JpaRepository<Appliance, Long> {
    List<Appliance> findAllByUserIdAndIsActiveTrue(Long userId);
}
