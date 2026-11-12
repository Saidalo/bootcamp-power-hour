package com.bootcamp.powerhour.controller;

import com.bootcamp.powerhour.config.SecurityConfig;
import com.bootcamp.powerhour.dto.RunView;
import com.bootcamp.powerhour.entity.PlannedRunStatus;
import com.bootcamp.powerhour.entity.User;
import com.bootcamp.powerhour.exception.NotFoundException;
import com.bootcamp.powerhour.service.CurrentUserService;
import com.bootcamp.powerhour.service.CustomUserDetailService;
import com.bootcamp.powerhour.service.PlannedRunService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Checks the "My runs" page and marking runs completed or canceled.
 *
 * @author Karyna Litvinova
 */
@WebMvcTest(RunController.class)
@Import(SecurityConfig.class)
class RunControllerTest {

    private static final long USER_ID = 42L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CurrentUserService currentUserService;

    @MockitoBean
    private PlannedRunService plannedRunService;

    @MockitoBean
    private CustomUserDetailService userDetailService;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(USER_ID);
        user.setUsername("ana");
        when(currentUserService.get(any())).thenReturn(user);
    }

    @Test
    void redirectsToLoginWhenNotSignedIn() throws Exception {
        mockMvc.perform(get("/runs").accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(username = "ana")
    void listsMyRuns() throws Exception {
        List<RunView> runs = List.of(
                new RunView(3L, "Dishwasher", "Thu 5 Nov, 02:00–03:30", new BigDecimal("0.40"), PlannedRunStatus.PLANNED),
                new RunView(4L, "Dryer", "Fri 6 Nov, 01:00–02:00", new BigDecimal("0.25"), PlannedRunStatus.COMPLETED),
                new RunView(5L, "Washer", "Sat 7 Nov, 03:00–04:00", new BigDecimal("0.30"), PlannedRunStatus.CANCELED));
        when(plannedRunService.runsOf(USER_ID)).thenReturn(runs);

        mockMvc.perform(get("/runs"))
                .andExpect(status().isOk())
                .andExpect(view().name("runs"))
                .andExpect(model().attribute("runs", runs));
    }

    @Test
    @WithMockUser(username = "ana")
    void showsTheEmptyPageWhenThereAreNoRuns() throws Exception {
        when(plannedRunService.runsOf(USER_ID)).thenReturn(List.of());

        mockMvc.perform(get("/runs"))
                .andExpect(status().isOk())
                .andExpect(view().name("runs"))
                .andExpect(model().attribute("runs", List.of()));
    }

    @Test
    @WithMockUser(username = "ana")
    void cancelsARun() throws Exception {
        mockMvc.perform(post("/runs/3/cancel").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/runs"));

        verify(plannedRunService).changeStatus(3L, USER_ID, PlannedRunStatus.CANCELED);
    }

    @Test
    @WithMockUser(username = "ana")
    void completesARun() throws Exception {
        mockMvc.perform(post("/runs/3/complete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/runs"));

        verify(plannedRunService).changeStatus(3L, USER_ID, PlannedRunStatus.COMPLETED);
    }

    @Test
    @WithMockUser(username = "ana")
    void showsNotFoundForSomeoneElsesRun() throws Exception {
        doThrow(new NotFoundException("Planned run", 99L))
                .when(plannedRunService).changeStatus(99L, USER_ID, PlannedRunStatus.CANCELED);

        mockMvc.perform(post("/runs/99/cancel").with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"));
    }

    @Test
    @WithMockUser(username = "ana")
    void refusesToCancelWithoutCsrfToken() throws Exception {
        mockMvc.perform(post("/runs/3/cancel"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(plannedRunService);
    }

    @Test
    @WithMockUser(username = "ana")
    void refusesToCompleteWithoutCsrfToken() throws Exception {
        mockMvc.perform(post("/runs/3/complete"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(plannedRunService);
    }

    @Test
    void refusesToChangeARunWhenNotSignedIn() throws Exception {
        mockMvc.perform(post("/runs/3/cancel").with(csrf()).accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verifyNoInteractions(plannedRunService);
    }
}