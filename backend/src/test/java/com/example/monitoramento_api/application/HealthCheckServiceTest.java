package com.example.monitoramento_api.application;

import com.example.monitoramento_api.application.port.HealthCheckRepository;
import com.example.monitoramento_api.application.port.HttpProbe;
import com.example.monitoramento_api.domain.AlertPolicy;
import com.example.monitoramento_api.domain.HealthCheck;
import com.example.monitoramento_api.domain.HealthStatus;
import com.example.monitoramento_api.domain.MonitoredApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HealthCheckServiceTest {

	private final MonitoredApi api = new MonitoredApi(1L, "Pagamentos", "https://api.exemplo.com", 30, 200, true, Instant.now());

	private HttpProbe probe;
	private HealthCheckRepository checks;
	private HealthCheckService servico;

	@BeforeEach
	void setUp() {
		probe = mock(HttpProbe.class);
		checks = mock(HealthCheckRepository.class);
		when(checks.save(any())).thenAnswer(i -> i.getArgument(0));
		servico = new HealthCheckService(probe, checks, new AlertPolicy(3));
	}

	@Test
	void gravaCadaChecagem() {
		when(probe.probe(api)).thenReturn(sucesso());

		servico.checar(api);

		verify(checks).save(any(HealthCheck.class));
	}

	@Test
	void tresFalhasSeguidasDerrubamApi() {
		when(probe.probe(api)).thenReturn(falha());

		servico.checar(api);
		servico.checar(api);
		servico.checar(api);

		assertThat(servico.estadoAtual(api.id())).isEqualTo(HealthStatus.DOWN);
	}

	@Test
	void sucessoDepoisDaFalhaRecuperaApi() {
		when(probe.probe(api)).thenReturn(falha(), falha(), falha(), sucesso());

		for (int i = 0; i < 4; i++) {
			servico.checar(api);
		}

		assertThat(servico.estadoAtual(api.id())).isEqualTo(HealthStatus.UP);
	}

	@Test
	void apiSemChecagemEstaUp() {
		assertThat(servico.estadoAtual(99L)).isEqualTo(HealthStatus.UP);
	}

	private static HealthCheck sucesso() {
		return new HealthCheck(null, 1L, 200, 42L, true, null, Instant.now());
	}

	private static HealthCheck falha() {
		return new HealthCheck(null, 1L, 500, 42L, false, "Status esperado 200, recebido 500", Instant.now());
	}

}
