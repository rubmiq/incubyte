package com.acme.hr.compensation;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.acme.hr.auth.application.HrUserDetailsService;
import com.acme.hr.auth.config.SecurityConfig;
import com.acme.hr.common.web.ConflictException;
import com.acme.hr.compensation.api.*;
import com.acme.hr.compensation.application.SalaryService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SalaryController.class)
@Import(SecurityConfig.class)
class SalaryControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean SalaryService service;
    @MockitoBean HrUserDetailsService users;
    private static final String URL = "/api/employees/EMP-1/salary";
    private static final String BODY = """
        {"amount":"85000.01","currency":"USD","changeReason":"Annual review","expectedVersion":0}
        """;
    private SalaryResponse salary() {
        return new SalaryResponse("85000.01", SalaryCurrency.USD, 1,
                LocalDate.of(2026, 10, 29), Instant.parse("2026-10-29T08:00:00Z"));
    }

    @Test
    void requiresHrOnCurrentHistoryAndWrites() throws Exception {
        for (String path : new String[]{URL, URL + "/history"}) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
            mvc.perform(get(path).with(user("viewer").roles("VIEWER"))).andExpect(status().isForbidden());
        }
        mvc.perform(put(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
        mvc.perform(put(URL).with(csrf()).with(user("viewer").roles("VIEWER"))
                .contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isForbidden());
        mvc.perform(put(URL).with(user("hr").roles("HR"))
                .contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isForbidden());
    }

    @Test
    void serializesMoneyAsStringsAndDerivesActorFromSession() throws Exception {
        var request = new SaveSalaryRequest("85000.01", SalaryCurrency.USD, "Annual review", 0L);
        when(service.save("EMP-1", request, "session-hr")).thenReturn(salary());
        mvc.perform(put(URL).with(csrf()).with(user("session-hr").roles("HR"))
                .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.amount").isString())
                .andExpect(jsonPath("$.amount").value("85000.01"))
                .andExpect(jsonPath("$.updatedAt").value("2026-10-29T08:00:00Z"));
    }

    @Test
    void distinguishesMissingSalaryFromCurrentSalary() throws Exception {
        when(service.current("EMP-1")).thenReturn(Optional.empty());
        mvc.perform(get(URL).with(user("hr").roles("HR"))).andExpect(status().isNoContent());
        when(service.current("EMP-1")).thenReturn(Optional.of(salary()));
        mvc.perform(get(URL).with(user("hr").roles("HR"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value("85000.01"));
    }

    @Test
    void rejectsMalformedAmountsCurrenciesReasonsAndVersions() throws Exception {
        for (String body : new String[]{BODY.replace("85000.01", "1.001"), BODY.replace("USD", "JPY"),
                BODY.replace("Annual review", " "), BODY.replace("\"expectedVersion\":0", "\"expectedVersion\":-1"),
                BODY.replace("85000.01", "0"), BODY.replace("\"85000.01\"", "85000.01")}) {
            mvc.perform(put(URL).with(csrf()).with(user("hr").roles("HR"))
                    .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        }
    }

    @Test
    void mapsStaleWritesToConflict() throws Exception {
        when(service.save("EMP-1", new SaveSalaryRequest("85000.01", SalaryCurrency.USD, "Annual review", 0L), "hr"))
                .thenThrow(new ConflictException("Salary changed. Refresh and review."));
        mvc.perform(put(URL).with(csrf()).with(user("hr").roles("HR"))
                .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void historyHasNoMutationEndpoints() throws Exception {
        mvc.perform(delete(URL + "/history/1").with(csrf()).with(user("hr").roles("HR")))
                .andExpect(status().isNotFound());
    }
}
