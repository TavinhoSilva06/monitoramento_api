package com.example.monitoramento_api.domain;

import java.time.Instant;
import java.util.List;

/**
 * Resumo de uma API em um periodo: disponibilidade e latencia.
 *
 * <p>Disponibilidade = checagens com sucesso / total de checagens.
 * Latencia considera apenas checagens que receberam resposta HTTP (statusCode presente),
 * para que timeouts e erros de DNS nao distorcam a media.
 * Campos nulos quando o periodo nao tem checagens (ou nao tem respostas, no caso da latencia).
 */
public record ApiSummary(
		Long apiId,
		Instant from,
		Instant to,
		int totalChecks,
		int successfulChecks,
		Double uptimePercent,
		Double averageLatencyMs,
		Long p95LatencyMs) {

	public static ApiSummary of(Long apiId, Instant from, Instant to, List<HealthCheck> checks) {
		int total = checks.size();
		int sucessos = (int) checks.stream().filter(HealthCheck::success).count();
		Double uptime = total == 0 ? null : sucessos * 100.0 / total;

		List<Long> latencias = checks.stream()
				.filter(c -> c.statusCode() != null && c.responseTimeMs() != null)
				.map(HealthCheck::responseTimeMs)
				.sorted()
				.toList();

		Double media = latencias.isEmpty()
				? null
				: latencias.stream().mapToLong(Long::longValue).average().orElseThrow();
		Long p95 = latencias.isEmpty() ? null : percentil(latencias, 95);

		return new ApiSummary(apiId, from, to, total, sucessos, uptime, media, p95);
	}

	/** Percentil pelo metodo do posto mais proximo sobre uma lista ja ordenada. */
	private static long percentil(List<Long> ordenada, int percentil) {
		int posto = (int) Math.ceil(percentil / 100.0 * ordenada.size());
		return ordenada.get(Math.max(posto - 1, 0));
	}

}
