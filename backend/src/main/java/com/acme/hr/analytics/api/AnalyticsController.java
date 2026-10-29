package com.acme.hr.analytics.api;

import com.acme.hr.analytics.application.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dashboard analytics endpoint. All aggregates (headcount, payroll, mean/median)
 * are computed server-side over the complete active employee population and
 * converted to USD via demo FX snapshot. HR role required for access.
 */
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

	private final AnalyticsService analyticsService;

	public AnalyticsController(AnalyticsService analyticsService) {
		this.analyticsService = analyticsService;
	}

	@GetMapping("/dashboard")
	@PreAuthorize("hasRole('HR')")
	public ResponseEntity<DashboardResponse> getDashboard() {
		DashboardResponse dashboard = analyticsService.computeDashboard();
		return ResponseEntity.ok(dashboard);
	}
}
