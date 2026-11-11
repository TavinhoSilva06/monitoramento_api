package com.example.monitoramento_api.infrastructure.metrics;

import com.example.monitoramento_api.application.HealthCheckService;
import com.example.monitoramento_api.domain.HealthStatus;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class MetricasConfig {

	/**
	 * Gauge {@code monitoramento_apis_estado{estado}} com a quantidade de APIs em cada estado.
	 * Um {@link MeterBinder} evita a dependencia circular entre o servico e o registro de metricas.
	 */
	@Bean
	MeterBinder estadoDasApis(HealthCheckService saude) {
		return registry -> {
			for (HealthStatus estado : new HealthStatus[] {HealthStatus.UP, HealthStatus.DEGRADED, HealthStatus.DOWN}) {
				Gauge.builder("monitoramento.apis.estado", saude, s -> s.contarEstado(estado))
						.tag("estado", estado.name())
						.description("Quantidade de APIs monitoradas em cada estado")
						.register(registry);
			}
		};
	}

}
