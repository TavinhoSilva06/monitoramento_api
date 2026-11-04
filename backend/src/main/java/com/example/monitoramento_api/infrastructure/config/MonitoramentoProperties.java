package com.example.monitoramento_api.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Limites da regra de alerta e da checagem, lidos de {@code monitoramento.*}.
 */
@ConfigurationProperties(prefix = "monitoramento")
public record MonitoramentoProperties(
		@DefaultValue Alerta alerta,
		@DefaultValue Checagem checagem) {

	public record Alerta(
			@DefaultValue("3") int falhasParaDown) {
	}

	public record Checagem(
			@DefaultValue("3000") int timeoutConexaoMs,
			@DefaultValue("5000") int timeoutLeituraMs,
			@DefaultValue("5000") int intervaloCicloMs,
			@DefaultValue("10") int concorrenciaMaxima) {
	}

}
