package com.example.plans.api;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.plans.IntegrationTest;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class OpenApiDocumentationTest extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void apiDocsDescribeThePlanEndpoints() throws Exception {
        String spec = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertTrue(spec.contains("\"title\":\"example-plans\""), spec.substring(0, Math.min(400, spec.length())));
        assertTrue(spec.contains("/api/v1/plans"), spec);
        assertTrue(spec.contains("/api/v1/plans/{id}"), spec);
    }

    @Test
    void scalarPagePointsAtTheApiDocsOfThisApplication() throws Exception {
        String page = mockMvc.perform(get("/docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertTrue(page.contains("/v3/api-docs"), page.substring(0, Math.min(400, page.length())));
        assertTrue(page.contains("scalar.js"), page);
    }

    @Test
    void scalarBundleIsServedNextToThePage() throws Exception {
        byte[] bundle = mockMvc.perform(get("/docs/scalar.js"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        assertTrue(bundle.length > 1000, "the scalar bundle should not be an empty body: " + bundle.length);
    }
}
