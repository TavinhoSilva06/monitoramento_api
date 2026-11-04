package com.example.monitoramento_api.infrastructure.scheduling;

import com.example.monitoramento_api.application.HealthCheckService;
import com.example.monitoramento_api.application.MonitoredApiService;
import com.example.monitoramento_api.domain.MonitoredApi;
import com.example.monitoramento_api.infrastructure.config.MonitoramentoProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;

/**
 * Dispara a checagem de cada API ativa no seu intervalo. Uma API nao e checada de novo
 * enquanto a checagem anterior ainda roda, e no maximo N checagens rodam ao mesmo tempo.
 */
@Component
public class CheckScheduler {

	private static final Logger log = LoggerFactory.getLogger(CheckScheduler.class);

	private final MonitoredApiService apis;
	private final HealthCheckService checks;
	private final Clock clock;
	private final Executor executor;
	private final Semaphore limite;
	private final Map<Long, Instant> ultimaExecucao = new ConcurrentHashMap<>();
	private final Set<Long> emExecucao = ConcurrentHashMap.newKeySet();

	public CheckScheduler(
			MonitoredApiService apis,
			HealthCheckService checks,
			Clock clock,
			Executor checagemExecutor,
			MonitoramentoProperties propriedades) {
		this.apis = apis;
		this.checks = checks;
		this.clock = clock;
		this.executor = checagemExecutor;
		this.limite = new Semaphore(propriedades.checagem().concorrenciaMaxima());
	}

	@Scheduled(fixedDelayString = "${monitoramento.checagem.intervalo-ciclo-ms:5000}")
	public void executarChecagensVencidas() {
		executarVencidas(clock.instant());
	}

	void executarVencidas(Instant agora) {
		for (MonitoredApi api : apis.listarAtivas()) {
			if (!vencida(api, agora) || !emExecucao.add(api.id())) {
				continue;
			}
			ultimaExecucao.put(api.id(), agora);
			executor.execute(() -> executar(api));
		}
	}

	private boolean vencida(MonitoredApi api, Instant agora) {
		Instant ultima = ultimaExecucao.get(api.id());
		return ultima == null || !agora.isBefore(ultima.plusSeconds(api.intervalSeconds()));
	}

	private void executar(MonitoredApi api) {
		try {
			limite.acquire();
			try {
				checks.checar(api);
			} finally {
				limite.release();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (RuntimeException e) {
			log.error("Erro inesperado ao checar a API {}", api.name(), e);
		} finally {
			emExecucao.remove(api.id());
		}
	}

}
