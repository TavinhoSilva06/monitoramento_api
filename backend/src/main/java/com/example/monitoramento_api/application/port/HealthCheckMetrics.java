package com.example.monitoramento_api.application.port;

import com.example.monitoramento_api.domain.HealthCheck;
import com.example.monitoramento_api.domain.MonitoredApi;

/**
 * Registra metricas de cada checagem. A implementacao concreta (Micrometer) fica na infraestrutura.
 */
public interface HealthCheckMetrics {

	void registrar(MonitoredApi api, HealthCheck resultado);

}
