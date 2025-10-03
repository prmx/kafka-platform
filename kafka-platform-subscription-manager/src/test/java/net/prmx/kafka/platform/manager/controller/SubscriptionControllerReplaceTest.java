package net.prmx.kafka.platform.manager.controller;

import net.prmx.kafka.platform.common.model.SubscriptionCommand;
import net.prmx.kafka.platform.manager.dto.SubscriptionRequest;
import net.prmx.kafka.platform.manager.service.SubscriptionCommandService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * T032: Test REPLACE subscription endpoint with MockMvc
 * Tests must FAIL before implementation exists
 */
@WebMvcTest(SubscriptionController.class)
class SubscriptionControllerReplaceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SubscriptionCommandService commandService;

    @Test
    void testReplaceSubscription_validRequest_returns200() throws Exception {
        // Mock the service to return a valid command
        SubscriptionCommand mockCommand = new SubscriptionCommand(
            "subscriber-001", "REPLACE", List.of("KEY000001", "KEY000002", "KEY000003"), System.currentTimeMillis()
        );
        when(commandService.publishCommand(eq("subscriber-001"), eq(SubscriptionCommand.Action.REPLACE), any()))
            .thenReturn(mockCommand);

        String requestBody = """
            {
                "instrumentIds": ["KEY000001", "KEY000002", "KEY000003"]
            }
            """;

        mockMvc.perform(put("/api/v1/subscriptions/subscriber-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscriberId").value("subscriber-001"))
                .andExpect(jsonPath("$.action").value("REPLACE"))
                .andExpect(jsonPath("$.instrumentIds").isArray())
                .andExpect(jsonPath("$.instrumentIds[0]").value("KEY000001"))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    void testReplaceSubscription_emptyInstrumentIds_returns400() throws Exception {
        String requestBody = """
            {
                "instrumentIds": []
            }
            """;

        mockMvc.perform(put("/api/v1/subscriptions/subscriber-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testReplaceSubscription_invalidInstrumentId_returns400() throws Exception {
        String requestBody = """
            {
                "instrumentIds": ["KEY000001", "INVALID_ID", "KEY000003"]
            }
            """;

        mockMvc.perform(put("/api/v1/subscriptions/subscriber-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void testReplaceSubscription_nullInstrumentIds_returns400() throws Exception {
        String requestBody = """
            {
                "instrumentIds": null
            }
            """;

        mockMvc.perform(put("/api/v1/subscriptions/subscriber-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testReplaceSubscription_invalidInstrumentPattern_returns400() throws Exception {
        String requestBody = """
            {
                "instrumentIds": ["KEY000001", "KEY1000000"]
            }
            """;

        mockMvc.perform(put("/api/v1/subscriptions/subscriber-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest());
    }
}
