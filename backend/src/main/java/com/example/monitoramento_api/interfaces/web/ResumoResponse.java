package com.example.monitoramento_api.interfaces.web;

import com.example.monitoramento_api.domain.ApiSummary;

import java.time.Instant;

public record ResumoResponse(
		Long apiId,
		Instant from,
		Instant to,
		int totalChecks,
		int successfulChecks,
		Double uptimePercent,
		Double averageLatencyMs,
		Long p95LatencyMs) {

	public static ResumoResponse from(ApiSummary resumo) {
		return new ResumoResponse(
				resumo.apiId(),
				resumo.from(),
				resumo.to(),
				resumo.totalChecks(),
				resumo.successfulChecks(),
				resumo.uptimePercent(),
				resumo.averageLatencyMs(),
				resumo.p95LatencyMs());
	}

}
