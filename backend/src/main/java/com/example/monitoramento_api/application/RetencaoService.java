package com.example.monitoramento_api.application;

import com.example.monitoramento_api.application.port.HealthCheckRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Apaga as checagens mais antigas que o limite de retencao.
 */
@Service
public class RetencaoService {

	private static final Logger log = LoggerFactory.getLogger(RetencaoService.class);

	private final HealthCheckRepository checks;
	private final Clock clock;
	private final Duration retencao;

	public RetencaoService(
			HealthCheckRepository checks,
			Clock clock,
			@Value("${monitoramento.retencao.dias:30}") int dias) {
		this.checks = checks;
		this.clock = clock;
		this.retencao = Duration.ofDays(dias);
	}

	public int executar() {
		Instant limite = clock.instant().minus(retencao);
		int apagadas = checks.apagarAnterioresA(limite);
		log.info("Retencao: {} checagens anteriores a {} foram apagadas", apagadas, limite);
		return apagadas;
	}

}
