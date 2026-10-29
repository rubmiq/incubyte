package com.acme.hr.common.web;

import java.io.IOException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Serves the Angular build as the single-page app shell for any
 * non-API, non-actuator route that doesn't resolve to a real static file,
 * so client-side routing (e.g. a deep link to /employees/42) resolves
 * correctly on a full page load. A {@code **} path-pattern variable can
 * only appear at the start or end of a Spring path pattern, which rules
 * out a {@link org.springframework.web.servlet.config.annotation.ViewControllerRegistry}
 * regex capture spanning arbitrary depth — resource resolution does not
 * have that restriction.
 */
@Configuration
class SpaForwardingConfig implements WebMvcConfigurer {

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/**")
				.addResourceLocations("classpath:/static/")
				.resourceChain(true)
				.addResolver(new SpaIndexFallbackResolver());
	}

	private static final class SpaIndexFallbackResolver extends PathResourceResolver {

		@Override
		protected Resource getResource(String resourcePath, Resource location) throws IOException {
			Resource requestedResource = location.createRelative(resourcePath);
			if (requestedResource.exists() && requestedResource.isReadable()) {
				return requestedResource;
			}
			if (resourcePath.startsWith("api/") || resourcePath.startsWith("actuator/")) {
				return null;
			}
			Resource index = new ClassPathResource("/static/index.html");
			return index.exists() && index.isReadable() ? index : null;
		}
	}
}
