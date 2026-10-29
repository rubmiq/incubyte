package com.acme.hr.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acme.hr.auth.api.AuthController;
import com.acme.hr.auth.application.HrUserDetailsService;
import com.acme.hr.auth.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

/**
 * Exercises the real {@link SecurityConfig} filter chain (CSRF, session
 * auth, JSON 401/403 handling) plus {@link AuthController}, with
 * {@link HrUserDetailsService} mocked so no database is required.
 */
@WebMvcTest(controllers = AuthController.class)
@Import(SecurityConfig.class)
class AuthSecurityTest {

	private static final String RAW_PASSWORD = "correct-horse-battery-staple";
	private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder();

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private HrUserDetailsService hrUserDetailsService;

	@Test
	void appShellRouteIsPublic() throws Exception {
		// This MVC slice has no bundled SPA; a 404 proves security allowed dispatch.
		mockMvc.perform(get("/login")).andExpect(status().isNotFound());
	}

	@Test
	void loginWithoutCsrfTokenIsRejected() throws Exception {
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginBody("hr.demo", RAW_PASSWORD)))
				.andExpect(status().isForbidden());
	}

	@Test
	void loginWithValidCredentialsSucceedsAndEstablishesSession() throws Exception {
		mockHrUser("hr.demo", RAW_PASSWORD);

		mockMvc.perform(post("/api/auth/login")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginBody("hr.demo", RAW_PASSWORD)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username", equalTo("hr.demo")))
				.andExpect(jsonPath("$.role", equalTo("HR")));
	}

	@Test
	void loginWithWrongPasswordIsUnauthenticated() throws Exception {
		mockHrUser("hr.demo", RAW_PASSWORD);

		mockMvc.perform(post("/api/auth/login")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginBody("hr.demo", "not-the-password")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code", equalTo("UNAUTHENTICATED")));
	}

	@Test
	void meWithoutSessionIsUnauthenticated() throws Exception {
		mockMvc.perform(get("/api/auth/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code", equalTo("UNAUTHENTICATED")));
	}

	@Test
	@WithMockUser(username = "hr.demo", roles = "HR")
	void meWithHrSessionIsAuthorized() throws Exception {
		mockMvc.perform(get("/api/auth/me"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username", equalTo("hr.demo")));
	}

	@Test
	void nonHrRoleIsForbidden() throws Exception {
		mockMvc.perform(get("/api/auth/me").with(user("someone").roles("OTHER")))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code", equalTo("FORBIDDEN")));
	}

	@Test
	void csrfEndpointIssuesXsrfCookie() throws Exception {
		mockMvc.perform(get("/api/auth/csrf"))
				.andExpect(status().isOk())
				.andExpect(cookie().exists("XSRF-TOKEN"));
	}

	@Test
	void logoutInvalidatesSessionSoSubsequentMeIsUnauthenticated() throws Exception {
		mockHrUser("hr.demo", RAW_PASSWORD);

		MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginBody("hr.demo", RAW_PASSWORD)))
				.andExpect(status().isOk())
				.andReturn();
		MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
		assertThat(session).isNotNull();

		mockMvc.perform(get("/api/auth/me").session(session))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/auth/logout").with(csrf()).session(session))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/auth/me").session(session))
				.andExpect(status().isUnauthorized());
	}

	private void mockHrUser(String username, String rawPassword) {
		UserDetails userDetails = User.withUsername(username)
				.password(ENCODER.encode(rawPassword))
				.authorities("ROLE_HR")
				.build();
		when(hrUserDetailsService.loadUserByUsername(anyString())).thenReturn(userDetails);
	}

	private String loginBody(String username, String password) throws Exception {
		return objectMapper.writeValueAsString(new com.acme.hr.auth.api.LoginRequest(username, password));
	}
}
