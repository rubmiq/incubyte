package com.acme.hr.common.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serves the Angular build as the single-page app shell for any
 * non-API, non-asset route, so client-side routing (e.g. a deep link to
 * /employees/42) resolves correctly on a full page load.
 */
@Configuration
class SpaForwardingConfig implements WebMvcConfigurer {

	@Override
	public void addViewControllers(ViewControllerRegistry registry) {
		registry.addViewController("/{path:^(?!api|actuator)[^.]*}").setViewName("forward:/index.html");
		registry.addViewController("/{path:^(?!api|actuator).*}/**{path:[^.]*}").setViewName("forward:/index.html");
	}

}
