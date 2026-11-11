package com.example.monitoramento_api.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApiSummaryTest {

	private static final Instant DE = Instant.parse("2026-01-01T00:00:00Z");
	private static final Instant ATE = Instant.parse("2026-01-02T00:00:00Z");

	@Test
	void periodoSemChecagensNaoTemUptimeNemLatencia() {
		ApiSummary resumo = ApiSummary.of(1L, DE, ATE, List.of());

		assertThat(resumo.totalChecks()).isZero();
		assertThat(resumo.uptimePercent()).isNull();
		assertThat(resumo.averageLatencyMs()).isNull();
		assertThat(resumo.p95LatencyMs()).isNull();
	}

	@Test
	void calculaUptimePelasChecagensComSucesso() {
		List<HealthCheck> checks = List.of(
				check(200, 100L, true),
				check(200, 100L, true),
				check(500, 100L, false),
				check(200, 100L, true));

		ApiSummary resumo = ApiSummary.of(1L, DE, ATE, checks);

		assertThat(resumo.totalChecks()).isEqualTo(4);
		assertThat(resumo.successfulChecks()).isEqualTo(3);
		assertThat(resumo.uptimePercent()).isEqualTo(75.0);
	}

	@Test
	void latenciaIgnoraChecagensSemResposta() {
		List<HealthCheck> checks = List.of(
				check(200, 100L, true),
				check(200, 300L, true),
				check(null, 5000L, false));

		ApiSummary resumo = ApiSummary.of(1L, DE, ATE, checks);

		assertThat(resumo.averageLatencyMs()).isEqualTo(200.0);
	}

	@Test
	void p95PeloMetodoDoPostoMaisProximo() {
		List<HealthCheck> checks = new ArrayList<>();
		for (long ms = 1; ms <= 20; ms++) {
			checks.add(check(200, ms, true));
		}

		// 95% de 20 valores = posto 19
		assertThat(ApiSummary.of(1L, DE, ATE, checks).p95LatencyMs()).isEqualTo(19L);
	}

	@Test
	void umaUnicaChecagemUsaElaComoP95() {
		ApiSummary resumo = ApiSummary.of(1L, DE, ATE, List.of(check(200, 42L, true)));

		assertThat(resumo.p95LatencyMs()).isEqualTo(42L);
	}

	private static HealthCheck check(Integer status, Long ms, boolean sucesso) {
		return new HealthCheck(null, 1L, status, ms, sucesso, null, DE);
	}

}
