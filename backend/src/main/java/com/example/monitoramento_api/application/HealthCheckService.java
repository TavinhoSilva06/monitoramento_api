package com.example.monitoramento_api.application;

import com.example.monitoramento_api.application.port.HealthCheckRepository;
import com.example.monitoramento_api.application.port.HttpProbe;
import com.example.monitoramento_api.domain.AlertPolicy;
import com.example.monitoramento_api.domain.ApiHealth;
import com.example.monitoramento_api.domain.HealthCheck;
import com.example.monitoramento_api.domain.HealthStatus;
import com.example.monitoramento_api.domain.HealthTransition;
import com.example.monitoramento_api.domain.MonitoredApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Executa uma checagem, grava o resultado e atualiza o estado de saude da API.
 */
@Service
public class HealthCheckService {

	private static final Logger log = LoggerFactory.getLogger(HealthCheckService.class);

	private final HttpProbe probe;
	private final HealthCheckRepository checks;
	private final AlertPolicy policy;
	private final Map<Long, ApiHealth> estados = new ConcurrentHashMap<>();

	public HealthCheckService(HttpProbe probe, HealthCheckRepository checks, AlertPolicy policy) {
		this.probe = probe;
		this.checks = checks;
		this.policy = policy;
	}

	public HealthCheck checar(MonitoredApi api) {
		HealthCheck resultado = checks.save(probe.probe(api));

		ApiHealth saude = estados.computeIfAbsent(api.id(), id -> new ApiHealth(policy));
		Optional<HealthTransition> transicao;
		synchronized (saude) {
			transicao = saude.register(resultado.success());
		}

		if (resultado.success()) {
			log.debug("Checagem OK: {} em {} ms", api.name(), resultado.responseTimeMs());
		} else {
			log.warn("Falha na checagem de {}: {}", api.name(), resultado.errorMessage());
		}
		transicao.ifPresent(t -> log.info("API {} mudou de {} para {}", api.name(), t.from(), t.to()));

		return resultado;
	}

	public HealthStatus estadoAtual(Long apiId) {
		ApiHealth saude = estados.get(apiId);
		return saude == null ? HealthStatus.UP : saude.status();
	}

}
