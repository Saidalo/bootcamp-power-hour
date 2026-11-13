package com.bootcamp.powerhour.service;

import com.bootcamp.powerhour.dto.CheapestWindow;
import com.bootcamp.powerhour.entity.Appliance;
import com.bootcamp.powerhour.entity.BiddingZone;
import com.bootcamp.powerhour.entity.ElectricityPrice;
import com.bootcamp.powerhour.exception.NoWindowAvailableException;
import com.bootcamp.powerhour.exception.PricesNotAvailableException;
import com.bootcamp.powerhour.repository.ElectricityPriceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * More cases for the cheapest-window calculation: allowed hours, other slot lengths,
 * windows across midnight and input the repository may hand back out of order.
 *
 * @author Karyna Litvinova
 */
@ExtendWith(MockitoExtension.class)
class CheapestWindowEdgeCasesTest {

    private static final OffsetDateTime DAY_START =
            ZonedDateTime.of(2026, 11, 5, 0, 0, 0, 0, PowerHourTime.ZONE).toOffsetDateTime();

    @Mock
    private ElectricityPriceRepository electricityPriceRepository;

    @InjectMocks
    private CheapestWindowService service;

    private Appliance appliance;

    @BeforeEach
    void setUp() {
        appliance = new Appliance();
        appliance.setId(1L);
        appliance.setName("Washing machine");
        appliance.setBiddingZone(BiddingZone.LV);
        appliance.setPowerKw(new BigDecimal("2.00"));
        appliance.setDurationMinutes(120);
        appliance.setAllowedFrom(LocalTime.MIDNIGHT);
        appliance.setAllowedTo(LocalTime.MIDNIGHT);
    }

    @ParameterizedTest
    @CsvSource({
            // moment, from, to, allowed
            "08:00, 08:00, 17:00, true",
            "16:59, 08:00, 17:00, true",
            "17:00, 08:00, 17:00, false",
            "07:59, 08:00, 17:00, false",
            "22:00, 22:00, 07:00, true",
            "23:30, 22:00, 07:00, true",
            "00:00, 22:00, 07:00, true",
            "06:59, 22:00, 07:00, true",
            "07:00, 22:00, 07:00, false",
            "12:00, 22:00, 07:00, false",
            "21:59, 22:00, 07:00, false"
    })
    void isAllowedTimeHandlesDayAndOvernightWindows(String moment, String from, String to, boolean allowed) {
        assertThat(CheapestWindowService.isAllowedTime(
                LocalTime.parse(moment), LocalTime.parse(from), LocalTime.parse(to)))
                .isEqualTo(allowed);
    }

    @Test
    void asksTheRepositoryForTheAppliancesZoneAndRange() {
        appliance.setBiddingZone(BiddingZone.EE);
        OffsetDateTime to = DAY_START.plusDays(1);
        when(electricityPriceRepository.findByBiddingZoneAndStartsAtBetween(BiddingZone.EE, DAY_START, to))
                .thenReturn(hourlyFrom(DAY_START, "0.10", "0.10", "0.10"));

        service.findCheapestWindow(appliance, DAY_START, to);

        verify(electricityPriceRepository).findByBiddingZoneAndStartsAtBetween(BiddingZone.EE, DAY_START, to);
    }

    @Test
    void oneSlotIsNotEnoughToWorkWith() {
        given(hourlyFrom(DAY_START, "0.10"));

        assertThatThrownBy(this::findWindow).isInstanceOf(PricesNotAvailableException.class);
    }

    @Test
    void worksWithQuarterHourSlots() {
        appliance.setDurationMinutes(30);
        given(slots(DAY_START, 15, "0.40", "0.10", "0.10", "0.40"));

        CheapestWindow window = findWindow();

        // 2 kW for 30 minutes = 1 kWh, half of it in each 0.10 slot
        assertThat(window.startsAt()).isEqualTo(DAY_START.plusMinutes(15));
        assertThat(window.endsAt()).isEqualTo(DAY_START.plusMinutes(45));
        assertThat(window.energyKwh()).isEqualByComparingTo("1.00");
        assertThat(window.estimatedCost()).isEqualByComparingTo("0.10");
    }

    @Test
    void aShortProgrammeFitsInOneSlot() {
        appliance.setPowerKw(new BigDecimal("1.50"));
        appliance.setDurationMinutes(45);
        given(hourlyFrom(DAY_START, "0.30", "0.20", "0.40"));

        CheapestWindow window = findWindow();

        // 1.5 kW for 45 minutes = 1.125 kWh, all at 0.20
        assertThat(window.startsAt()).isEqualTo(DAY_START.plusHours(1));
        assertThat(window.endsAt()).isEqualTo(DAY_START.plusHours(1).plusMinutes(45));
        assertThat(window.energyKwh()).isEqualByComparingTo("1.125");
        assertThat(window.estimatedCost()).isEqualByComparingTo("0.225");
    }

    @Test
    void aWindowMayRunPastMidnight() {
        given(hourlyFrom(DAY_START.plusHours(21), "0.50", "0.30", "0.05", "0.05", "0.50"));

        CheapestWindow window = findWindow();

        // 23:00 on 5 Nov until 01:00 on 6 Nov
        assertThat(window.startsAt()).isEqualTo(DAY_START.plusHours(23));
        assertThat(window.endsAt()).isEqualTo(DAY_START.plusHours(25));
        assertThat(window.estimatedCost()).isEqualByComparingTo("0.20");
    }

    @Test
    void anOvernightWindowMayRunPastMidnight() {
        appliance.setAllowedFrom(LocalTime.of(22, 0));
        appliance.setAllowedTo(LocalTime.of(7, 0));
        given(hourlyFrom(DAY_START.plusHours(22), "0.30", "0.05", "0.05", "0.30"));

        CheapestWindow window = findWindow();

        assertThat(window.startsAt()).isEqualTo(DAY_START.plusHours(23));
    }

    @Test
    void aRunMayEndExactlyWhenTheAllowedWindowCloses() {
        appliance.setAllowedFrom(LocalTime.of(1, 0));
        appliance.setAllowedTo(LocalTime.of(3, 0));
        given(hourlyFrom(DAY_START, "0.10", "0.10", "0.10", "0.10", "0.10"));

        CheapestWindow window = findWindow();

        assertThat(window.startsAt()).isEqualTo(DAY_START.plusHours(1));
        assertThat(window.endsAt()).isEqualTo(DAY_START.plusHours(3));
    }

    @Test
    void oneMinuteTooLongNoLongerFits() {
        appliance.setAllowedFrom(LocalTime.of(1, 0));
        appliance.setAllowedTo(LocalTime.of(3, 0));
        appliance.setDurationMinutes(121);
        given(hourlyFrom(DAY_START, "0.10", "0.10", "0.10", "0.10", "0.10"));

        assertThatThrownBy(this::findWindow).isInstanceOf(NoWindowAvailableException.class);
    }

    @Test
    void aWindowThatStartsBeforeTheAllowedHoursIsSkipped() {
        appliance.setAllowedFrom(LocalTime.of(2, 0));
        appliance.setAllowedTo(LocalTime.of(6, 0));
        // 00:00 and 01:00 are cheapest but not allowed
        given(hourlyFrom(DAY_START, "0.01", "0.01", "0.30", "0.30", "0.20", "0.20"));

        CheapestWindow window = findWindow();

        assertThat(window.startsAt()).isEqualTo(DAY_START.plusHours(4));
    }

    @Test
    void sortsSlotsTheRepositoryReturnedOutOfOrder() {
        List<ElectricityPrice> slots = new ArrayList<>(hourlyFrom(DAY_START, "0.30", "0.10", "0.10", "0.30"));
        List<ElectricityPrice> shuffled = List.of(slots.get(3), slots.get(1), slots.get(0), slots.get(2));
        given(shuffled);

        CheapestWindow window = findWindow();

        assertThat(window.startsAt()).isEqualTo(DAY_START.plusHours(1));
        assertThat(window.estimatedCost()).isEqualByComparingTo("0.40");
    }

    @Test
    void findsTheCheapestEvenWhenItIsTheLastPossibleWindow() {
        given(hourlyFrom(DAY_START, "0.30", "0.30", "0.30", "0.01", "0.01"));

        CheapestWindow window = findWindow();

        assertThat(window.startsAt()).isEqualTo(DAY_START.plusHours(3));
        assertThat(window.estimatedCost()).isEqualByComparingTo("0.04");
    }

    @Test
    void negativePricesMakeTheRunEarnMoney() {
        given(hourlyFrom(DAY_START, "0.10", "-0.05", "-0.05", "0.10"));

        CheapestWindow window = findWindow();

        assertThat(window.startsAt()).isEqualTo(DAY_START.plusHours(1));
        assertThat(window.estimatedCost()).isEqualByComparingTo("-0.20");
    }

    @Test
    void reportsTheWholeProgrammeInTheWindow() {
        appliance.setDurationMinutes(150);
        given(hourlyFrom(DAY_START, "0.10", "0.10", "0.10", "0.10"));

        CheapestWindow window = findWindow();

        assertThat(window.endsAt()).isEqualTo(window.startsAt().plusMinutes(150));
        assertThat(window.energyKwh()).isEqualByComparingTo("5.00");
    }

    @Test
    void aPartialLastQuarterIsChargedOnlyForItsMinutes() {
        appliance.setDurationMinutes(40);
        given(slots(DAY_START, 15, "0.10", "0.10", "0.10", "0.90"));

        CheapestWindow window = findWindow();

        // 15 + 15 + 10 minutes at 2 kW = 0.5 + 0.5 + 0.3333 kWh, all at 0.10
        assertThat(window.startsAt()).isEqualTo(DAY_START);
        assertThat(window.energyKwh()).isEqualByComparingTo("1.3333");
        assertThat(window.estimatedCost()).isEqualByComparingTo("0.1333");
    }

    @Test
    void aProgrammeLongerThanAllThePricesDoesNotFit() {
        appliance.setDurationMinutes(240);
        given(hourlyFrom(DAY_START, "0.10", "0.10", "0.10"));

        assertThatThrownBy(this::findWindow).isInstanceOf(NoWindowAvailableException.class);
    }

    @Test
    void aGapOnBothSidesLeavesNoWindow() {
        // every pair of slots is two hours apart, so no two of them are consecutive
        appliance.setDurationMinutes(180);
        given(List.of(
                price(DAY_START, "0.10"),
                price(DAY_START.plusHours(1), "0.10"),
                price(DAY_START.plusHours(3), "0.10"),
                price(DAY_START.plusHours(4), "0.10")));

        assertThatThrownBy(this::findWindow).isInstanceOf(NoWindowAvailableException.class);
    }

    @Test
    void anExactHourCostsOneFullSlot() {
        appliance.setDurationMinutes(60);
        given(hourlyFrom(DAY_START, "0.30", "0.15", "0.20"));

        CheapestWindow window = findWindow();

        assertThat(window.startsAt()).isEqualTo(DAY_START.plusHours(1));
        assertThat(window.energyKwh()).isEqualByComparingTo("2.00");
        assertThat(window.estimatedCost()).isEqualByComparingTo("0.30");
    }

    @Test
    void freeElectricityCostsNothing() {
        given(hourlyFrom(DAY_START, "0.20", "0.00", "0.00", "0.20"));

        CheapestWindow window = findWindow();

        assertThat(window.startsAt()).isEqualTo(DAY_START.plusHours(1));
        assertThat(window.estimatedCost()).isEqualByComparingTo("0");
    }

    @Test
    void aTieBetweenTwoDaysPicksTheFirstDay() {
        List<ElectricityPrice> twoDays = new ArrayList<>(hourlyFrom(DAY_START, "0.30", "0.10", "0.10", "0.30"));
        twoDays.addAll(hourlyFrom(DAY_START.plusDays(1), "0.30", "0.10", "0.10", "0.30"));
        given(twoDays);

        CheapestWindow window = findWindow();

        assertThat(window.startsAt()).isEqualTo(DAY_START.plusHours(1));
    }

    @Test
    void aCheaperWindowOnTheSecondDayWins() {
        List<ElectricityPrice> twoDays = new ArrayList<>(hourlyFrom(DAY_START, "0.30", "0.10", "0.10", "0.30"));
        twoDays.addAll(hourlyFrom(DAY_START.plusDays(1), "0.30", "0.05", "0.05", "0.30"));
        given(twoDays);

        CheapestWindow window = findWindow();

        assertThat(window.startsAt()).isEqualTo(DAY_START.plusDays(1).plusHours(1));
        assertThat(window.estimatedCost()).isEqualByComparingTo("0.20");
    }

    @Test
    void anAllowedWindowEndingAtMidnightStopsAtMidnight() {
        appliance.setAllowedFrom(LocalTime.of(18, 0));
        appliance.setAllowedTo(LocalTime.MIDNIGHT);
        // after midnight is much cheaper, but not allowed
        given(hourlyFrom(DAY_START.plusHours(20), "0.30", "0.10", "0.10", "0.10", "0.01", "0.01"));

        CheapestWindow window = findWindow();

        // 21:00 and 22:00 both cost 0.40; the earlier one wins
        assertThat(window.startsAt()).isEqualTo(DAY_START.plusHours(21));
        assertThat(window.endsAt()).isEqualTo(DAY_START.plusHours(23));
    }

    @Test
    void theSameFromAndToMeansAnyTime() {
        appliance.setAllowedFrom(LocalTime.of(8, 0));
        appliance.setAllowedTo(LocalTime.of(8, 0));
        given(hourlyFrom(DAY_START, "0.30", "0.10", "0.10", "0.30"));

        CheapestWindow window = findWindow();

        assertThat(window.startsAt()).isEqualTo(DAY_START.plusHours(1));
    }

    @Test
    void aRunOverTheSpringClockChangeLastsItsRealLength() {
        // On 29 March 2026 clocks in Riga jump from 03:00 to 04:00
        OffsetDateTime dayStart = PowerHourTime.startOfDay(java.time.LocalDate.of(2026, 3, 29));
        given(hourlyFrom(dayStart, "0.30", "0.30", "0.05", "0.05", "0.30"));

        CheapestWindow window = service.findCheapestWindow(appliance, dayStart, dayStart.plusDays(1));

        // starts at 02:00 on the wall clock and ends at 05:00, but runs exactly two hours
        assertThat(window.startsAt()).isEqualTo(dayStart.plusHours(2));
        assertThat(java.time.Duration.between(window.startsAt(), window.endsAt()).toMinutes()).isEqualTo(120);
        assertThat(PowerHourTime.time(window.startsAt())).isEqualTo("02:00");
        assertThat(PowerHourTime.time(window.endsAt())).isEqualTo("05:00");
    }

    @Test
    void allowedHoursAreReadOnTheRigaClockNotInUtc() {
        // allowed 00:00 – 02:00 in Riga is 22:00 – 00:00 UTC in winter
        appliance.setAllowedFrom(LocalTime.MIDNIGHT);
        appliance.setAllowedTo(LocalTime.of(2, 0));
        given(hourlyFrom(DAY_START.minusHours(2), "0.01", "0.01", "0.20", "0.20", "0.50"));

        CheapestWindow window = findWindow();

        // the 22:00 and 23:00 Riga slots are cheapest but outside the allowed hours
        assertThat(window.startsAt()).isEqualTo(DAY_START);
        assertThat(PowerHourTime.time(window.startsAt())).isEqualTo("00:00");
    }

    private CheapestWindow findWindow() {
        return service.findCheapestWindow(appliance, DAY_START, DAY_START.plusDays(2));
    }

    private void given(List<ElectricityPrice> slots) {
        when(electricityPriceRepository
                .findByBiddingZoneAndStartsAtBetween(eq(BiddingZone.LV), any(), any()))
                .thenReturn(slots);
    }

    private List<ElectricityPrice> hourlyFrom(OffsetDateTime first, String... prices) {
        return slots(first, 60, prices);
    }

    private ElectricityPrice price(OffsetDateTime startsAt, String eurPerKwh) {
        ElectricityPrice slot = new ElectricityPrice();
        slot.setBiddingZone(BiddingZone.LV);
        slot.setStartsAt(startsAt);
        slot.setPriceEurKwh(new BigDecimal(eurPerKwh));
        return slot;
    }

    private List<ElectricityPrice> slots(OffsetDateTime first, int minutes, String... prices) {
        List<ElectricityPrice> slots = new ArrayList<>();
        for (int i = 0; i < prices.length; i++) {
            ElectricityPrice slot = new ElectricityPrice();
            slot.setBiddingZone(BiddingZone.LV);
            slot.setStartsAt(first.plusMinutes((long) minutes * i));
            slot.setPriceEurKwh(new BigDecimal(prices[i]));
            slots.add(slot);
        }
        return slots;
    }
}
