package com.acme.hr.employee.api;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acme.hr.auth.application.HrUserDetailsService;
import com.acme.hr.auth.config.SecurityConfig;
import com.acme.hr.common.reference.Country;
import com.acme.hr.common.web.ConflictException;
import com.acme.hr.common.web.NotFoundException;
import com.acme.hr.employee.application.EmployeeService;
import com.acme.hr.employee.persistence.Department;
import com.acme.hr.employee.persistence.Employee;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

/**
 * Exercises the HTTP contract (status codes, JSON shape, auth boundary) with
 * {@link EmployeeService} mocked, so no database is required for this slice.
 */
@WebMvcTest(controllers = EmployeeController.class)
@Import(SecurityConfig.class)
class EmployeeControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private EmployeeService employeeService;

	@MockitoBean
	private HrUserDetailsService hrUserDetailsService;

	private static Employee sampleEmployee() {
		return new Employee(
				"EMP-000001",
				"Alice",
				"Zimmer",
				"alice.zimmer@acme.test",
				Country.US,
				Department.ENGINEERING,
				"Engineer",
				LocalDate.of(2025, 1, 1),
				Instant.parse("2026-01-01T00:00:00Z"));
	}

	@Test
	@WithMockUser(roles = "HR")
	void oversizedProfileFieldIsValidationError() throws Exception {
		var request = new CreateEmployeeRequest("A".repeat(101), "Zimmer", "alice@acme.test",
				Country.US, Department.ENGINEERING, "Engineer", LocalDate.of(2025, 1, 1));
		mockMvc.perform(post("/api/employees").with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[0].field", equalTo("firstName")));
	}

	@Test
	void listWithoutSessionIsUnauthenticated() throws Exception {
		mockMvc.perform(get("/api/employees"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code", equalTo("UNAUTHENTICATED")));
	}

	@Test
	void nonHrRoleIsForbidden() throws Exception {
		mockMvc.perform(get("/api/employees").with(user("someone").roles("OTHER")))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code", equalTo("FORBIDDEN")));
	}

	@Test
	@WithMockUser(roles = "HR")
	void listReturnsPagedSummaries() throws Exception {
		when(employeeService.list(any(), any(), any(), any(), any()))
				.thenReturn(new PageImpl<>(java.util.List.of(sampleEmployee()), PageRequest.of(0, 20), 1));

		mockMvc.perform(get("/api/employees"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].employeeCode", equalTo("EMP-000001")))
				.andExpect(jsonPath("$.totalElements", equalTo(1)));
	}

	@Test
	@WithMockUser(roles = "HR")
	void getUnknownEmployeeIsNotFound() throws Exception {
		when(employeeService.getByCode("EMP-999999")).thenThrow(new NotFoundException("No employee found."));

		mockMvc.perform(get("/api/employees/EMP-999999"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code", equalTo("NOT_FOUND")));
	}

	@Test
	@WithMockUser(roles = "HR")
	void createWithInvalidEmailIsRejected() throws Exception {
		String body = """
				{"firstName":"Alice","lastName":"Zimmer","email":"not-an-email",
				"country":"US","department":"ENGINEERING","jobTitle":"Engineer","hiredOn":"2025-01-01"}
				""";

		mockMvc.perform(post("/api/employees").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", equalTo("VALIDATION_FAILED")));
	}

	@Test
	@WithMockUser(roles = "HR")
	void createWithValidRequestReturnsCreatedWithLocation() throws Exception {
		EmployeeDetail created = EmployeeDetail.from(sampleEmployee());
		when(employeeService.create(any())).thenReturn(created);

		String body = """
				{"firstName":"Alice","lastName":"Zimmer","email":"alice.zimmer@acme.test",
				"country":"US","department":"ENGINEERING","jobTitle":"Engineer","hiredOn":"2025-01-01"}
				""";

		mockMvc.perform(post("/api/employees").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", org.hamcrest.Matchers.containsString("EMP-000001")))
				.andExpect(jsonPath("$.employeeCode", equalTo("EMP-000001")));
	}

	@Test
	@WithMockUser(roles = "HR")
	void archivingAlreadyArchivedEmployeeIsConflict() throws Exception {
		when(employeeService.archive(anyString()))
				.thenThrow(new ConflictException("Employee EMP-000001 is already archived."));

		mockMvc.perform(post("/api/employees/EMP-000001/archive").with(csrf()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code", equalTo("CONFLICT")));
	}

	@Test
	@WithMockUser(roles = "HR")
	void updateWithoutCsrfTokenIsRejected() throws Exception {
		String body = objectMapper.writeValueAsString(
				new UpdateEmployeeRequest(
						"Alice", "Zimmer", "alice.zimmer@acme.test", Country.US, Department.ENGINEERING, "Engineer",
						LocalDate.of(2025, 1, 1)));

		mockMvc.perform(put("/api/employees/EMP-000001").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isForbidden());
	}
}
