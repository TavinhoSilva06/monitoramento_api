package com.example.monitoramento_api.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiHealthTest {

	private final ApiHealth health = new ApiHealth(new AlertPolicy(3));

	@Test
	void comecaDesconhecidaAntesDeQualquerChecagem() {
		assertThat(health.status()).isEqualTo(HealthStatus.UNKNOWN);
	}

	@Test
	void tresFalhasSeguidasDeixamApiDown() {
		assertThat(health.register(false)).contains(new HealthTransition(HealthStatus.UNKNOWN, HealthStatus.DEGRADED));
		assertThat(health.register(false)).isEmpty();
		assertThat(health.register(false)).contains(new HealthTransition(HealthStatus.DEGRADED, HealthStatus.DOWN));

		assertThat(health.status()).isEqualTo(HealthStatus.DOWN);
		assertThat(health.consecutiveFailures()).isEqualTo(3);
	}

	@Test
	void falhaContinuaDownSemNovaTransicao() {
		for (int i = 0; i < 3; i++) {
			health.register(false);
		}

		assertThat(health.register(false)).isEmpty();
		assertThat(health.status()).isEqualTo(HealthStatus.DOWN);
	}

	@Test
	void sucessoNaRecuperacaoVoltaParaUpEZeraContador() {
		for (int i = 0; i < 3; i++) {
			health.register(false);
		}

		assertThat(health.register(true)).contains(new HealthTransition(HealthStatus.DOWN, HealthStatus.UP));
		assertThat(health.status()).isEqualTo(HealthStatus.UP);
		assertThat(health.consecutiveFailures()).isZero();
	}

	@Test
	void falhaIntercaladaComSucessoNaoDerrubaApi() {
		health.register(false);
		health.register(false);
		health.register(true);
		health.register(false);

		assertThat(health.status()).isEqualTo(HealthStatus.DEGRADED);
		assertThat(health.consecutiveFailures()).isEqualTo(1);
	}

	@Test
	void primeiroSucessoMudaDeDesconhecidaParaUp() {
		assertThat(health.register(true)).contains(new HealthTransition(HealthStatus.UNKNOWN, HealthStatus.UP));
		assertThat(health.register(true)).isEmpty();
		assertThat(health.status()).isEqualTo(HealthStatus.UP);
	}

}
