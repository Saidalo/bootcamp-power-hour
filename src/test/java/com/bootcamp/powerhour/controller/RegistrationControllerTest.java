package com.bootcamp.powerhour.controller;

import com.bootcamp.powerhour.config.SecurityConfig;
import com.bootcamp.powerhour.exception.UserAlreadyExistsException;
import com.bootcamp.powerhour.service.CustomUserDetailService;
import com.bootcamp.powerhour.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RegistrationController.class)
@Import(SecurityConfig.class)
class RegistrationControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private CustomUserDetailService userDetailService;

    @Test
    void showRegisterForm_shouldBePublic() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    void register_shouldRedirectToLoginWhenValid() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("username", "anna")
                        .param("email", "anna@example.com")
                        .param("password", "password123")
                        .param("biddingZone", "LV"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));

        verify(userService).register(any());
    }

    @Test
    void register_shouldShowErrorsWhenInvalid() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("username", "")
                        .param("email", "not-an-email")
                        .param("password", "short"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors("form", "username", "email", "password"));

        verify(userService, never()).register(any());
    }

    @Test
    void register_shouldShowErrorWhenUsernameExists() throws Exception {
        when(userService.register(any())).thenThrow(new UserAlreadyExistsException("username", "anna"));

        mockMvc.perform(post("/register").with(csrf())
                        .param("username", "anna")
                        .param("email", "anna@example.com")
                        .param("password", "password123")
                        .param("biddingZone", "LV"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors("form", "username"));
    }

    @Test
    void register_shouldBeRejectedWithoutCsrfToken() throws Exception {
        mockMvc.perform(post("/register")
                        .param("username", "anna")
                        .param("email", "anna@example.com")
                        .param("password", "password123")
                        .param("biddingZone", "LV"))
                .andExpect(status().isForbidden());
    }
}
