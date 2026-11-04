package com.example.monitoramento_api.application.port;

import com.example.monitoramento_api.domain.HealthCheck;
import com.example.monitoramento_api.domain.MonitoredApi;

/**
 * Faz uma chamada HTTP a API monitorada. Falhas de rede viram um resultado sem sucesso, nunca uma excecao.
 */
public interface HttpProbe {

	HealthCheck probe(MonitoredApi api);

}
