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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Checks the cheapest-window calculation on hand-built price slots.
 *
 * @author Karyna Litvinova
 */
@ExtendWith(MockitoExtension.class)
public class CheapestWindowServiceTest {

    private static final OffsetDateTime DAY_START =
            OffsetDateTime.of(2026, 11, 5, 0, 0, 0, 0, ZoneOffset.UTC);

    @Mock
    private ElectricityPriceRepository electricityPriceRepository;

    @InjectMocks
    private CheapestWindowService service;

    private Appliance appliance;

    @BeforeEach
    void setUp() {
        appliance = new Appliance();
        appliance.setId(1L);
        appliance.setName("Dishwasher");
        appliance.setBiddingZone(BiddingZone.LV);
        appliance.setPowerKw(new BigDecimal("2.00"));
        appliance.setDurationMinutes(120);
        appliance.setAllowedFrom(LocalTime.MIDNIGHT);
        appliance.setAllowedTo(LocalTime.MIDNIGHT);
    }

    @Test
    void picksTheCheapestPairOfHours() {
        givenHourlyPrices("0.30", "0.10", "0.10", "0.40");

        CheapestWindow window = findWindow();

        assertThat(window.startsAt()).isEqualTo(DAY_START.plusHours(1));
        assertThat(window.endsAt()).isEqualTo(DAY_START.plusHours(3));
        assertThat(window.energyKwh()).isEqualByComparingTo("4.00");
        assertThat(window.estimatedCost()).isEqualByComparingTo("0.40");
    }

    @Test
    void prefersTheEarlierWindowWhenPricesTie() {
        givenHourlyPrices("0.10", "0.10", "0.10", "0.10");

        assertThat(findWindow().startsAt()).isEqualTo(DAY_START);
    }

    @Test
    void staysInsideAnOvernightWindow() {
        appliance.setAllowedFrom(LocalTime.of(22, 0));
        appliance.setAllowedTo(LocalTime.of(7, 0));
        // 20:00 and 21:00 are the cheapest hours of the day, but are not allowed
        givenHourlyPricesFrom(DAY_START.plusHours(20), "0.01", "0.01", "0.20", "0.20", "0.30");

        CheapestWindow window = findWindow();

        assertThat(window.startsAt()).isEqualTo(DAY_START.plusHours(22));
        assertThat(window.estimatedCost()).isEqualByComparingTo("0.80");
    }

    @Test
    void neverSuggestsAWindowWithAMissingSlotInIt() {
        // 01:00 was not imported, so 00:00 and 02:00 are not consecutive in time
        given(List.of(
                price(DAY_START, "0.01"),
                price(DAY_START.plusHours(2), "0.01"),
                price(DAY_START.plusHours(3), "0.50"),
                price(DAY_START.plusHours(4), "0.50")));

        CheapestWindow window = findWindow();

        assertThat(window.startsAt()).isEqualTo(DAY_START.plusHours(2));
        assertThat(window.estimatedCost()).isEqualByComparingTo("1.02");
    }

    @Test
    void chargesOnlyTheMinutesUsedInThePartialSlot() {
        appliance.setDurationMinutes(90);
        givenHourlyPrices("0.10", "0.20", "0.90");

        CheapestWindow window = findWindow();

        // 2 kW for 90 minutes: a full hour at 0.10 plus half an hour at 0.20
        assertThat(window.energyKwh()).isEqualByComparingTo("3.00");
        assertThat(window.estimatedCost()).isEqualByComparingTo("0.40");
    }

    @Test
    void failsWhenNoPricesWereImported() {
        given(List.of());

        assertThatThrownBy(this::findWindow)
                .isInstanceOf(PricesNotAvailableException.class);
    }

    @Test
    void failsWhenTheProgrammeIsLongerThanTheAllowedWindow() {
        appliance.setDurationMinutes(300);
        appliance.setAllowedFrom(LocalTime.of(1, 0));
        appliance.setAllowedTo(LocalTime.of(3, 0));
        givenHourlyPrices("0.10", "0.10", "0.10", "0.10", "0.10", "0.10");

        assertThatThrownBy(this::findWindow)
                .isInstanceOf(NoWindowAvailableException.class);
    }

    private CheapestWindow findWindow() {
        return service.findCheapestWindow(appliance, DAY_START, DAY_START.plusDays(2));
    }

    private void givenHourlyPrices(String... prices) {
        givenHourlyPricesFrom(DAY_START, prices);
    }

    private void givenHourlyPricesFrom(OffsetDateTime first, String... prices) {
        List<ElectricityPrice> slots = new ArrayList<>();
        for (int i = 0; i < prices.length; i++) {
            slots.add(price(first.plusHours(i), prices[i]));
        }
        given(slots);
    }

    private void given(List<ElectricityPrice> slots) {
        when(electricityPriceRepository
                .findByBiddingZoneAndStartsAtBetween(eq(BiddingZone.LV), any(), any()))
                .thenReturn(slots);
    }

    private ElectricityPrice price(OffsetDateTime startsAt, String eurPerKwh) {
        ElectricityPrice slot = new ElectricityPrice();
        slot.setBiddingZone(BiddingZone.LV);
        slot.setStartsAt(startsAt);
        slot.setPriceEurKwh(new BigDecimal(eurPerKwh));
        return slot;
    }
}
