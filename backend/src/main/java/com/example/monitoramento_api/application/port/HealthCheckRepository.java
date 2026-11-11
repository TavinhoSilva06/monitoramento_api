package com.example.monitoramento_api.application.port;

import com.example.monitoramento_api.domain.HealthCheck;

import java.time.Instant;
import java.util.List;

public interface HealthCheckRepository {

	HealthCheck save(HealthCheck check);

	/**
	 * Checagens de uma API feitas entre {@code from} (inclusive) e {@code to} (exclusive), da mais recente para a mais antiga.
	 */
	List<HealthCheck> findEntre(Long apiId, Instant from, Instant to);

	/**
	 * As {@code limite} checagens mais recentes de uma API, da mais recente para a mais antiga.
	 */
	List<HealthCheck> findUltimas(Long apiId, int limite);

	/**
	 * Apaga as checagens feitas antes do instante informado e devolve quantas foram apagadas.
	 */
	int apagarAnterioresA(Instant limite);

}
