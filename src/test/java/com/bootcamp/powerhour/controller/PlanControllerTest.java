package com.bootcamp.powerhour.controller;

import com.bootcamp.powerhour.config.SecurityConfig;
import com.bootcamp.powerhour.dto.ApplianceResponse;
import com.bootcamp.powerhour.dto.CheapestWindow;
import com.bootcamp.powerhour.dto.PriceTick;
import com.bootcamp.powerhour.dto.SlotView;
import com.bootcamp.powerhour.entity.Appliance;
import com.bootcamp.powerhour.entity.BiddingZone;
import com.bootcamp.powerhour.entity.ElectricityPrice;
import com.bootcamp.powerhour.entity.User;
import com.bootcamp.powerhour.exception.ApplianceNotFoundException;
import com.bootcamp.powerhour.exception.NoWindowAvailableException;
import com.bootcamp.powerhour.exception.PricesNotAvailableException;
import com.bootcamp.powerhour.service.ApplianceService;
import com.bootcamp.powerhour.service.CheapestWindowService;
import com.bootcamp.powerhour.service.CurrentUserService;
import com.bootcamp.powerhour.service.CustomUserDetailService;
import com.bootcamp.powerhour.service.PlannedRunService;
import com.bootcamp.powerhour.service.PowerHourTime;
import com.bootcamp.powerhour.service.PriceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Checks the plan page: showing the cheapest window, the messages when there is none,
 * and saving a run.
 *
 * @author Karyna Litvinova
 */
@WebMvcTest(PlanController.class)
@Import(SecurityConfig.class)
class PlanControllerTest {

    private static final long USER_ID = 42L;

    private static final List<PriceTick> TICKS = List.of(
            new PriceTick("0.00", 0), new PriceTick("0.10", 33), new PriceTick("0.20", 67), new PriceTick("0.30", 100));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CurrentUserService currentUserService;

    @MockitoBean
    private ApplianceService applianceService;

    @MockitoBean
    private PriceService priceService;

    @MockitoBean
    private CheapestWindowService cheapestWindowService;

    @MockitoBean
    private PlannedRunService plannedRunService;

    @MockitoBean
    private CustomUserDetailService userDetailService;

    private LocalDate tomorrow;
    private OffsetDateTime tomorrowStart;
    private Appliance appliance;
    private CheapestWindow window;

    @BeforeEach
    void setUp() {
        tomorrow = PowerHourTime.today().plusDays(1);
        tomorrowStart = PowerHourTime.startOfDay(tomorrow);

        User user = new User();
        user.setId(USER_ID);
        user.setUsername("ana");
        when(currentUserService.get(any())).thenReturn(user);

        appliance = new Appliance();
        appliance.setId(1L);
        appliance.setUser(user);
        appliance.setName("Dishwasher");
        appliance.setBiddingZone(BiddingZone.LV);
        appliance.setPowerKw(new BigDecimal("2.00"));
        appliance.setDurationMinutes(90);
        appliance.setAllowedFrom(LocalTime.MIDNIGHT);
        appliance.setAllowedTo(LocalTime.MIDNIGHT);

        when(applianceService.getAppliancesByUserId(USER_ID)).thenReturn(List.of(new ApplianceResponse(
                1L, USER_ID, BiddingZone.LV, "Dishwasher", new BigDecimal("2.00"), 90,
                LocalTime.MIDNIGHT, LocalTime.MIDNIGHT)));
        when(applianceService.getOwned(1L, USER_ID)).thenReturn(appliance);

        // 02:00 – 03:30 tomorrow
        window = new CheapestWindow(tomorrowStart.plusHours(2), tomorrowStart.plusHours(2).plusMinutes(90),
                new BigDecimal("3.0000"), new BigDecimal("0.4000"));
    }

    @Test
    void redirectsToLoginWhenNotSignedIn() throws Exception {
        mockMvc.perform(get("/plan").accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(username = "ana")
    void showsTheFormWhenNoApplianceIsChosen() throws Exception {
        mockMvc.perform(get("/plan"))
                .andExpect(status().isOk())
                .andExpect(view().name("plan"))
                .andExpect(model().attributeExists("appliances"))
                .andExpect(model().attribute("day", "today"))
                .andExpect(model().attributeDoesNotExist("window", "error"));

        verifyNoInteractions(priceService, cheapestWindowService);
    }

    @Test
    @WithMockUser(username = "ana")
    void showsTheCheapestWindow() throws Exception {
        givenPricesTomorrow();
        when(cheapestWindowService.findCheapestWindow(eq(appliance), any(), any())).thenReturn(window);

        mockMvc.perform(get("/plan").param("applianceId", "1").param("day", "tomorrow"))
                .andExpect(status().isOk())
                .andExpect(view().name("plan"))
                .andExpect(model().attribute("selectedId", 1L))
                .andExpect(model().attribute("window", window))
                .andExpect(model().attribute("startLabel", PowerHourTime.time(window.startsAt())))
                .andExpect(model().attribute("endLabel", PowerHourTime.time(window.endsAt())))
                .andExpect(model().attribute("costLabel", new BigDecimal("0.40")))
                .andExpect(model().attribute("energyLabel", new BigDecimal("3.00")))
                .andExpect(model().attribute("durationLabel", "1h 30min"))
                .andExpect(model().attributeExists("slots"))
                .andExpect(model().attribute("ticks", TICKS))
                .andExpect(model().attributeDoesNotExist("error"));
    }

    @Test
    @WithMockUser(username = "ana")
    void showsThePriceChartOnThePage() throws Exception {
        givenPricesTomorrow();
        when(cheapestWindowService.findCheapestWindow(eq(appliance), any(), any())).thenReturn(window);

        mockMvc.perform(get("/plan").param("applianceId", "1").param("day", "tomorrow"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"price-chart\"")))
                .andExpect(content().string(containsString("Plan this run")));
    }

    @Test
    @WithMockUser(username = "ana")
    void showsTheAddApplianceHintWhenTheUserHasNone() throws Exception {
        when(applianceService.getAppliancesByUserId(USER_ID)).thenReturn(List.of());

        mockMvc.perform(get("/plan"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/appliances/new")));
    }

    @Test
    @WithMockUser(username = "ana")
    void searchesTheWholeOfTomorrow() throws Exception {
        givenPricesTomorrow();
        when(cheapestWindowService.findCheapestWindow(eq(appliance), any(), any())).thenReturn(window);

        mockMvc.perform(get("/plan").param("applianceId", "1").param("day", "tomorrow"))
                .andExpect(status().isOk());

        verify(cheapestWindowService).findCheapestWindow(
                appliance, tomorrowStart, PowerHourTime.startOfDay(tomorrow.plusDays(1)).minusSeconds(1));
    }

    @Test
    @WithMockUser(username = "ana")
    void marksTheBarsOfTheChosenWindow() throws Exception {
        List<ElectricityPrice> prices = givenPricesTomorrow();
        when(cheapestWindowService.findCheapestWindow(eq(appliance), any(), any())).thenReturn(window);

        mockMvc.perform(get("/plan").param("applianceId", "1").param("day", "tomorrow"))
                .andExpect(status().isOk());

        ArgumentCaptor<boolean[]> allowed = ArgumentCaptor.forClass(boolean[].class);
        ArgumentCaptor<boolean[]> chosen = ArgumentCaptor.forClass(boolean[].class);
        verify(priceService).toSlots(eq(prices), allowed.capture(), chosen.capture());
        // slots at 01:00, 02:00 and 03:00; the run is 02:00 – 03:30
        assertThat(allowed.getValue()).containsExactly(true, true, true);
        assertThat(chosen.getValue()).containsExactly(false, true, true);
    }

    @Test
    @WithMockUser(username = "ana")
    void greysOutHoursOutsideTheAllowedWindow() throws Exception {
        appliance.setAllowedFrom(LocalTime.of(2, 0));
        appliance.setAllowedTo(LocalTime.of(6, 0));
        givenPricesTomorrow();
        when(cheapestWindowService.findCheapestWindow(eq(appliance), any(), any())).thenReturn(window);

        mockMvc.perform(get("/plan").param("applianceId", "1").param("day", "tomorrow"))
                .andExpect(status().isOk());

        ArgumentCaptor<boolean[]> allowed = ArgumentCaptor.forClass(boolean[].class);
        verify(priceService).toSlots(any(), allowed.capture(), any());
        assertThat(allowed.getValue()).containsExactly(false, true, true);
    }

    @Test
    @WithMockUser(username = "ana")
    void saysTomorrowsPricesAreNotPublishedYet() throws Exception {
        when(priceService.pricesForDay(BiddingZone.LV, tomorrow)).thenReturn(List.of());

        mockMvc.perform(get("/plan").param("applianceId", "1").param("day", "tomorrow"))
                .andExpect(status().isOk())
                .andExpect(view().name("plan"))
                .andExpect(model().attribute("error", "Tomorrow's prices aren't published yet. Try again after 14:00."))
                .andExpect(model().attributeDoesNotExist("window"));

        verifyNoInteractions(cheapestWindowService);
    }

    @Test
    @WithMockUser(username = "ana")
    void saysTodaysPricesAreNotImportedYet() throws Exception {
        when(priceService.pricesForDay(BiddingZone.LV, PowerHourTime.today())).thenReturn(List.of());

        mockMvc.perform(get("/plan").param("applianceId", "1").param("day", "today"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("error", "Today's prices aren't imported yet."));
    }

    @Test
    @WithMockUser(username = "ana")
    void saysTheProgrammeIsLongerThanTheAllowedWindow() throws Exception {
        givenPricesTomorrow();
        when(cheapestWindowService.findCheapestWindow(eq(appliance), any(), any()))
                .thenThrow(new NoWindowAvailableException("does not fit"));

        mockMvc.perform(get("/plan").param("applianceId", "1").param("day", "tomorrow"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("error",
                        "This programme is longer than the window you allow. Widen it, or shorten the run."))
                .andExpect(model().attributeDoesNotExist("window"))
                .andExpect(model().attributeExists("slots"));
    }

    @Test
    @WithMockUser(username = "ana")
    void saysTodaysWindowHasPassedWhenItWouldFitEarlierInTheDay() throws Exception {
        givenPricesTomorrow();
        when(cheapestWindowService.findCheapestWindow(eq(appliance), any(), any()))
                .thenThrow(new NoWindowAvailableException("already passed"))
                .thenReturn(window);

        mockMvc.perform(get("/plan").param("applianceId", "1").param("day", "tomorrow"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("error", "Today's allowed window has already passed. Try Tomorrow."));
    }

    @Test
    @WithMockUser(username = "ana")
    void saysThereAreNotEnoughPricesLeft() throws Exception {
        givenPricesTomorrow();
        when(cheapestWindowService.findCheapestWindow(eq(appliance), any(), any()))
                .thenThrow(new PricesNotAvailableException("one slot left"));

        mockMvc.perform(get("/plan").param("applianceId", "1").param("day", "tomorrow"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("error", "Not enough prices left today. Try Tomorrow."));
    }

    @Test
    @WithMockUser(username = "ana")
    void doesNotShowSomeoneElsesAppliance() throws Exception {
        when(applianceService.getOwned(2L, USER_ID)).thenThrow(new ApplianceNotFoundException("Appliance 2 not found"));

        mockMvc.perform(get("/plan").param("applianceId", "2").param("day", "tomorrow"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"));

        verifyNoInteractions(priceService, cheapestWindowService);
    }

    @Test
    @WithMockUser(username = "ana")
    void savesTheRecalculatedWindowAndOpensMyRuns() throws Exception {
        when(cheapestWindowService.findCheapestWindow(eq(appliance), any(), any())).thenReturn(window);

        mockMvc.perform(post("/plan").with(csrf())
                        .param("applianceId", "1")
                        .param("day", "tomorrow")
                        .param("shownStartsAt", window.startsAt().toString())
                        .param("shownCost", window.estimatedCost().toPlainString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/runs"));

        verify(cheapestWindowService).findCheapestWindow(
                appliance, tomorrowStart, PowerHourTime.startOfDay(tomorrow.plusDays(1)).minusSeconds(1));
        verify(plannedRunService).plan(appliance, window);
    }

    @Test
    @WithMockUser(username = "ana")
    void savesWhenTheShownTimeIsWrittenWithAnotherOffset() throws Exception {
        when(cheapestWindowService.findCheapestWindow(eq(appliance), any(), any())).thenReturn(window);

        // the same moment, written in Riga time instead of UTC
        String inRigaTime = window.startsAt().atZoneSameInstant(PowerHourTime.ZONE).toOffsetDateTime().toString();

        mockMvc.perform(post("/plan").with(csrf())
                        .param("applianceId", "1")
                        .param("day", "tomorrow")
                        .param("shownStartsAt", inRigaTime)
                        .param("shownCost", "0.40"))
                .andExpect(redirectedUrl("/runs"));

        verify(plannedRunService).plan(appliance, window);
    }

    @Test
    @WithMockUser(username = "ana")
    void asksAgainWhenTheCheapestWindowMoved() throws Exception {
        when(cheapestWindowService.findCheapestWindow(eq(appliance), any(), any())).thenReturn(window);

        mockMvc.perform(post("/plan").with(csrf())
                        .param("applianceId", "1")
                        .param("day", "tomorrow")
                        .param("shownStartsAt", window.startsAt().minusHours(1).toString())
                        .param("shownCost", window.estimatedCost().toPlainString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/plan?applianceId=1&day=tomorrow"))
                .andExpect(flash().attribute("notice", PlanController.WINDOW_CHANGED));

        verifyNoInteractions(plannedRunService);
    }

    @Test
    @WithMockUser(username = "ana")
    void asksAgainWhenThePriceChanged() throws Exception {
        when(cheapestWindowService.findCheapestWindow(eq(appliance), any(), any())).thenReturn(window);

        mockMvc.perform(post("/plan").with(csrf())
                        .param("applianceId", "1")
                        .param("day", "tomorrow")
                        .param("shownStartsAt", window.startsAt().toString())
                        .param("shownCost", "0.3500"))
                .andExpect(redirectedUrl("/plan?applianceId=1&day=tomorrow"))
                .andExpect(flash().attribute("notice", PlanController.WINDOW_CHANGED));

        verifyNoInteractions(plannedRunService);
    }

    @Test
    @WithMockUser(username = "ana")
    void asksAgainWhenTheFormDidNotSayWhichWindowWasShown() throws Exception {
        when(cheapestWindowService.findCheapestWindow(eq(appliance), any(), any())).thenReturn(window);

        mockMvc.perform(post("/plan").with(csrf())
                        .param("applianceId", "1")
                        .param("day", "tomorrow"))
                .andExpect(redirectedUrl("/plan?applianceId=1&day=tomorrow"))
                .andExpect(flash().attribute("notice", PlanController.WINDOW_CHANGED));

        verifyNoInteractions(plannedRunService);
    }

    @Test
    @WithMockUser(username = "ana")
    void asksAgainWhenTheShownTimeIsNotADate() throws Exception {
        when(cheapestWindowService.findCheapestWindow(eq(appliance), any(), any())).thenReturn(window);

        mockMvc.perform(post("/plan").with(csrf())
                        .param("applianceId", "1")
                        .param("day", "tomorrow")
                        .param("shownStartsAt", "not-a-date")
                        .param("shownCost", "0.4000"))
                .andExpect(redirectedUrl("/plan?applianceId=1&day=tomorrow"));

        verifyNoInteractions(plannedRunService);
    }

    @Test
    @WithMockUser(username = "ana")
    void showsTheNoticeAfterTheWindowMoved() throws Exception {
        givenPricesTomorrow();
        when(cheapestWindowService.findCheapestWindow(eq(appliance), any(), any())).thenReturn(window);

        mockMvc.perform(get("/plan").param("applianceId", "1").param("day", "tomorrow")
                        .flashAttr("notice", PlanController.WINDOW_CHANGED))
                .andExpect(status().isOk())
                .andExpect(model().attribute("window", window))
                .andExpect(content().string(containsString("the cheapest window moved")));
    }

    @Test
    @WithMockUser(username = "ana")
    void theFormSendsTheWindowTheUserSaw() throws Exception {
        givenPricesTomorrow();
        when(cheapestWindowService.findCheapestWindow(eq(appliance), any(), any())).thenReturn(window);

        mockMvc.perform(get("/plan").param("applianceId", "1").param("day", "tomorrow"))
                .andExpect(content().string(containsString(
                        "name=\"shownStartsAt\" value=\"" + window.startsAt() + "\"")))
                .andExpect(content().string(containsString(
                        "name=\"shownCost\" value=\"" + window.estimatedCost() + "\"")));
    }

    @Test
    @WithMockUser(username = "ana")
    void refusesToSaveWithoutCsrfToken() throws Exception {
        mockMvc.perform(post("/plan")
                        .param("applianceId", "1")
                        .param("day", "tomorrow"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(plannedRunService);
    }

    @Test
    @WithMockUser(username = "ana")
    void doesNotSaveARunForSomeoneElsesAppliance() throws Exception {
        when(applianceService.getOwned(2L, USER_ID)).thenThrow(new ApplianceNotFoundException("Appliance 2 not found"));

        mockMvc.perform(post("/plan").with(csrf())
                        .param("applianceId", "2")
                        .param("day", "tomorrow"))
                .andExpect(status().isNotFound());

        verifyNoInteractions(plannedRunService);
    }

    /** Three hourly prices tomorrow: 01:00, 02:00, 03:00. */
    private List<ElectricityPrice> givenPricesTomorrow() {
        List<ElectricityPrice> prices = new ArrayList<>();
        String[] eurPerKwh = {"0.30", "0.10", "0.10"};
        for (int i = 0; i < eurPerKwh.length; i++) {
            ElectricityPrice price = new ElectricityPrice();
            price.setBiddingZone(BiddingZone.LV);
            price.setStartsAt(tomorrowStart.plusHours(1 + i));
            price.setPriceEurKwh(new BigDecimal(eurPerKwh[i]));
            prices.add(price);
        }
        when(priceService.pricesForDay(BiddingZone.LV, tomorrow)).thenReturn(prices);
        when(priceService.toSlots(any(), any(), any())).thenReturn(List.of(
                new SlotView("01:00", new BigDecimal("0.30"), true, false, 100, "01"),
                new SlotView("02:00", new BigDecimal("0.10"), true, true, 33, "02"),
                new SlotView("03:00", new BigDecimal("0.10"), true, true, 33, "03")));
        when(priceService.axisTicks(any())).thenReturn(TICKS);
        return prices;
    }
}