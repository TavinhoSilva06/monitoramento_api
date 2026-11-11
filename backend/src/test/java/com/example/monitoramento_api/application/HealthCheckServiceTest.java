package com.example.monitoramento_api.application;

import com.example.monitoramento_api.application.port.HealthCheckMetrics;
import com.example.monitoramento_api.application.port.HealthCheckRepository;
import com.example.monitoramento_api.application.port.HttpProbe;
import com.example.monitoramento_api.domain.AlertPolicy;
import com.example.monitoramento_api.domain.HealthCheck;
import com.example.monitoramento_api.domain.HealthStatus;
import com.example.monitoramento_api.domain.MonitoredApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HealthCheckServiceTest {

	private final MonitoredApi api = new MonitoredApi(1L, "Pagamentos", "https://api.exemplo.com", 30, 200, true, Instant.now());

	private HttpProbe probe;
	private HealthCheckRepository checks;
	private HealthCheckMetrics metrics;
	private HealthCheckService servico;

	@BeforeEach
	void setUp() {
		probe = mock(HttpProbe.class);
		checks = mock(HealthCheckRepository.class);
		metrics = mock(HealthCheckMetrics.class);
		when(checks.save(any())).thenAnswer(i -> i.getArgument(0));
		servico = new HealthCheckService(probe, checks, new AlertPolicy(3), metrics);
	}

	@Test
	void gravaCadaChecagem() {
		when(probe.probe(api)).thenReturn(sucesso());

		servico.checar(api);

		verify(checks).save(any(HealthCheck.class));
	}

	@Test
	void registraMetricaDeCadaChecagem() {
		HealthCheck resultado = falha();
		when(probe.probe(api)).thenReturn(resultado);

		servico.checar(api);

		verify(metrics).registrar(api, resultado);
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
	void apiSemChecagemEstaDesconhecida() {
		assertThat(servico.estadoAtual(99L)).isEqualTo(HealthStatus.UNKNOWN);
	}

	@Test
	void restauraDownAPartirDasUltimasFalhas() {
		when(checks.findUltimas(api.id(), 3)).thenReturn(List.of(falha(), falha(), falha()));

		servico.restaurar(api);

		assertThat(servico.estadoAtual(api.id())).isEqualTo(HealthStatus.DOWN);
	}

	@Test
	void restauraUpQuandoUltimaChecagemFoiSucesso() {
		// Ordem da mais recente para a mais antiga: a ultima foi sucesso
		when(checks.findUltimas(api.id(), 3)).thenReturn(List.of(sucesso(), falha(), falha()));

		servico.restaurar(api);

		assertThat(servico.estadoAtual(api.id())).isEqualTo(HealthStatus.UP);
	}

	@Test
	void restauraDegradadaComUmaFalhaRecente() {
		when(checks.findUltimas(api.id(), 3)).thenReturn(List.of(falha(), sucesso(), sucesso()));

		servico.restaurar(api);

		assertThat(servico.estadoAtual(api.id())).isEqualTo(HealthStatus.DEGRADED);
	}

	@Test
	void restauracaoSemHistoricoContinuaDesconhecida() {
		when(checks.findUltimas(api.id(), 3)).thenReturn(List.of());

		servico.restaurar(api);

		assertThat(servico.estadoAtual(api.id())).isEqualTo(HealthStatus.UNKNOWN);
	}

	@Test
	void contaApisEmCadaEstado() {
		MonitoredApi outra = new MonitoredApi(2L, "Outra", "https://outra.exemplo.com", 30, 200, true, Instant.now());
		when(probe.probe(api)).thenReturn(falha());
		when(probe.probe(outra)).thenReturn(sucesso());
		for (int i = 0; i < 3; i++) {
			servico.checar(api);
		}
		servico.checar(outra);

		assertThat(servico.contarEstado(HealthStatus.DOWN)).isEqualTo(1);
		assertThat(servico.contarEstado(HealthStatus.UP)).isEqualTo(1);
		assertThat(servico.contarEstado(HealthStatus.DEGRADED)).isZero();
	}

	@Test
	void esquecerRemoveEstadoDaApi() {
		when(probe.probe(api)).thenReturn(falha());
		for (int i = 0; i < 3; i++) {
			servico.checar(api);
		}

		servico.esquecer(api.id());

		assertThat(servico.estadoAtual(api.id())).isEqualTo(HealthStatus.UNKNOWN);
		assertThat(servico.contarEstado(HealthStatus.DOWN)).isZero();
	}

	private static HealthCheck sucesso() {
		return new HealthCheck(null, 1L, 200, 42L, true, null, Instant.now());
	}

	private static HealthCheck falha() {
		return new HealthCheck(null, 1L, 500, 42L, false, "Status esperado 200, recebido 500", Instant.now());
	}

}
