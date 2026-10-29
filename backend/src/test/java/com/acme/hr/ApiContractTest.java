package com.acme.hr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.acme.hr.employee.application.EmployeeService;
import com.acme.hr.auth.persistence.HrUserRepository;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = "spring.autoconfigure.exclude="
        + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
        + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
        + "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration,"
        + "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration")
@AutoConfigureMockMvc
class ApiContractTest {
    @MockitoBean EmployeeService employeeService;
    @MockitoBean com.acme.hr.compensation.application.SalaryService salaryService;
    @MockitoBean com.acme.hr.analytics.application.AnalyticsService analyticsService;
    @MockitoBean com.acme.hr.employee.application.EmployeeSeeder employeeSeeder;
    @MockitoBean HrUserRepository hrUserRepository;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    void generatedContractIncludesEmployeeLifecycleAndValidation() throws Exception {
        String json = mvc.perform(get("/v3/api-docs").with(user("contract-test").roles("HR")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var contract = mapper.readTree(json);
        assertThat(contract.at("/paths/~1api~1employees/get").isMissingNode()).isFalse();
        assertThat(contract.at("/paths/~1api~1employees~1{employeeCode}~1salary/put").isMissingNode()).isFalse();
        assertThat(contract.at("/paths/~1api~1employees~1{employeeCode}~1salary~1history/get").isMissingNode()).isFalse();
        assertThat(contract.at("/components/schemas/SalaryResponse/properties/amount/type").asText()).isEqualTo("string");
        assertThat(contract.at("/components/schemas/SaveSalaryRequest/properties/amount/type").asText()).isEqualTo("string");
        assertThat(contract.at("/paths/~1api~1analytics~1dashboard/get/parameters/0/name").asText())
                .isEqualTo("country");
        assertThat(contract.at("/paths/~1api~1analytics~1dashboard/get/parameters/0/schema/enum").toString())
                .contains("US", "DE");
        assertThat(contract.at("/paths/~1api~1employees~1{employeeCode}~1archive/post").isMissingNode()).isFalse();
        assertThat(contract.at("/components/schemas/CreateEmployeeRequest/properties/firstName/maxLength").asInt())
                .isEqualTo(100);
        // Explicit export keeps the checked-in artifact generated from the running API.
        String output = System.getProperty("openapi.output");
        if (output != null) {
            Files.writeString(Path.of(output), mapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(contract) + System.lineSeparator());
        }
    }
}
