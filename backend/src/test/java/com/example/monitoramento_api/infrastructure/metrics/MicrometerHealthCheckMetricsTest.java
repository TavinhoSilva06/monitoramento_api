package com.example.monitoramento_api.infrastructure.metrics;

import com.example.monitoramento_api.domain.HealthCheck;
import com.example.monitoramento_api.domain.MonitoredApi;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class MicrometerHealthCheckMetricsTest {

	private final MonitoredApi api = new MonitoredApi(1L, "Pagamentos", "https://api.exemplo.com", 30, 200, true, Instant.now());

	@Test
	void contaSucessosEFalhasSeparadamente() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		MicrometerHealthCheckMetrics metricas = new MicrometerHealthCheckMetrics(registry);

		metricas.registrar(api, new HealthCheck(null, 1L, 200, 40L, true, null, Instant.now()));
		metricas.registrar(api, new HealthCheck(null, 1L, 500, 40L, false, "erro", Instant.now()));
		metricas.registrar(api, new HealthCheck(null, 1L, 500, 40L, false, "erro", Instant.now()));

		assertThat(registry.counter("monitoramento.checagens", "resultado", "sucesso").count()).isEqualTo(1.0);
		assertThat(registry.counter("monitoramento.checagens", "resultado", "falha").count()).isEqualTo(2.0);
	}

	@Test
	void mediLatenciaSomenteQuandoHouveResposta() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		MicrometerHealthCheckMetrics metricas = new MicrometerHealthCheckMetrics(registry);

		metricas.registrar(api, new HealthCheck(null, 1L, 200, 250L, true, null, Instant.now()));
		metricas.registrar(api, new HealthCheck(null, 1L, null, 5000L, false, "timeout", Instant.now()));

		assertThat(registry.timer("monitoramento.checagem.latencia", "api", "Pagamentos").count()).isEqualTo(1L);
	}

}
