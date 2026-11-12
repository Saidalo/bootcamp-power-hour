package com.bootcamp.powerhour.controller;

import com.bootcamp.powerhour.config.SecurityConfig;
import com.bootcamp.powerhour.dto.SavingsSummary;
import com.bootcamp.powerhour.entity.User;
import com.bootcamp.powerhour.service.CurrentUserService;
import com.bootcamp.powerhour.service.CustomUserDetailService;
import com.bootcamp.powerhour.service.PowerHourTime;
import com.bootcamp.powerhour.service.SavingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.YearMonth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Checks the savings page picks the right month for the signed-in user.
 *
 * @author Karyna Litvinova
 */
@WebMvcTest(SavingsController.class)
@Import(SecurityConfig.class)
class SavingsControllerTest {

    private static final long USER_ID = 42L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CurrentUserService currentUserService;

    @MockitoBean
    private SavingsService savingsService;

    @MockitoBean
    private CustomUserDetailService userDetailService;

    private SavingsSummary summary;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(USER_ID);
        user.setUsername("ana");
        when(currentUserService.get(any())).thenReturn(user);

        summary = new SavingsSummary(new BigDecimal("0.40"), new BigDecimal("1.00"), new BigDecimal("1.60"),
                new BigDecimal("0.60"), 1, new BigDecimal("4.0"), 0);
        when(savingsService.forMonth(anyLong(), any(YearMonth.class))).thenReturn(summary);
    }

    @Test
    void redirectsToLoginWhenNotSignedIn() throws Exception {
        mockMvc.perform(get("/savings").accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(username = "ana")
    void showsThisMonthByDefault() throws Exception {
        YearMonth thisMonth = YearMonth.from(PowerHourTime.today());

        mockMvc.perform(get("/savings"))
                .andExpect(status().isOk())
                .andExpect(view().name("savings"))
                .andExpect(model().attribute("month", thisMonth))
                .andExpect(model().attribute("summary", summary));

        verify(savingsService).forMonth(USER_ID, thisMonth);
    }

    @Test
    @WithMockUser(username = "ana")
    void showsTheChosenMonth() throws Exception {
        mockMvc.perform(get("/savings").param("month", "2026-10"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("month", YearMonth.of(2026, 10)));

        verify(savingsService).forMonth(eq(USER_ID), eq(YearMonth.of(2026, 10)));
    }

    @Test
    @WithMockUser(username = "ana")
    void showsRunsLeftOutForMissingPrices() throws Exception {
        SavingsSummary withGaps = new SavingsSummary(new BigDecimal("0.40"), new BigDecimal("1.00"),
                new BigDecimal("1.60"), new BigDecimal("0.60"), 1, new BigDecimal("4.0"), 2);
        when(savingsService.forMonth(anyLong(), any(YearMonth.class))).thenReturn(withGaps);

        mockMvc.perform(get("/savings"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("summary", withGaps));
    }
}