package com.example.monitoramento_api.application.port;

import com.example.monitoramento_api.domain.HealthCheck;

import java.time.Instant;
import java.util.List;

public interface HealthCheckRepository {

	HealthCheck save(HealthCheck check);

	/**
	 * Checagens de uma API feitas depois do instante informado, da mais recente para a mais antiga.
	 */
	List<HealthCheck> findByApiSince(Long apiId, Instant since);

}
