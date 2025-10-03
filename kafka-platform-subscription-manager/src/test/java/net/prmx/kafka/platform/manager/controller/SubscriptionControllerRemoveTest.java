package net.prmx.kafka.platform.manager.controller;

import net.prmx.kafka.platform.manager.service.SubscriptionCommandService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * T034: Test REMOVE instruments endpoint
 * Tests must FAIL before implementation exists
 */
@WebMvcTest(SubscriptionController.class)
class SubscriptionControllerRemoveTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SubscriptionCommandService commandService;

    @Test
    void testRemoveInstruments_validRequest_returns200() throws Exception {
        String requestBody = """
            {
                "instrumentIds": ["KEY000001", "KEY000002"]
            }
            """;

        mockMvc.perform(post("/api/v1/subscriptions/subscriber-001/remove")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscriberId").value("subscriber-001"))
                .andExpect(jsonPath("$.action").value("REMOVE"))
                .andExpect(jsonPath("$.instrumentIds").isArray())
                .andExpect(jsonPath("$.instrumentIds[0]").value("KEY000001"))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    void testRemoveInstruments_emptyInstrumentIds_returns400() throws Exception {
        String requestBody = """
            {
                "instrumentIds": []
            }
            """;

        mockMvc.perform(post("/api/v1/subscriptions/subscriber-001/remove")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void testRemoveInstruments_invalidInstrumentId_returns400() throws Exception {
        String requestBody = """
            {
                "instrumentIds": ["KEY000001", "INVALID"]
            }
            """;

        mockMvc.perform(post("/api/v1/subscriptions/subscriber-001/remove")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void testRemoveInstruments_validationError_returnsErrorResponse() throws Exception {
        String requestBody = """
            {
                "instrumentIds": ["KEY999999", "KEY1000000"]
            }
            """;

        mockMvc.perform(post("/api/v1/subscriptions/subscriber-001/remove")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
