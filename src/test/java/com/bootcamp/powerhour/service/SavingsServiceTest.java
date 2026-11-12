package com.bootcamp.powerhour.service;

import com.bootcamp.powerhour.dto.SavingsSummary;
import com.bootcamp.powerhour.entity.Appliance;
import com.bootcamp.powerhour.entity.BiddingZone;
import com.bootcamp.powerhour.entity.ElectricityPrice;
import com.bootcamp.powerhour.entity.PlannedRun;
import com.bootcamp.powerhour.entity.PlannedRunStatus;
import com.bootcamp.powerhour.repository.PlannedRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Checks the monthly savings summary: what was paid against the day's average and peak price.
 *
 * @author Karyna Litvinova
 */
@ExtendWith(MockitoExtension.class)
class SavingsServiceTest {

    private static final long USER_ID = 42L;
    private static final YearMonth NOVEMBER = YearMonth.of(2026, 11);
    private static final LocalDate DAY = LocalDate.of(2026, 11, 5);
    private static final OffsetDateTime START =
            ZonedDateTime.of(2026, 11, 5, 2, 0, 0, 0, PowerHourTime.ZONE).toOffsetDateTime();

    @Mock
    private PlannedRunRepository plannedRunRepository;

    @Mock
    private PriceService priceService;

    @InjectMocks
    private SavingsService service;

    private Appliance appliance;

    @BeforeEach
    void setUp() {
        // 2 kW for 2 hours = 4 kWh
        appliance = new Appliance();
        appliance.setId(7L);
        appliance.setName("Dishwasher");
        appliance.setBiddingZone(BiddingZone.LV);
        appliance.setPowerKw(new BigDecimal("2.00"));
        appliance.setDurationMinutes(120);
        appliance.setAllowedFrom(LocalTime.MIDNIGHT);
        appliance.setAllowedTo(LocalTime.MIDNIGHT);
    }

    @Test
    void isAllZeroWhenNoRunWasCompleted() {
        givenCompletedRuns();

        SavingsSummary summary = service.forMonth(USER_ID, NOVEMBER);

        assertThat(summary.paid()).isEqualByComparingTo("0");
        assertThat(summary.atAverage()).isEqualByComparingTo("0");
        assertThat(summary.atPeak()).isEqualByComparingTo("0");
        assertThat(summary.saved()).isEqualByComparingTo("0");
        assertThat(summary.energyKwh()).isEqualByComparingTo("0");
        assertThat(summary.completedRuns()).isZero();
        assertThat(summary.runsWithoutPrices()).isZero();
    }

    @Test
    void comparesWhatWasPaidWithTheDaysAverageAndPeak() {
        givenCompletedRuns(completedRun(START, "0.40"));
        givenPricesOn(DAY, "0.10", "0.20", "0.30", "0.40");

        SavingsSummary summary = service.forMonth(USER_ID, NOVEMBER);

        // average 0.25 and peak 0.40, times 4 kWh
        assertThat(summary.paid()).isEqualByComparingTo("0.40");
        assertThat(summary.atAverage()).isEqualByComparingTo("1.00");
        assertThat(summary.atPeak()).isEqualByComparingTo("1.60");
        assertThat(summary.saved()).isEqualByComparingTo("0.60");
        assertThat(summary.energyKwh()).isEqualByComparingTo("4.0");
        assertThat(summary.completedRuns()).isEqualTo(1);
    }

    @Test
    void addsUpSeveralRuns() {
        givenCompletedRuns(completedRun(START, "0.40"), completedRun(START.plusHours(5), "0.60"));
        givenPricesOn(DAY, "0.10", "0.20", "0.30", "0.40");

        SavingsSummary summary = service.forMonth(USER_ID, NOVEMBER);

        assertThat(summary.paid()).isEqualByComparingTo("1.00");
        assertThat(summary.atAverage()).isEqualByComparingTo("2.00");
        assertThat(summary.atPeak()).isEqualByComparingTo("3.20");
        assertThat(summary.saved()).isEqualByComparingTo("1.00");
        assertThat(summary.energyKwh()).isEqualByComparingTo("8.0");
        assertThat(summary.completedRuns()).isEqualTo(2);
    }

    @Test
    void savedIsNegativeWhenTheRunCostMoreThanTheAverage() {
        givenCompletedRuns(completedRun(START, "1.20"));
        givenPricesOn(DAY, "0.10", "0.20", "0.30", "0.40");

        SavingsSummary summary = service.forMonth(USER_ID, NOVEMBER);

        assertThat(summary.saved()).isEqualByComparingTo("-0.20");
    }

    @Test
    void leavesOutRunsOnDaysWithoutPrices() {
        givenCompletedRuns(completedRun(START, "0.40"));
        when(priceService.pricesForDay(BiddingZone.LV, DAY)).thenReturn(List.of());

        SavingsSummary summary = service.forMonth(USER_ID, NOVEMBER);

        assertThat(summary.runsWithoutPrices()).isEqualTo(1);
        assertThat(summary.completedRuns()).isZero();
        assertThat(summary.paid()).isEqualByComparingTo("0");
    }

    @Test
    void countsRunsWithAndWithoutPricesSeparately() {
        LocalDate nextDay = DAY.plusDays(1);
        givenCompletedRuns(completedRun(START, "0.40"), completedRun(START.plusDays(1), "0.50"));
        givenPricesOn(DAY, "0.10", "0.20", "0.30", "0.40");
        when(priceService.pricesForDay(BiddingZone.LV, nextDay)).thenReturn(List.of());

        SavingsSummary summary = service.forMonth(USER_ID, NOVEMBER);

        assertThat(summary.completedRuns()).isEqualTo(1);
        assertThat(summary.runsWithoutPrices()).isEqualTo(1);
        assertThat(summary.paid()).isEqualByComparingTo("0.40");
    }

    @Test
    void asksOnlyForCompletedRunsOfThatMonth() {
        givenCompletedRuns();

        service.forMonth(USER_ID, NOVEMBER);

        verify(plannedRunRepository).findByApplianceUserIdAndStatusAndStartsAtGreaterThanEqualAndStartsAtLessThan(
                USER_ID,
                PlannedRunStatus.COMPLETED,
                PowerHourTime.startOfDay(LocalDate.of(2026, 11, 1)),
                PowerHourTime.startOfDay(LocalDate.of(2026, 12, 1)));
    }

    @Test
    void usesThePricesOfTheLocalDayTheRunStarted() {
        // 23:30 in Riga is still 5 November there, but already 21:30 UTC
        OffsetDateTime lateEvening = ZonedDateTime.of(2026, 11, 5, 23, 30, 0, 0, PowerHourTime.ZONE)
                .toOffsetDateTime();
        givenCompletedRuns(completedRun(lateEvening, "0.40"));
        givenPricesOn(DAY, "0.10", "0.20");

        service.forMonth(USER_ID, NOVEMBER);

        verify(priceService).pricesForDay(BiddingZone.LV, DAY);
    }

    @Test
    void roundsMoneyToCents() {
        givenCompletedRuns(completedRun(START, "0.123456"));
        givenPricesOn(DAY, "0.10", "0.20", "0.30", "0.40");

        SavingsSummary summary = service.forMonth(USER_ID, NOVEMBER);

        assertThat(summary.paid()).isEqualTo(new BigDecimal("0.12"));
        assertThat(summary.atAverage().scale()).isEqualTo(2);
        assertThat(summary.atPeak().scale()).isEqualTo(2);
        assertThat(summary.saved().scale()).isEqualTo(2);
    }

    @Test
    void roundsEnergyToOneDecimal() {
        givenCompletedRuns(completedRun(START, "0.40"));
        givenPricesOn(DAY, "0.10", "0.20");

        SavingsSummary summary = service.forMonth(USER_ID, NOVEMBER);

        assertThat(summary.energyKwh().scale()).isEqualTo(1);
    }

    @Disabled("Turn on after SavingsService reads energyKwh from the run (mentor review)")
    @Test
    void usesTheEnergyStoredOnTheRunNotTheEditedAppliance() {
        givenCompletedRuns(completedRun(START, "0.40"));
        givenPricesOn(DAY, "0.10", "0.20", "0.30", "0.40");
        // the appliance was edited after the run: 4 kWh were used, not 6
        appliance.setPowerKw(new BigDecimal("3.00"));

        SavingsSummary summary = service.forMonth(USER_ID, NOVEMBER);

        assertThat(summary.energyKwh()).isEqualByComparingTo("4.0");
        assertThat(summary.atAverage()).isEqualByComparingTo("1.00");
    }

    @Disabled("Turn on after SavingsService reads the bidding zone from the run (mentor review)")
    @Test
    void usesTheZoneStoredOnTheRunNotTheMovedAppliance() {
        givenCompletedRuns(completedRun(START, "0.40"));
        givenPricesOn(DAY, "0.10", "0.20", "0.30", "0.40");
        appliance.setBiddingZone(BiddingZone.EE);

        service.forMonth(USER_ID, NOVEMBER);

        verify(priceService).pricesForDay(BiddingZone.LV, DAY);
    }

    private void givenCompletedRuns(PlannedRun... runs) {
        when(plannedRunRepository.findByApplianceUserIdAndStatusAndStartsAtGreaterThanEqualAndStartsAtLessThan(
                eq(USER_ID), eq(PlannedRunStatus.COMPLETED), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(List.of(runs));
    }

    private void givenPricesOn(LocalDate day, String... eurPerKwh) {
        List<ElectricityPrice> prices = new ArrayList<>();
        OffsetDateTime first = PowerHourTime.startOfDay(day);
        for (int i = 0; i < eurPerKwh.length; i++) {
            ElectricityPrice price = new ElectricityPrice();
            price.setBiddingZone(BiddingZone.LV);
            price.setStartsAt(first.plusHours(i));
            price.setPriceEurKwh(new BigDecimal(eurPerKwh[i]));
            prices.add(price);
        }
        when(priceService.pricesForDay(BiddingZone.LV, day)).thenReturn(prices);
    }

    private PlannedRun completedRun(OffsetDateTime startsAt, String cost) {
        PlannedRun run = new PlannedRun();
        run.setAppliance(appliance);
        run.setBiddingZone(BiddingZone.LV);
        run.setStartsAt(startsAt);
        run.setEndsAt(startsAt.plusMinutes(appliance.getDurationMinutes()));
        run.setEnergyKwh(new BigDecimal("4.0000"));
        run.setEstimatedCost(new BigDecimal(cost));
        run.setStatus(PlannedRunStatus.COMPLETED);
        return run;
    }
}