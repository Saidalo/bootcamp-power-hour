package com.bootcamp.powerhour.service;

import com.bootcamp.powerhour.dto.CheapestWindow;
import com.bootcamp.powerhour.dto.RunView;
import com.bootcamp.powerhour.entity.Appliance;
import com.bootcamp.powerhour.entity.PlannedRun;
import com.bootcamp.powerhour.entity.PlannedRunStatus;
import com.bootcamp.powerhour.exception.NotFoundException;
import com.bootcamp.powerhour.repository.PlannedRunRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlannedRunService {
    private final PlannedRunRepository plannedRunRepository;

    @Transactional
    public PlannedRun plan(Appliance appliance, CheapestWindow window) {
        PlannedRun run = new PlannedRun();
        run.setAppliance(appliance);
        run.setStartsAt(window.startsAt());
        run.setEndsAt(window.endsAt());
        run.setEstimatedCost(window.estimatedCost());
        run.setEnergyKwh(window.energyKwh());
        run.setBiddingZone(appliance.getBiddingZone());
        run.setStatus(PlannedRunStatus.PLANNED);
        PlannedRun saved = plannedRunRepository.save(run);
        log.info("Run planned: id={} appliance={} start={}", saved.getId(), appliance.getId(), window.startsAt());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<RunView> runsOf(long userId) {
        return plannedRunRepository.findByApplianceUserIdOrderByStartsAtAsc(userId).stream()
                .map(this::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<RunView> nextPlanned(long userId) {
        return plannedRunRepository.findFirstByApplianceUserIdAndStatusAndStartsAtAfterOrderByStartsAtAsc(
                userId, PlannedRunStatus.PLANNED, OffsetDateTime.now()).map(this::toView);
    }

    @Transactional
    public void changeStatus(long runId, long userId, PlannedRunStatus status) {
        PlannedRun run = plannedRunRepository.findByIdAndApplianceUserId(runId, userId)
                .orElseThrow(() -> new NotFoundException("Planned run", runId));
        if (run.getStatus() != PlannedRunStatus.PLANNED) {
            throw new IllegalArgumentException("Only planned runs can be changed");
        }
        run.setStatus(status);
        log.info("Run {} marked {}", runId, status);
    }

    private RunView toView(PlannedRun run) {
        String when = PowerHourTime.dayTime(run.getStartsAt()) + "–" + PowerHourTime.time(run.getEndsAt());
        return new RunView(run.getId(), run.getAppliance().getName(), when,
                run.getEstimatedCost().setScale(2, RoundingMode.HALF_UP), run.getStatus());
    }
}