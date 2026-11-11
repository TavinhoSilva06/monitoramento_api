package com.example.monitoramento_api.interfaces.web;

import com.example.monitoramento_api.domain.HealthCheck;

import java.time.Instant;

public record ChecagemResponse(
		Long id,
		Integer statusCode,
		Long responseTimeMs,
		boolean success,
		String errorMessage,
		Instant checkedAt) {

	public static ChecagemResponse from(HealthCheck check) {
		return new ChecagemResponse(
				check.id(),
				check.statusCode(),
				check.responseTimeMs(),
				check.success(),
				check.errorMessage(),
				check.checkedAt());
	}

}
