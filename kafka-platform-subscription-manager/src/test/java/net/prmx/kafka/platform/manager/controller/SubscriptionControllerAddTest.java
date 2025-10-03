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
 * T033: Test ADD instruments endpoint
 * Tests must FAIL before implementation exists
 */
@WebMvcTest(SubscriptionController.class)
class SubscriptionControllerAddTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SubscriptionCommandService commandService;

    @Test
    void testAddInstruments_validRequest_returns200() throws Exception {
        String requestBody = """
            {
                "instrumentIds": ["KEY000004", "KEY000005"]
            }
            """;

        mockMvc.perform(post("/api/v1/subscriptions/subscriber-001/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscriberId").value("subscriber-001"))
                .andExpect(jsonPath("$.action").value("ADD"))
                .andExpect(jsonPath("$.instrumentIds").isArray())
                .andExpect(jsonPath("$.instrumentIds[0]").value("KEY000004"))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    void testAddInstruments_emptyInstrumentIds_returns400() throws Exception {
        String requestBody = """
            {
                "instrumentIds": []
            }
            """;

        mockMvc.perform(post("/api/v1/subscriptions/subscriber-001/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void testAddInstruments_invalidInstrumentId_returns400() throws Exception {
        String requestBody = """
            {
                "instrumentIds": ["KEY000001", "BAD_FORMAT"]
            }
            """;

        mockMvc.perform(post("/api/v1/subscriptions/subscriber-001/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void testAddInstruments_blankSubscriberId_returns404() throws Exception {
        String requestBody = """
            {
                "instrumentIds": ["KEY000001"]
            }
            """;

        mockMvc.perform(post("/api/v1/subscriptions/ /add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isNotFound());
    }
}
