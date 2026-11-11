package com.bootcamp.powerhour.service;

import com.bootcamp.powerhour.dto.CheapestWindow;
import com.bootcamp.powerhour.dto.RunView;
import com.bootcamp.powerhour.entity.Appliance;
import com.bootcamp.powerhour.entity.BiddingZone;
import com.bootcamp.powerhour.entity.PlannedRun;
import com.bootcamp.powerhour.entity.PlannedRunStatus;
import com.bootcamp.powerhour.exception.NotFoundException;
import com.bootcamp.powerhour.repository.PlannedRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Checks planning, listing and status changes of planned runs.
 *
 * @author Karyna Litvinova
 */
@ExtendWith(MockitoExtension.class)
class PlannedRunServiceTest {

    private static final long USER_ID = 42L;

    // Thursday 5 November 2026, 02:00 in Riga
    private static final OffsetDateTime START =
            ZonedDateTime.of(2026, 11, 5, 2, 0, 0, 0, PowerHourTime.ZONE).toOffsetDateTime();

    @Mock
    private PlannedRunRepository plannedRunRepository;

    @InjectMocks
    private PlannedRunService service;

    private Appliance appliance;

    @BeforeEach
    void setUp() {
        appliance = new Appliance();
        appliance.setId(7L);
        appliance.setName("Dishwasher");
        appliance.setBiddingZone(BiddingZone.LV);
        appliance.setPowerKw(new BigDecimal("2.00"));
        appliance.setDurationMinutes(90);
        appliance.setAllowedFrom(LocalTime.MIDNIGHT);
        appliance.setAllowedTo(LocalTime.MIDNIGHT);
    }

    @Test
    void planSavesTheWindowAsAPlannedRun() {
        CheapestWindow window = window();
        when(plannedRunRepository.save(any(PlannedRun.class))).thenAnswer(call -> call.getArgument(0));

        PlannedRun run = service.plan(appliance, window);

        assertThat(run.getAppliance()).isSameAs(appliance);
        assertThat(run.getStartsAt()).isEqualTo(START);
        assertThat(run.getEndsAt()).isEqualTo(START.plusMinutes(90));
        assertThat(run.getEnergyKwh()).isEqualByComparingTo("3.00");
        assertThat(run.getEstimatedCost()).isEqualByComparingTo("0.40");
        assertThat(run.getStatus()).isEqualTo(PlannedRunStatus.PLANNED);
    }

    @Test
    void planStoresTheZoneOfTheAppliance() {
        appliance.setBiddingZone(BiddingZone.EE);
        when(plannedRunRepository.save(any(PlannedRun.class))).thenAnswer(call -> call.getArgument(0));

        PlannedRun run = service.plan(appliance, window());

        assertThat(run.getBiddingZone()).isEqualTo(BiddingZone.EE);
    }

    @Test
    void planKeepsItsZoneWhenTheApplianceMovesLater() {
        when(plannedRunRepository.save(any(PlannedRun.class))).thenAnswer(call -> call.getArgument(0));

        PlannedRun run = service.plan(appliance, window());
        appliance.setBiddingZone(BiddingZone.LT);

        assertThat(run.getBiddingZone()).isEqualTo(BiddingZone.LV);
    }

    @Test
    void planSavesExactlyOnce() {
        when(plannedRunRepository.save(any(PlannedRun.class))).thenAnswer(call -> call.getArgument(0));

        service.plan(appliance, window());

        ArgumentCaptor<PlannedRun> saved = ArgumentCaptor.forClass(PlannedRun.class);
        verify(plannedRunRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(PlannedRunStatus.PLANNED);
    }

    @Test
    void runsOfTurnsEachRunIntoAView() {
        PlannedRun run = run(3L, PlannedRunStatus.PLANNED);
        run.setEstimatedCost(new BigDecimal("0.4567"));
        when(plannedRunRepository.findByApplianceUserIdOrderByStartsAtAsc(USER_ID)).thenReturn(List.of(run));

        List<RunView> views = service.runsOf(USER_ID);

        assertThat(views).hasSize(1);
        RunView view = views.get(0);
        assertThat(view.id()).isEqualTo(3L);
        assertThat(view.applianceName()).isEqualTo("Dishwasher");
        assertThat(view.when()).isEqualTo("Thu 5 Nov, 02:00–03:30");
        assertThat(view.cost()).isEqualTo(new BigDecimal("0.46"));
        assertThat(view.status()).isEqualTo(PlannedRunStatus.PLANNED);
    }

    @Test
    void runsOfKeepsTheOrderFromTheRepository() {
        PlannedRun first = run(1L, PlannedRunStatus.COMPLETED);
        PlannedRun second = run(2L, PlannedRunStatus.PLANNED);
        second.setStartsAt(START.plusDays(1));
        second.setEndsAt(START.plusDays(1).plusMinutes(90));
        when(plannedRunRepository.findByApplianceUserIdOrderByStartsAtAsc(USER_ID))
                .thenReturn(List.of(first, second));

        assertThat(service.runsOf(USER_ID)).extracting(RunView::id).containsExactly(1L, 2L);
    }

    @Test
    void runsOfIsEmptyWhenTheUserHasNoRuns() {
        when(plannedRunRepository.findByApplianceUserIdOrderByStartsAtAsc(USER_ID)).thenReturn(List.of());

        assertThat(service.runsOf(USER_ID)).isEmpty();
    }

    @Test
    void nextPlannedReturnsTheComingRun() {
        when(plannedRunRepository.findFirstByApplianceUserIdAndStatusAndStartsAtAfterOrderByStartsAtAsc(
                eq(USER_ID), eq(PlannedRunStatus.PLANNED), any(OffsetDateTime.class)))
                .thenReturn(Optional.of(run(5L, PlannedRunStatus.PLANNED)));

        Optional<RunView> next = service.nextPlanned(USER_ID);

        assertThat(next).isPresent();
        assertThat(next.get().id()).isEqualTo(5L);
    }

    @Test
    void nextPlannedIsEmptyWhenNothingIsPlanned() {
        when(plannedRunRepository.findFirstByApplianceUserIdAndStatusAndStartsAtAfterOrderByStartsAtAsc(
                eq(USER_ID), eq(PlannedRunStatus.PLANNED), any(OffsetDateTime.class)))
                .thenReturn(Optional.empty());

        assertThat(service.nextPlanned(USER_ID)).isEmpty();
    }

    @Test
    void nextPlannedOnlyLooksFromNowOn() {
        OffsetDateTime before = OffsetDateTime.now();
        when(plannedRunRepository.findFirstByApplianceUserIdAndStatusAndStartsAtAfterOrderByStartsAtAsc(
                eq(USER_ID), eq(PlannedRunStatus.PLANNED), any(OffsetDateTime.class)))
                .thenReturn(Optional.empty());

        service.nextPlanned(USER_ID);
        OffsetDateTime after = OffsetDateTime.now();

        ArgumentCaptor<OffsetDateTime> from = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(plannedRunRepository).findFirstByApplianceUserIdAndStatusAndStartsAtAfterOrderByStartsAtAsc(
                eq(USER_ID), eq(PlannedRunStatus.PLANNED), from.capture());
        assertThat(from.getValue()).isBetween(before, after);
    }

    @Test
    void changeStatusCompletesAPlannedRun() {
        PlannedRun run = run(5L, PlannedRunStatus.PLANNED);
        when(plannedRunRepository.findByIdAndApplianceUserId(5L, USER_ID)).thenReturn(Optional.of(run));

        service.changeStatus(5L, USER_ID, PlannedRunStatus.COMPLETED);

        assertThat(run.getStatus()).isEqualTo(PlannedRunStatus.COMPLETED);
    }

    @Test
    void changeStatusCancelsAPlannedRun() {
        PlannedRun run = run(5L, PlannedRunStatus.PLANNED);
        when(plannedRunRepository.findByIdAndApplianceUserId(5L, USER_ID)).thenReturn(Optional.of(run));

        service.changeStatus(5L, USER_ID, PlannedRunStatus.CANCELED);

        assertThat(run.getStatus()).isEqualTo(PlannedRunStatus.CANCELED);
    }

    @Test
    void changeStatusFailsForAnUnknownOrForeignRun() {
        when(plannedRunRepository.findByIdAndApplianceUserId(99L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeStatus(99L, USER_ID, PlannedRunStatus.CANCELED))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void changeStatusDoesNotReopenACompletedRun() {
        PlannedRun run = run(5L, PlannedRunStatus.COMPLETED);
        when(plannedRunRepository.findByIdAndApplianceUserId(5L, USER_ID)).thenReturn(Optional.of(run));

        assertThatThrownBy(() -> service.changeStatus(5L, USER_ID, PlannedRunStatus.CANCELED))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(run.getStatus()).isEqualTo(PlannedRunStatus.COMPLETED);
    }

    @Test
    void changeStatusDoesNotChangeACanceledRun() {
        PlannedRun run = run(5L, PlannedRunStatus.CANCELED);
        when(plannedRunRepository.findByIdAndApplianceUserId(5L, USER_ID)).thenReturn(Optional.of(run));

        assertThatThrownBy(() -> service.changeStatus(5L, USER_ID, PlannedRunStatus.COMPLETED))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(run.getStatus()).isEqualTo(PlannedRunStatus.CANCELED);
    }

    private CheapestWindow window() {
        return new CheapestWindow(START, START.plusMinutes(90), new BigDecimal("3.0000"), new BigDecimal("0.4000"));
    }

    private PlannedRun run(long id, PlannedRunStatus status) {
        PlannedRun run = new PlannedRun();
        run.setId(id);
        run.setAppliance(appliance);
        run.setBiddingZone(BiddingZone.LV);
        run.setStartsAt(START);
        run.setEndsAt(START.plusMinutes(90));
        run.setEnergyKwh(new BigDecimal("3.0000"));
        run.setEstimatedCost(new BigDecimal("0.4000"));
        run.setStatus(status);
        return run;
    }
}