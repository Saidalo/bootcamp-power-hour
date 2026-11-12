package com.bootcamp.powerhour.controller;

import com.bootcamp.powerhour.config.SecurityConfig;
import com.bootcamp.powerhour.dto.RunView;
import com.bootcamp.powerhour.entity.User;
import com.bootcamp.powerhour.service.ApplianceService;
import com.bootcamp.powerhour.service.CheapestWindowService;
import com.bootcamp.powerhour.service.CurrentUserService;
import com.bootcamp.powerhour.service.CustomUserDetailService;
import com.bootcamp.powerhour.service.PlannedRunService;
import com.bootcamp.powerhour.service.PriceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Checks the shared page shell (fragments/layout.html) that every page uses:
 * stylesheet, page title, navigation, the current page, the admin link and logout.
 *
 * @author Karyna Litvinova
 */
@WebMvcTest({PlanController.class, RunController.class})
@Import(SecurityConfig.class)
class LayoutTest {

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

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(42L);
        user.setUsername("ana");
        when(currentUserService.get(any())).thenReturn(user);
        when(applianceService.getAppliancesByUserId(anyLong())).thenReturn(List.of());
        when(plannedRunService.runsOf(anyLong())).thenReturn(List.<RunView>of());
    }

    @Test
    @WithMockUser(username = "ana")
    void linksTheSharedStylesheet() throws Exception {
        mockMvc.perform(get("/plan"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/css/style.css\"")));
    }

    @Test
    @WithMockUser(username = "ana")
    void namesThePageInTheTitle() throws Exception {
        mockMvc.perform(get("/plan"))
                .andExpect(content().string(containsString("<title>PowerHour — Plan a run</title>")));

        mockMvc.perform(get("/runs"))
                .andExpect(content().string(containsString("<title>PowerHour — My runs</title>")));
    }

    @Test
    @WithMockUser(username = "ana")
    void showsEveryNavigationLink() throws Exception {
        mockMvc.perform(get("/plan"))
                .andExpect(content().string(containsString("href=\"/appliances\"")))
                .andExpect(content().string(containsString("href=\"/plan\"")))
                .andExpect(content().string(containsString("href=\"/runs\"")))
                .andExpect(content().string(containsString("href=\"/savings\"")));
    }

    @Test
    @WithMockUser(username = "ana")
    void marksThePlanPageAsCurrent() throws Exception {
        mockMvc.perform(get("/plan"))
                .andExpect(content().string(matchesPattern(
                        Pattern.compile(".*<a href=\"/plan\"\\s+class=\"current\">Plan</a>.*", Pattern.DOTALL))));
    }

    @Test
    @WithMockUser(username = "ana")
    void marksOnlyOnePageAsCurrent() throws Exception {
        mockMvc.perform(get("/runs"))
                .andExpect(content().string(matchesPattern(
                        Pattern.compile(".*<a href=\"/runs\"\\s+class=\"current\">My runs</a>.*", Pattern.DOTALL))))
                .andExpect(content().string(matchesPattern(
                        Pattern.compile(".*<a href=\"/plan\"\\s*>Plan</a>.*", Pattern.DOTALL))));
    }

    @Test
    @WithMockUser(username = "ana")
    void showsTheSignedInUsername() throws Exception {
        mockMvc.perform(get("/plan"))
                .andExpect(content().string(containsString("<span>ana</span>")));
    }

    @Test
    @WithMockUser(username = "ana")
    void hidesTheAdminLinkFromAUser() throws Exception {
        mockMvc.perform(get("/plan"))
                .andExpect(content().string(not(containsString("href=\"/admin\""))));
    }

    @Test
    @WithMockUser(username = "ana", roles = "ADMIN")
    void showsTheAdminLinkToAnAdmin() throws Exception {
        mockMvc.perform(get("/plan"))
                .andExpect(content().string(containsString("href=\"/admin\"")));
    }

    @Test
    @WithMockUser(username = "ana")
    void logsOutWithAPostThatCarriesTheCsrfToken() throws Exception {
        mockMvc.perform(get("/plan"))
                .andExpect(content().string(containsString("action=\"/logout\"")))
                .andExpect(content().string(containsString("method=\"post\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    @WithMockUser(username = "ana")
    void theLogoLeadsHome() throws Exception {
        mockMvc.perform(get("/plan"))
                .andExpect(content().string(containsString("class=\"logo\" href=\"/\"")));
    }
}
