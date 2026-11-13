package com.bootcamp.powerhour.repository;

import com.bootcamp.powerhour.entity.PlannedRun;
import com.bootcamp.powerhour.entity.PlannedRunStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
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

    List<PlannedRun> findByApplianceUserIdOrderByStartsAtAsc(long userId);

    Optional<PlannedRun> findFirstByApplianceUserIdAndStatusAndStartsAtAfterOrderByStartsAtAsc(
            long userId, PlannedRunStatus status, OffsetDateTime after);

    List<PlannedRun> findByApplianceUserIdAndStatusAndStartsAtGreaterThanEqualAndStartsAtLessThan(
            long userId, PlannedRunStatus status, OffsetDateTime from, OffsetDateTime to);
}