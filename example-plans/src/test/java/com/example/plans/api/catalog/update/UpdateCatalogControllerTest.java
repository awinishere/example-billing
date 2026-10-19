package com.example.plans.api.catalog.update;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.plans.applications.catalog.InMemoryPlanRepository;
import com.example.plans.applications.catalog.update.UpdatePlanHandler;
import com.example.plans.domain.model.Plan;
import com.example.plans.extensions.exception.PlansExceptionHandler;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class UpdateCatalogControllerTest {

    private final InMemoryPlanRepository planRepository = new InMemoryPlanRepository();

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new UpdateCatalogController(new UpdatePlanHandler(planRepository)))
            .setControllerAdvice(new PlansExceptionHandler())
            .build();

    @Test
    void updateReturnsTheNewContentTogetherWithTheFrozenIdentity() throws Exception {
        UUID id = planRepository.save(Plan.newPlan("pro", "Pro", "old description")).id();

        String body = mockMvc.perform(put("/api/v1/plans/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Pro Renamed\",\"description\":\"new description\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertTrue(body.contains("\"code\":\"pro\""), body);
        assertTrue(body.contains("\"name\":\"Pro Renamed\""), body);
        assertEquals("Pro Renamed", planRepository.findById(id).orElseThrow().name());
    }

    @Test
    void updateOfUnknownIdReturnsNotFound() throws Exception {
        mockMvc.perform(put("/api/v1/plans/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ghost\",\"description\":\"description\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void blankNameReturnsBadRequestBeforeTouchingTheDatabase() throws Exception {
        UUID id = planRepository.save(Plan.newPlan("pro", "Pro", "old description")).id();

        mockMvc.perform(put("/api/v1/plans/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" \",\"description\":\"new description\"}"))
                .andExpect(status().isBadRequest());

        assertEquals("Pro", planRepository.findById(id).orElseThrow().name());
    }
}
