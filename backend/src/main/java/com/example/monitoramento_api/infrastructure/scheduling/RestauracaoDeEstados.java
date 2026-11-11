package com.example.monitoramento_api.infrastructure.scheduling;

import com.example.monitoramento_api.application.HealthCheckService;
import com.example.monitoramento_api.application.MonitoredApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Ao subir a aplicacao, recalcula o estado de cada API a partir do historico gravado,
 * para que um reinicio nao mostre todas as APIs como desconhecidas.
 */
@Component
class RestauracaoDeEstados {

	private static final Logger log = LoggerFactory.getLogger(RestauracaoDeEstados.class);

	private final MonitoredApiService apis;
	private final HealthCheckService saude;

	RestauracaoDeEstados(MonitoredApiService apis, HealthCheckService saude) {
		this.apis = apis;
		this.saude = saude;
	}

	@EventListener(ApplicationReadyEvent.class)
	void restaurar() {
		apis.listar().forEach(api -> {
			try {
				saude.restaurar(api);
			} catch (RuntimeException e) {
				log.error("Nao foi possivel restaurar o estado da API {}", api.name(), e);
			}
		});
	}

}
