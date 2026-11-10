package com.bootcamp.powerhour.controller;

import com.bootcamp.powerhour.config.SecurityConfig;
import com.bootcamp.powerhour.dto.ApplianceForm;
import com.bootcamp.powerhour.entity.Appliance;
import com.bootcamp.powerhour.entity.User;
import com.bootcamp.powerhour.exception.ApplianceNotFoundException;
import com.bootcamp.powerhour.service.ApplianceService;
import com.bootcamp.powerhour.service.CurrentUserService;
import com.bootcamp.powerhour.service.CustomUserDetailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(ApplianceController.class)
@Import(SecurityConfig.class)
public class ApplianceControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApplianceService applianceService;

    @MockitoBean
    private CurrentUserService currentUserService;

    @MockitoBean
    private CustomUserDetailService userDetailService;

    @Test
    void list_shouldRedirectToLoginWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/appliances").accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void list_shouldFetchAppliancesForAuthenticatedUser() throws Exception {
        User currentUser = new User();
        currentUser.setId(1L);

        when(currentUserService.get(any())).thenReturn(currentUser);
        when(applianceService.getAppliancesByUserId(1L)).thenReturn(List.of());

        mockMvc.perform(get("/appliances").with(user("testuser")))
                .andExpect(status().isOk())
                .andExpect(view().name("appliances"))
                .andExpect(model().attribute("appliances", List.of()));

        verify(applianceService).getAppliancesByUserId(1L);
        verifyNoMoreInteractions(applianceService);
    }

    @Test
    void newForm_shouldDisplayEmptyApplianceForm() throws Exception {
        mockMvc.perform(get("/appliances/new")
                        .with(user("testuser")))
                .andExpect(status().isOk())
                .andExpect(view().name("appliance-form"))
                .andExpect(model().attributeExists("form"))
                .andExpect(model().attribute("form", instanceOf(ApplianceForm.class)))
                .andExpect(model().attribute("applianceId", nullValue()));

        verifyNoInteractions(applianceService);
        verifyNoInteractions(currentUserService);
    }

    @Test
    void editForm_shouldDisplayOwnedAppliance() throws Exception {
        User currentUser = new User();
        currentUser.setId(1L);

        Appliance appliance = new Appliance();
        appliance.setId(10L);

        ApplianceForm form = new ApplianceForm();
        form.setName("Dishwasher");

        when(currentUserService.get(any())).thenReturn(currentUser);
        when(applianceService.getOwned(10L, 1L)).thenReturn(appliance);
        when(applianceService.toForm(appliance)).thenReturn(form);

        mockMvc.perform(get("/appliances/10/edit").with(user("testuser")))
                .andExpect(status().isOk())
                .andExpect(view().name("appliance-form"))
                .andExpect(model().attribute("form", sameInstance(form)))
                .andExpect(model().attribute("applianceId", 10L));

        verify(applianceService).getOwned(10L, 1L);
        verify(applianceService).toForm(appliance);
    }

    @Test
    void editForm_shouldReturn404WhenApplianceNotOwned() throws Exception {
        User currentUser = new User();
        currentUser.setId(1L);

        when(currentUserService.get(any())).thenReturn(currentUser);
        when(applianceService.getOwned(10L, 1L))
                .thenThrow(new ApplianceNotFoundException("Appliance with id 10 not found."));

        mockMvc.perform(get("/appliances/10/edit").with(user("testuser")))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("status", 404))
                .andExpect(model().attribute("error", "Not Found"));

        verify(applianceService).getOwned(10L, 1L);
        verify(applianceService, never()).toForm(any(Appliance.class));
    }

    @Test
    void create_shouldSaveApplianceWhenFormIsValid() throws Exception {
        User currentUser = new User();
        currentUser.setId(1L);

        when(currentUserService.get(any())).thenReturn(currentUser);

        mockMvc.perform(post("/appliances")
                        .with(user("testuser"))
                        .with(csrf())
                        .param("biddingZone", "LV")
                        .param("name", "Dishwasher")
                        .param("powerKw", "2.5")
                        .param("durationMinutes", "90")
                        .param("allowedFrom", "17:00")
                        .param("allowedTo", "23:00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/appliances"));

        verify(applianceService).save(isNull(), any(ApplianceForm.class), same(currentUser));
    }

    @Test
    void create_shouldReturnFormWhenValidationFails() throws Exception {
        mockMvc.perform(post("/appliances")
                        .with(user("testuser"))
                        .with(csrf())
                        .param("biddingZone", "LV")
                        .param("name", "  ")
                        .param("powerKw", "2.5")
                        .param("durationMinutes", "90")
                        .param("allowedFrom", "17:00")
                        .param("allowedTo", "23:00"))
                .andExpect(status().isOk())
                .andExpect(view().name("appliance-form"))
                .andExpect(model().attributeHasFieldErrors("form", "name"))
                .andExpect(model().attribute("applianceId", nullValue()));

        verifyNoInteractions(applianceService);
        verifyNoInteractions(currentUserService);
    }

    @Test
    void create_shouldRejectRequestWithoutCsrf() throws Exception {
        mockMvc.perform(post("/appliances")
                        .with(user("testuser"))
                        .param("biddingZone", "LV")
                        .param("name", "Dishwasher")
                        .param("powerKw", "2.5")
                        .param("durationMinutes", "90")
                        .param("allowedFrom", "17:00")
                        .param("allowedTo", "23:00"))
                .andExpect(status().isForbidden());
    }

    @Test
    void update_shouldSaveApplianceWhenFormIsValid() throws Exception {
        User currentUser = new User();
        currentUser.setId(1L);

        when(currentUserService.get(any())).thenReturn(currentUser);

        mockMvc.perform(post("/appliances/10")
                        .with(user("testuser"))
                        .with(csrf())
                        .param("biddingZone", "EE")
                        .param("name", "Updated Dishwasher")
                        .param("powerKw", "3.0")
                        .param("durationMinutes", "120")
                        .param("allowedFrom", "23:00")
                        .param("allowedTo", "09:00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/appliances"));

        verify(applianceService).save(eq(10L), any(ApplianceForm.class), same(currentUser));
    }

    @Test
    void update_shouldReturnFormWhenValidationFails() throws Exception {
        mockMvc.perform(post("/appliances/10")
                        .with(user("testuser"))
                        .with(csrf())
                        .param("biddingZone", "EE")
                        .param("name", "Updated Dishwasher")
                        .param("powerKw", "0")
                        .param("durationMinutes", "120")
                        .param("allowedFrom", "23:00")
                        .param("allowedTo", "09:00"))
                .andExpect(status().isOk())
                .andExpect(view().name("appliance-form"))
                .andExpect(model().attributeHasFieldErrors("form", "powerKw"))
                .andExpect(model().attribute("applianceId", 10L));

        verifyNoInteractions(applianceService);
        verifyNoInteractions(currentUserService);
    }

    @Test
    void update_shouldRejectRequestWithoutCsrf() throws Exception {
        mockMvc.perform(post("/appliances/10")
                        .with(user("testuser"))
                        .param("biddingZone", "EE")
                        .param("name", "Updated Dishwasher")
                        .param("powerKw", "2.0")
                        .param("durationMinutes", "120")
                        .param("allowedFrom", "23:00")
                        .param("allowedTo", "09:00"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(applianceService);
        verifyNoInteractions(currentUserService);
    }

    @Test
    void update_shouldReturn404WhenApplianceNotOwned() throws Exception {
        User currentUser = new User();
        currentUser.setId(1L);

        when(currentUserService.get(any())).thenReturn(currentUser);
        when(applianceService.save(
                eq(10L),
                any(ApplianceForm.class),
                same(currentUser)))
                .thenThrow(new ApplianceNotFoundException("Appliance with id 10 not found."));

        mockMvc.perform(post("/appliances/10")
                        .with(user("testuser"))
                        .with(csrf())
                        .param("biddingZone", "EE")
                        .param("name", "Updated Dishwasher")
                        .param("powerKw", "3.0")
                        .param("durationMinutes", "120")
                        .param("allowedFrom", "23:00")
                        .param("allowedTo", "09:00"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("status", 404));

        verify(applianceService).save(eq(10L), any(ApplianceForm.class), same(currentUser));
    }

    @Test
    void delete_shouldDeleteOwnedAppliance() throws Exception {
        User currentUser = new User();
        currentUser.setId(1L);

        when(currentUserService.get(any())).thenReturn(currentUser);

        mockMvc.perform(post("/appliances/10/delete")
                        .with(user("testuser"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/appliances"));

        verify(applianceService).delete(10L, 1L);
    }

    @Test
    void delete_shouldRejectRequestWithoutCsrf() throws Exception {
        mockMvc.perform(post("/appliances/10/delete")
                        .with(user("testuser")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(applianceService);
        verifyNoInteractions(currentUserService);
    }

    @Test
    void delete_shouldReturn404WhenApplianceNotOwned() throws Exception {
        User currentUser = new User();
        currentUser.setId(1L);

        when(currentUserService.get(any())).thenReturn(currentUser);

        doThrow(new ApplianceNotFoundException("Appliance with id 10 not found."))
                .when(applianceService).delete(10L, 1L);

        mockMvc.perform(post("/appliances/10/delete")
                        .with(user("testuser"))
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("status", 404));

        verify(applianceService).delete(10L, 1L);
    }
}
