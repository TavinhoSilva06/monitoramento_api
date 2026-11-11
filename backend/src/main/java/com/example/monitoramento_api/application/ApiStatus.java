package com.example.monitoramento_api.application;

import com.example.monitoramento_api.domain.HealthCheck;
import com.example.monitoramento_api.domain.HealthStatus;
import com.example.monitoramento_api.domain.MonitoredApi;

/**
 * Situacao atual de uma API para o painel: estado, e a ultima checagem (nula se nunca foi checada).
 */
public record ApiStatus(MonitoredApi api, HealthStatus estado, HealthCheck ultimaChecagem) {
}
