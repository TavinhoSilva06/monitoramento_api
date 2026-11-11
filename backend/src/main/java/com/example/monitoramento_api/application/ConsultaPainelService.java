package com.example.monitoramento_api.application;

import com.example.monitoramento_api.application.port.HealthCheckRepository;
import com.example.monitoramento_api.domain.ApiSummary;
import com.example.monitoramento_api.domain.HealthCheck;
import com.example.monitoramento_api.domain.MonitoredApi;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Consultas usadas pelo painel: situacao atual, historico de checagens e resumo de um periodo.
 */
@Service
public class ConsultaPainelService {

	static final Duration PERIODO_PADRAO = Duration.ofHours(24);
	static final Duration PERIODO_MAXIMO = Duration.ofDays(30);

	private final MonitoredApiService apis;
	private final HealthCheckService saude;
	private final HealthCheckRepository checks;
	private final Clock clock;

	public ConsultaPainelService(MonitoredApiService apis, HealthCheckService saude, HealthCheckRepository checks, Clock clock) {
		this.apis = apis;
		this.saude = saude;
		this.checks = checks;
		this.clock = clock;
	}

	public List<ApiStatus> status() {
		return apis.listar().stream()
				.map(api -> new ApiStatus(
						api,
						saude.estadoAtual(api.id()),
						checks.findUltimas(api.id(), 1).stream().findFirst().orElse(null)))
				.toList();
	}

	public List<HealthCheck> checagens(Long apiId, Instant from, Instant to) {
		MonitoredApi api = apis.buscar(apiId);
		Periodo periodo = periodo(from, to);
		return checks.findEntre(api.id(), periodo.from(), periodo.to());
	}

	public ApiSummary resumo(Long apiId, Instant from, Instant to) {
		MonitoredApi api = apis.buscar(apiId);
		Periodo periodo = periodo(from, to);
		List<HealthCheck> historico = checks.findEntre(api.id(), periodo.from(), periodo.to());
		return ApiSummary.of(api.id(), periodo.from(), periodo.to(), historico);
	}

	/**
	 * Preenche o periodo com os padroes (ultimas 24 horas) e valida os limites.
	 */
	private Periodo periodo(Instant from, Instant to) {
		Instant fim = to != null ? to : clock.instant();
		Instant inicio = from != null ? from : fim.minus(PERIODO_PADRAO);
		if (!inicio.isBefore(fim)) {
			throw new IllegalArgumentException("O parametro 'from' deve ser anterior a 'to'");
		}
		if (Duration.between(inicio, fim).compareTo(PERIODO_MAXIMO) > 0) {
			throw new IllegalArgumentException("O periodo maximo e de " + PERIODO_MAXIMO.toDays() + " dias");
		}
		return new Periodo(inicio, fim);
	}

	private record Periodo(Instant from, Instant to) {
	}

}
