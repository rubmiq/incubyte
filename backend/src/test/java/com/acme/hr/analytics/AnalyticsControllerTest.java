package com.acme.hr.analytics;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acme.hr.analytics.api.AnalyticsController;
import com.acme.hr.analytics.api.DashboardResponse;
import com.acme.hr.analytics.application.AnalyticsService;
import com.acme.hr.auth.application.HrUserDetailsService;
import com.acme.hr.auth.config.SecurityConfig;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AnalyticsController.class)
@Import(SecurityConfig.class)
class AnalyticsControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean AnalyticsService service;
    @MockitoBean HrUserDetailsService users;

    @Test
    void requiresAuthenticationAndHrRole() throws Exception {
        mvc.perform(get("/api/analytics/dashboard")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/analytics/dashboard").with(user("viewer").roles("VIEWER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsInvalidFiltersWithFieldErrors() throws Exception {
        for (String field : new String[]{"country", "department"}) {
            mvc.perform(get("/api/analytics/dashboard").param(field, "INVALID")
                    .with(user("hr").roles("HR")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.fieldErrors[0].field").value(field));
        }
    }

    @Test
    void appliesCombinedFilters() throws Exception {
        when(service.computeDashboard("US", "ENGINEERING")).thenReturn(new DashboardResponse(
                1, new BigDecimal("100.00"), new BigDecimal("100.00"), new BigDecimal("100.00"),
                Map.of(), Map.of(), "2026-01-01", "Demo FX"));
        mvc.perform(get("/api/analytics/dashboard").param("country", "US")
                .param("department", "ENGINEERING").with(user("hr").roles("HR")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active_headcount").value(1));
    }
}
