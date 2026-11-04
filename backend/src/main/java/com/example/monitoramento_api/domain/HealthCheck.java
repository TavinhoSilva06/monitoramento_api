package com.example.monitoramento_api.domain;

import java.time.Instant;

/**
 * Resultado de uma checagem feita em uma API monitorada.
 * O id e nulo ate a checagem ser gravada.
 */
public record HealthCheck(
		Long id,
		Long apiId,
		Integer statusCode,
		Long responseTimeMs,
		boolean success,
		String errorMessage,
		Instant checkedAt) {
}
