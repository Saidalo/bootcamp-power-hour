package com.bootcamp.powerhour.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ApiExceptionHandlerTest {
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void notFoundException_shouldReturn404() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Appliance with id 1 was not found"));
    }

    @Test
    void exceptionWithResponseStatus_shouldReturnThatStatus() throws Exception {
        mockMvc.perform(get("/test/zone-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Zone not found"));
    }

    @Test
    void illegalArgumentException_shouldReturn400() throws Exception {
        mockMvc.perform(get("/test/bad-request"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unexpectedException_shouldReturn500WithGenericMessage() throws Exception {
        mockMvc.perform(get("/test/error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("Something went wrong"));
    }

    @Test
    void wrongHttpMethod_shouldReturn405() throws Exception {
        mockMvc.perform(post("/test/not-found"))
                .andExpect(status().isMethodNotAllowed());
    }

    @RestController
    static class TestController {
        @GetMapping("/test/not-found")
        void notFound() {
            throw new NotFoundException("Appliance", 1);
        }

        @GetMapping("/test/zone-not-found")
        void zoneNotFound() {
            throw new ZoneNotFound();
        }

        @GetMapping("/test/bad-request")
        void badRequest() {
            throw new IllegalArgumentException("Invalid date range");
        }

        @GetMapping("/test/error")
        void error() {
            throw new RuntimeException("Database error");
        }
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    static class ZoneNotFound extends RuntimeException {
        ZoneNotFound() {
            super("Zone not found");
        }
    }
}
