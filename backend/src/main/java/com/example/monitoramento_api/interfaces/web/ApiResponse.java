package com.example.monitoramento_api.interfaces.web;

import com.example.monitoramento_api.domain.MonitoredApi;

import java.time.Instant;

public record ApiResponse(
		Long id,
		String name,
		String url,
		int intervalSeconds,
		int expectedStatus,
		boolean active,
		Instant createdAt) {

	public static ApiResponse from(MonitoredApi api) {
		return new ApiResponse(
				api.id(),
				api.name(),
				api.url(),
				api.intervalSeconds(),
				api.expectedStatus(),
				api.active(),
				api.createdAt());
	}

}
