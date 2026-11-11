package com.example.monitoramento_api.application;

import com.example.monitoramento_api.application.port.HealthCheckRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RetencaoServiceTest {

	private static final Instant AGORA = Instant.parse("2026-03-01T03:00:00Z");

	@Test
	void apagaChecagensAnterioresAoLimiteDeDias() {
		HealthCheckRepository checks = mock(HealthCheckRepository.class);
		when(checks.apagarAnterioresA(Instant.parse("2026-01-30T03:00:00Z"))).thenReturn(7);
		RetencaoService retencao = new RetencaoService(checks, Clock.fixed(AGORA, ZoneOffset.UTC), 30);

		int apagadas = retencao.executar();

		assertThat(apagadas).isEqualTo(7);
		verify(checks).apagarAnterioresA(Instant.parse("2026-01-30T03:00:00Z"));
	}

}
