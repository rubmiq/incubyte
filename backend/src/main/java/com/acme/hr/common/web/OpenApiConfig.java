package com.acme.hr.common.web;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfig {
    @Bean
    OpenAPI hrApi() {
        return new OpenAPI()
                .info(new Info().title("ACME HR API").version("1")
                        .description("Same-origin session API. Mutating requests require the X-XSRF-TOKEN "
                                + "header from the XSRF-TOKEN cookie. Get /api/auth/csrf at startup and after "
                                + "login/logout. Dates use ISO 8601; audit timestamps use UTC."))
                .servers(List.of(new Server().url("/")))
                .components(new Components().addSecuritySchemes("session", new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.COOKIE).name("JSESSIONID")))
                .addSecurityItem(new SecurityRequirement().addList("session"))
                // Spring Security owns logout, so MVC discovery cannot see it.
                .path("/api/auth/logout", new PathItem().post(new Operation().operationId("logout")
                        .description("Invalidate the session and clear the CSRF cookie. Requires a CSRF header.")
                        .responses(new ApiResponses().addApiResponse("204", new ApiResponse()
                                .description("Logged out; obtain a fresh CSRF cookie before login.")))));
    }
}
