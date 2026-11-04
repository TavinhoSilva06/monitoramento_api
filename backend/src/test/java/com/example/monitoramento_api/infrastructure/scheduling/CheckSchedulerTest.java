package com.example.monitoramento_api.infrastructure.scheduling;

import com.example.monitoramento_api.application.HealthCheckService;
import com.example.monitoramento_api.application.MonitoredApiService;
import com.example.monitoramento_api.domain.MonitoredApi;
import com.example.monitoramento_api.infrastructure.config.MonitoramentoProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CheckSchedulerTest {

	private static final Instant T0 = Instant.parse("2026-01-01T12:00:00Z");

	private final MonitoredApi rapida = api(1L, 30);
	private final MonitoredApi lenta = api(2L, 60);

	private MonitoredApiService apis;
	private HealthCheckService checks;

	@BeforeEach
	void setUp() {
		apis = mock(MonitoredApiService.class);
		checks = mock(HealthCheckService.class);
		when(apis.listarAtivas()).thenReturn(List.of(rapida, lenta));
	}

	@Test
	void checaApenasAsApisVencidas() {
		CheckScheduler agendador = agendador(Runnable::run);

		agendador.executarVencidas(T0);
		agendador.executarVencidas(T0.plusSeconds(30));

		verify(checks, times(2)).checar(rapida);
		verify(checks, times(1)).checar(lenta);
	}

	@Test
	void naoChecaDeNovaUmaApiEnquantoACheckagemAnteriorRoda() {
		List<Runnable> fila = new ArrayList<>();
		CheckScheduler agendador = agendador(fila::add);

		agendador.executarVencidas(T0);
		agendador.executarVencidas(T0.plusSeconds(120));

		assertThat(fila).hasSize(2);
		fila.forEach(Runnable::run);
		verify(checks, times(1)).checar(rapida);
		verify(checks, times(1)).checar(lenta);
	}

	@Test
	void erroInesperadoNaoImpedeProximasChecagens() {
		doThrow(new IllegalStateException("falha inesperada")).when(checks).checar(rapida);
		CheckScheduler agendador = agendador(Runnable::run);

		agendador.executarVencidas(T0);
		agendador.executarVencidas(T0.plusSeconds(30));

		verify(checks, times(2)).checar(rapida);
	}

	private CheckScheduler agendador(Executor executor) {
		MonitoramentoProperties propriedades = new MonitoramentoProperties(
				new MonitoramentoProperties.Alerta(3),
				new MonitoramentoProperties.Checagem(3000, 5000, 5000, 10));
		Clock relogio = Clock.fixed(T0, ZoneOffset.UTC);
		return new CheckScheduler(apis, checks, relogio, executor, propriedades);
	}

	private static MonitoredApi api(Long id, int intervalo) {
		return new MonitoredApi(id, "API " + id, "https://api.exemplo.com", intervalo, 200, true, T0);
	}

}
