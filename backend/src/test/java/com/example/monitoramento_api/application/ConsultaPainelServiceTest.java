package com.example.monitoramento_api.application;

import com.example.monitoramento_api.application.port.HealthCheckRepository;
import com.example.monitoramento_api.domain.ApiNotFoundException;
import com.example.monitoramento_api.domain.ApiSummary;
import com.example.monitoramento_api.domain.HealthCheck;
import com.example.monitoramento_api.domain.HealthStatus;
import com.example.monitoramento_api.domain.MonitoredApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsultaPainelServiceTest {

	private static final Instant AGORA = Instant.parse("2026-01-10T12:00:00Z");

	private final MonitoredApi api = new MonitoredApi(1L, "Pagamentos", "https://api.exemplo.com", 30, 200, true, AGORA);

	private MonitoredApiService apis;
	private HealthCheckService saude;
	private HealthCheckRepository checks;
	private ConsultaPainelService consultas;

	@BeforeEach
	void setUp() {
		apis = mock(MonitoredApiService.class);
		saude = mock(HealthCheckService.class);
		checks = mock(HealthCheckRepository.class);
		consultas = new ConsultaPainelService(apis, saude, checks, Clock.fixed(AGORA, ZoneOffset.UTC));
		when(apis.buscar(1L)).thenReturn(api);
	}

	@Test
	void statusTrazEstadoEUltimaChecagem() {
		HealthCheck ultima = new HealthCheck(9L, 1L, 500, 80L, false, "erro", AGORA);
		when(apis.listar()).thenReturn(List.of(api));
		when(saude.estadoAtual(1L)).thenReturn(HealthStatus.DEGRADED);
		when(checks.findUltimas(1L, 1)).thenReturn(List.of(ultima));

		List<ApiStatus> status = consultas.status();

		assertThat(status).singleElement().satisfies(s -> {
			assertThat(s.estado()).isEqualTo(HealthStatus.DEGRADED);
			assertThat(s.ultimaChecagem()).isEqualTo(ultima);
		});
	}

	@Test
	void statusDeApiSemChecagemTemUltimaChecagemNula() {
		when(apis.listar()).thenReturn(List.of(api));
		when(saude.estadoAtual(1L)).thenReturn(HealthStatus.UNKNOWN);
		when(checks.findUltimas(1L, 1)).thenReturn(List.of());

		assertThat(consultas.status()).singleElement().satisfies(s -> {
			assertThat(s.estado()).isEqualTo(HealthStatus.UNKNOWN);
			assertThat(s.ultimaChecagem()).isNull();
		});
	}

	@Test
	void periodoPadraoSaoAsUltimas24Horas() {
		consultas.checagens(1L, null, null);

		verify(checks).findEntre(1L, AGORA.minusSeconds(24 * 3600), AGORA);
	}

	@Test
	void usaOPeriodoInformado() {
		Instant de = Instant.parse("2026-01-05T00:00:00Z");
		Instant ate = Instant.parse("2026-01-06T00:00:00Z");

		consultas.checagens(1L, de, ate);

		verify(checks).findEntre(1L, de, ate);
	}

	@Test
	void rejeitaPeriodoComInicioDepoisDoFim() {
		Instant de = Instant.parse("2026-01-06T00:00:00Z");
		Instant ate = Instant.parse("2026-01-05T00:00:00Z");

		assertThatThrownBy(() -> consultas.checagens(1L, de, ate))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("anterior");
	}

	@Test
	void rejeitaPeriodoMaiorQueTrintaDias() {
		Instant de = Instant.parse("2025-12-01T00:00:00Z");

		assertThatThrownBy(() -> consultas.checagens(1L, de, AGORA))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("30 dias");
	}

	@Test
	void resumoCalculaSobreOPeriodo() {
		when(checks.findEntre(eq(1L), any(), any())).thenReturn(List.of(
				new HealthCheck(null, 1L, 200, 100L, true, null, AGORA),
				new HealthCheck(null, 1L, 500, 100L, false, "erro", AGORA)));

		ApiSummary resumo = consultas.resumo(1L, null, null);

		assertThat(resumo.uptimePercent()).isEqualTo(50.0);
		assertThat(resumo.apiId()).isEqualTo(1L);
	}

	@Test
	void apiInexistenteLancaNaoEncontrada() {
		when(apis.buscar(2L)).thenThrow(new ApiNotFoundException(2L));

		assertThatThrownBy(() -> consultas.resumo(2L, null, null)).isInstanceOf(ApiNotFoundException.class);
	}

}
