package com.bootcamp.powerhour.repository;

import com.bootcamp.powerhour.entity.PlannedRun;
import com.bootcamp.powerhour.entity.PlannedRunStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Access to planned runs, always scoped to the owner of the appliance
 *
 * @author Karyna Litvinova
 */
public interface PlannedRunRepository extends JpaRepository<PlannedRun, Long> {

    List<PlannedRun> findByApplianceUserIdOrderByStartsAtDesc(long userId);

    Optional<PlannedRun> findByIdAndApplianceUserId(long id, long userId);

    List<PlannedRun> findByApplianceIdAndStatus(long applianceId, PlannedRunStatus status);
}