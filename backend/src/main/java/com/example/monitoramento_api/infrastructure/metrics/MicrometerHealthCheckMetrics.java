package com.example.monitoramento_api.infrastructure.metrics;

import com.example.monitoramento_api.application.port.HealthCheckMetrics;
import com.example.monitoramento_api.domain.HealthCheck;
import com.example.monitoramento_api.domain.MonitoredApi;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Metricas de cada checagem:
 * <ul>
 *   <li>{@code monitoramento_checagens_total{resultado}}: contador por sucesso e falha;</li>
 *   <li>{@code monitoramento_checagem_latencia_seconds{api}}: latencia por API (so quando houve resposta HTTP).</li>
 * </ul>
 * O gauge de APIs por estado fica em {@link MetricasConfig}.
 */
@Component
class MicrometerHealthCheckMetrics implements HealthCheckMetrics {

	private final MeterRegistry registry;

	MicrometerHealthCheckMetrics(MeterRegistry registry) {
		this.registry = registry;
	}

	@Override
	public void registrar(MonitoredApi api, HealthCheck resultado) {
		registry.counter("monitoramento.checagens", "resultado", resultado.success() ? "sucesso" : "falha").increment();

		if (resultado.statusCode() != null && resultado.responseTimeMs() != null) {
			Timer.builder("monitoramento.checagem.latencia")
					.tag("api", api.name())
					.publishPercentileHistogram()
					.register(registry)
					.record(resultado.responseTimeMs(), TimeUnit.MILLISECONDS);
		}
	}

}
