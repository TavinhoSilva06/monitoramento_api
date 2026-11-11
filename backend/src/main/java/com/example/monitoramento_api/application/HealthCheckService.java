package com.example.monitoramento_api.application;

import com.example.monitoramento_api.application.port.HealthCheckMetrics;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
	private final HealthCheckMetrics metrics;
	private final Map<Long, ApiHealth> estados = new ConcurrentHashMap<>();

	public HealthCheckService(HttpProbe probe, HealthCheckRepository checks, AlertPolicy policy, HealthCheckMetrics metrics) {
		this.probe = probe;
		this.checks = checks;
		this.policy = policy;
		this.metrics = metrics;
	}

	public HealthCheck checar(MonitoredApi api) {
		HealthCheck resultado = checks.save(probe.probe(api));
		metrics.registrar(api, resultado);

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

	/**
	 * Recalcula o estado da API a partir das ultimas checagens gravadas. Usado na inicializacao,
	 * para que o estado nao volte a UNKNOWN depois de um reinicio.
	 *
	 * <p>Basta reproduzir as ultimas {@code falhasParaDown} checagens: o estado depende apenas
	 * de quantas falhas seguidas existem, e esse valor nunca passa do limite.
	 */
	public void restaurar(MonitoredApi api) {
		List<HealthCheck> ultimas = new ArrayList<>(checks.findUltimas(api.id(), policy.failuresToDown()));
		ApiHealth saude = new ApiHealth(policy);
		synchronized (saude) {
			Collections.reverse(ultimas);
			ultimas.forEach(c -> saude.register(c.success()));
		}
		estados.put(api.id(), saude);
	}

	/**
	 * Remove o estado de uma API que foi excluida, para que ela nao entre na contagem.
	 */
	public void esquecer(Long apiId) {
		estados.remove(apiId);
	}

	public HealthStatus estadoAtual(Long apiId) {
		ApiHealth saude = estados.get(apiId);
		return saude == null ? HealthStatus.UNKNOWN : saude.status();
	}

	/**
	 * Quantas APIs estao no estado informado.
	 */
	public long contarEstado(HealthStatus estado) {
		return estados.values().stream().filter(s -> s.status() == estado).count();
	}

}
